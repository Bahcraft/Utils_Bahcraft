package com.utils_bahcraft.items;

import com.utils_bahcraft.UtilsBahCraft;
import com.utils_bahcraft.utils.HammerUtils;
import org.jetbrains.annotations.NotNull;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.EquipmentSlot;

import java.util.UUID;

public class LightningHammerItem extends Item {

    private static final UUID KNOCKBACK_UUID = UUID.fromString("2f3d9178-6f6f-45d2-a3c3-5684534f4342");
    private static final UUID MOVEMENT_UUID = UUID.fromString("1a2b3c4d-1e2f-3a4b-5c6d-7e8f9a0b1c2d");

    public LightningHammerItem(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (player.isCrouching()) {
            if (!level.isClientSide) {
                HammerUtils.toggleMode(level, player, stack, HammerUtils.TAG_MODE);
            }
            return InteractionResultHolder.consume(stack);
        }

        if (HammerUtils.isModeActive(stack)) {
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }

        return InteractionResultHolder.pass(stack);
    }

    @Override
    public void onUseTick(@NotNull Level level, @NotNull LivingEntity livingEntity, @NotNull ItemStack stack, int remainingUseDuration) {
        if (!(livingEntity instanceof Player player)) return;

        if (HammerUtils.isModeActive(stack) && player.isUsingItem() && player.getUseItem() == stack) {
            HammerUtils.spinTick(level, player, stack, remainingUseDuration);
            return;
        }
        if (!level.isClientSide) {
            HammerUtils.clearLaunchState(player);
        }
        livingEntity.stopUsingItem();
    }

    @Override
    public void releaseUsing(@NotNull ItemStack stack, @NotNull Level level, @NotNull LivingEntity livingEntity, int timeLeft) {
        if (!level.isClientSide && livingEntity instanceof Player player) {
            HammerUtils.clearLaunchState(player);
        }
        super.releaseUsing(stack, level, livingEntity, timeLeft);
    }

    @Override
    public @NotNull InteractionResult useOn(@NotNull UseOnContext context) {
        Player player = context.getPlayer();
        Level level = context.getLevel();

        if (player != null && player.isCrouching()) {
            return InteractionResult.PASS;
        }

        if (!HammerUtils.isModeActive(context.getItemInHand())) {
            return InteractionResult.PASS;
        }

        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        Vec3 positionClicked = Vec3.atBottomCenterOf(context.getClickedPos().above());

        if (player != null) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 5, 5, false, false));
        }

        HammerUtils.spawnLightningAt(level, positionClicked);

        return InteractionResult.SUCCESS;
    }

    @Override
    public void inventoryTick(@NotNull ItemStack stack, @NotNull Level level, @NotNull Entity entity, int slotId, boolean isSelected) {
        if (!(entity instanceof Player player)) return;

        if (level.isClientSide) {
            boolean isUnarmed = !player.onGround();
            stack.getOrCreateTag().putBoolean("RenderInAir", isUnarmed);
        }

        // Not selected, ensure elytra is off
        if (!isSelected) {
            if (!level.isClientSide) {
                HammerUtils.clearLaunchState(player);
            }
            HammerUtils.cancelSmash(stack);
            HammerUtils.removeGodMode(player);
            return;
        }

        // If launch flag is on but player is not using this item anymore, stop elytra
        if (!level.isClientSide) {
            boolean launchActive = player.getPersistentData().getBoolean(HammerUtils.PD_LAUNCH);
            boolean usingThis = player.isUsingItem() && player.getUseItem() == stack;
            if (launchActive && !usingThis) {
                HammerUtils.clearLaunchState(player);
            }
        }

        if (HammerUtils.isModeActive(stack)) {
            HammerUtils.applyGodModeEffects(player);
        }

        // Smash logic
        CompoundTag tag = stack.getOrCreateTag();
        if (tag.getBoolean(HammerUtils.TAG_LAUNCH)) {
            if (HammerUtils.checkAndExecuteSmash(level, player, stack)) {
                HammerUtils.cancelSmash(stack);
            }
        }
    }

    @Override
    public boolean hurtEnemy(@NotNull ItemStack stack, @NotNull LivingEntity target, @NotNull LivingEntity attacker) {
        if (!HammerUtils.isModeActive(stack)) {
            return super.hurtEnemy(stack, target, attacker);
        }

        Level level = target.level();
        if (!level.isClientSide()) {
            HammerUtils.spawnLightningAt(level, target.position());

            new Thread(() -> {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    UtilsBahCraft.LOGGER.error("Interrupted while waiting to force kill target", e);
                }

                level.getServer().execute(() -> {
                    if (target.isAlive()) {
                        HammerUtils.forceKill(target, level, level.damageSources().lightningBolt());
                    }
                });
            }).start();
        }

        return true;
    }

    @Override
    public boolean onLeftClickEntity(@NotNull ItemStack stack, @NotNull Player player, @NotNull Entity entity) {
        if (player.level().isClientSide) return super.onLeftClickEntity(stack, player, entity);
        if (!(entity instanceof LivingEntity target)) return super.onLeftClickEntity(stack, player, entity);
        if (!HammerUtils.isModeActive(stack)) return super.onLeftClickEntity(stack, player, entity);

        HammerUtils.spawnLightningAt(player.level(), target.position());

        HammerUtils.forceKill(target, player.level(), player.level().damageSources().lightningBolt());
        return true;
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        if (slot != EquipmentSlot.MAINHAND) {
            return super.getDefaultAttributeModifiers(slot);
        }

        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();

        if (HammerUtils.isModeActive(stack)) {
            builder.put(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_UUID, "Tool modifier",
                    Float.POSITIVE_INFINITY, AttributeModifier.Operation.ADDITION));
            builder.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Tool modifier",
                    Float.POSITIVE_INFINITY, AttributeModifier.Operation.ADDITION));
            builder.put(Attributes.ATTACK_KNOCKBACK, new AttributeModifier(KNOCKBACK_UUID, "Weapon knockback",
                    50.0f, AttributeModifier.Operation.ADDITION));
            builder.put(Attributes.MOVEMENT_SPEED, new AttributeModifier(MOVEMENT_UUID, "Weapon speed",
                    0.15f, AttributeModifier.Operation.ADDITION));
        } else {
            builder.put(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_UUID, "Tool modifier",
                    15.0f, AttributeModifier.Operation.ADDITION));
            builder.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Tool modifier",
                    15.0f, AttributeModifier.Operation.ADDITION));
            builder.put(Attributes.ATTACK_KNOCKBACK, new AttributeModifier(KNOCKBACK_UUID, "Weapon knockback",
                    5.0f, AttributeModifier.Operation.ADDITION));
        }

        return builder.build();
    }

    @Override
    public @NotNull Component getName(ItemStack stack) {
        if (HammerUtils.isModeActive(stack)) {
            return Component.literal("Martelão")
                    .withStyle(ChatFormatting.GOLD)
                    .withStyle(ChatFormatting.BOLD);
        } else {
            return Component.literal("Martelão")
                    .withStyle(ChatFormatting.GRAY);
        }
    }

    @Override
    public boolean canElytraFly(ItemStack stack, LivingEntity entity) {
        if (!(entity instanceof Player player)) return false;
        return HammerUtils.isModeActive(stack)
                && player.isUsingItem()
                && player.getUseItem() == stack;
    }

    @Override
    public boolean elytraFlightTick(ItemStack stack, LivingEntity entity, int flightTicks) {
        if (!(entity instanceof Player player)) return false;

        boolean usingThis = HammerUtils.isModeActive(stack)
                && player.isUsingItem()
                && player.getUseItem() == stack;

        if (!usingThis && !entity.level().isClientSide) {
            HammerUtils.clearLaunchState(player);
        }

        return usingThis;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return HammerUtils.isModeActive(stack);
    }

    @Override
    public boolean onDroppedByPlayer(ItemStack item, Player player) {
        HammerUtils.removeGodMode(player);
        return true;
    }


    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.SPEAR;
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged && newStack.getItem() != oldStack.getItem();
    }

}
