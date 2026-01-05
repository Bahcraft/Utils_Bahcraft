package com.utils_bahcraft.items.hammer;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.utils_bahcraft.interfaces.LightningHammerBase;
import com.utils_bahcraft.utils.HammerUtils;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class LightningHammerItem extends LightningHammerBase {

    protected static final UUID KNOCKBACK_UUID = UUID.fromString("2f3d9178-6f6f-45d2-a3c3-5684534f4342");
    protected static final UUID MOVEMENT_UUID = UUID.fromString("1a2b3c4d-1e2f-3a4b-5c6d-7e8f9a0b1c2d");

    public LightningHammerItem(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull String getModeTag() {
        return HammerUtils.TAG_MODE;
    }

    @Override
    protected @NotNull String getHammerName() {
        return "Martelão";
    }

    @Override
    protected boolean shouldStartUsingWhenModeActive() {
        return true;
    }

    @Override
    public @NotNull UseAnim getUseAnimation(@NotNull ItemStack stack) {
        return UseAnim.SPEAR;
    }

    @Override
    public void onUseTick(@NotNull Level level, @NotNull LivingEntity livingEntity, @NotNull ItemStack stack, int remainingUseDuration) {
        if (!(livingEntity instanceof Player player)) return;

        if (HammerUtils.isModeActive(stack, getModeTag()) && player.isUsingItem() && player.getUseItem() == stack) {
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

        if (!HammerUtils.isModeActive(context.getItemInHand(), getModeTag())) {
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

        // 1. CLIENT SIDE: Visuals Only
        if (level.isClientSide) {
            if (isSelected) {
                boolean isUnarmed = !player.onGround();
                if (stack.getOrCreateTag().getBoolean("RenderInAir") != isUnarmed) {
                    stack.getOrCreateTag().putBoolean("RenderInAir", isUnarmed);
                }
            }
            return;
        }

        // 2. UNSELECTED LOGIC (Cleanup)
        if (!isSelected) {
            boolean holdingAnotherHammer = HammerUtils.isHoldingLightingHammer(player);

            if (stack.hasTag() && stack.getTag().getBoolean(HammerUtils.TAG_LAUNCH)) {
                HammerUtils.cancelSmash(stack);
            }

            if (!holdingAnotherHammer) {
                HammerUtils.clearLaunchState(player);
                HammerUtils.removeGodMode(player);
            }
            return;
        }

        // 3. SELECTED LOGIC (Active)
        boolean launchActive = player.getPersistentData().getBoolean(HammerUtils.PD_LAUNCH);
        boolean isUsingThisStack = player.isUsingItem() && player.getUseItem() == stack;

        if (launchActive && !isUsingThisStack) {
            HammerUtils.clearLaunchState(player);
        }

        if (HammerUtils.isModeActive(stack, getModeTag())) {
            HammerUtils.applyGodModeEffects(player);
        }

        if (stack.getTag() != null && stack.getTag().getBoolean(HammerUtils.TAG_LAUNCH)) {
            if (HammerUtils.checkAndExecuteSmash(level, player, stack)) {
                HammerUtils.cancelSmash(stack);
            }
        }
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        if (slot != EquipmentSlot.MAINHAND) {
            return super.getDefaultAttributeModifiers(slot);
        }

        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();

        if (HammerUtils.isModeActive(stack, getModeTag())) {
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
    public boolean canElytraFly(ItemStack stack, LivingEntity entity) {
        if (!(entity instanceof Player player)) return false;
        return HammerUtils.isModeActive(stack, getModeTag())
                && player.isUsingItem()
                && player.getUseItem() == stack;
    }

    @Override
    public boolean elytraFlightTick(ItemStack stack, LivingEntity entity, int flightTicks) {
        if (!(entity instanceof Player player)) return false;

        boolean usingThis = HammerUtils.isModeActive(stack, getModeTag())
                && player.isUsingItem()
                && player.getUseItem() == stack;

        if (!usingThis && !entity.level().isClientSide) {
            HammerUtils.clearLaunchState(player);
        }

        return usingThis;
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

}
