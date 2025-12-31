package com.utils_bahcraft.items;

import com.utils_bahcraft.utils.HammerUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

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
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (player.isCrouching()) {
            if (!level.isClientSide) {
                HammerUtils.toggleMode(level, player, stack);
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
    public void onUseTick(Level level, LivingEntity livingEntity, ItemStack stack, int remainingUseDuration) {
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
    public void releaseUsing(ItemStack stack, Level level, LivingEntity livingEntity, int timeLeft) {
        if (!level.isClientSide && livingEntity instanceof Player player) {
            HammerUtils.clearLaunchState(player);
        }
        super.releaseUsing(stack, level, livingEntity, timeLeft);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
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
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);

        if (bolt != null) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 5, 5, false, false));
            bolt.moveTo(positionClicked);
            level.addFreshEntity(bolt);
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
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
            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
            if (bolt != null) {
                bolt.moveTo(target.position());
                level.addFreshEntity(bolt);
            }

            new Thread(() -> {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                level.getServer().execute(() -> {
                    if (target.isAlive()) {
                        HammerUtils.forceKill(target, level);
                    }
                });
            }).start();
        }

        return true;
    }

    @Override
    public boolean onLeftClickEntity(ItemStack stack, Player player, Entity entity) {
        if (player.level().isClientSide) return super.onLeftClickEntity(stack, player, entity);
        if (!(entity instanceof LivingEntity target)) return super.onLeftClickEntity(stack, player, entity);
        if (!HammerUtils.isModeActive(stack)) return super.onLeftClickEntity(stack, player, entity);

        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(player.level());
        if (bolt != null) {
            bolt.moveTo(target.position());
            player.level().addFreshEntity(bolt);
        }

        HammerUtils.forceKill(target, player.level());
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
