package com.utils_bahcraft.interfaces;

import com.utils_bahcraft.UtilsBahCraft;
import com.utils_bahcraft.utils.HammerUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

/**
 * Base class that contains shared logic between LightningHammerItem and SimpleLightningHammerItem.
 * Subclasses should provide their own mode tag via {@link #getModeTag()} and optionally
 * indicate whether the item should start using when mode is active by overriding
 * {@link #shouldStartUsingWhenModeActive()}.
 */
public abstract class LightningHammerBase extends Item {
    public LightningHammerBase(Properties properties) {
        super(properties);
    }

    /**
     * Return the NBT tag key used to store the mode for this hammer.
     */
    @NotNull
    public abstract String getModeTag();

    protected abstract @NotNull String getHammerName();

    /**
     * If true, the item will call startUsingItem when mode is active and the player uses the item.
     */
    protected boolean shouldStartUsingWhenModeActive() {
        return false;
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (player.isCrouching()) {
            if (!level.isClientSide) {
                HammerUtils.toggleMode(level, player, stack, getModeTag());
                return InteractionResultHolder.consume(stack);
            }
            return InteractionResultHolder.consume(stack);
        }

        if (HammerUtils.isModeActive(stack, getModeTag()) && shouldStartUsingWhenModeActive()) {
            player.startUsingItem(hand);
            HammerUtils.spawnLightningAt(level, player.position());
            player.clearFire();
            return InteractionResultHolder.consume(stack);
        }

        return InteractionResultHolder.pass(stack);
    }

    @Override
    public boolean isFoil(@NotNull ItemStack stack) {
        return HammerUtils.isModeActive(stack, getModeTag());
    }

    @Override
    public @NotNull UseAnim getUseAnimation(@NotNull ItemStack stack) {
        return UseAnim.NONE;
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged
                || newStack.getItem() != oldStack.getItem();
    }

    @Override
    public @NotNull Component getName(@NotNull ItemStack stack) {
        if (HammerUtils.isModeActive(stack, getModeTag())) {
            return Component.literal(getHammerName())
                    .withStyle(ChatFormatting.GOLD)
                    .withStyle(ChatFormatting.BOLD);
        } else {
            return Component.literal(getHammerName())
                    .withStyle(ChatFormatting.GRAY);
        }
    }

    @Override
    public @NotNull InteractionResult interactLivingEntity(@NotNull ItemStack stack, @NotNull Player player, @NotNull LivingEntity interactionTarget, @NotNull InteractionHand usedHand) {
        Level level = player.level();
        if (level.isClientSide || !HammerUtils.isModeActive(stack, getModeTag())) {
            return InteractionResult.SUCCESS;
        }

        HammerUtils.spawnLightningAt(level, interactionTarget.position());
        HammerUtils.forceKill(interactionTarget, level);

        return InteractionResult.SUCCESS;

    }

    @Override
    public boolean onLeftClickEntity(@NotNull ItemStack stack, @NotNull Player player, @NotNull Entity entity) {
        Level level = player.level();
        if (level.isClientSide) return super.onLeftClickEntity(stack, player, entity);
        if (!(entity instanceof LivingEntity target)) return super.onLeftClickEntity(stack, player, entity);
        if (!HammerUtils.isModeActive(stack, getModeTag())) return super.onLeftClickEntity(stack, player, entity);

        HammerUtils.spawnLightningAt(level, target.position());
        HammerUtils.forceKill(target, level, level.damageSources().lightningBolt());
        return true;
    }

    @Override
    public boolean hurtEnemy(@NotNull ItemStack stack, @NotNull LivingEntity target, @NotNull LivingEntity attacker) {
        if (!HammerUtils.isModeActive(stack, getModeTag())) {
            return super.hurtEnemy(stack, target, attacker);
        }

        Level level = target.level();
        if (!level.isClientSide()) {
            HammerUtils.spawnLightningAt(level, target.position());

            var server = level.getServer();
            if (server == null) {
                UtilsBahCraft.LOGGER.warn("Server is null, cannot schedule force kill for target");
            } else {
                new Thread(() -> {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        UtilsBahCraft.LOGGER.error("Interrupted while waiting to force kill target", e);
                        Thread.currentThread().interrupt();
                    }

                    server.execute(() -> {
                        if (target.isAlive()) {
                            HammerUtils.forceKill(target, level, level.damageSources().lightningBolt());
                        }
                    });
                }).start();
            }
        }

        return true;
    }
}
