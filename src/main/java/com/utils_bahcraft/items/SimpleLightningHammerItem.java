package com.utils_bahcraft.items;

import com.utils_bahcraft.utils.HammerUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class SimpleLightningHammerItem extends Item {
    private static final String TAG_MODE = "SIMPLE_HAMMER_MODE";

    public SimpleLightningHammerItem(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull InteractionResult useOn(@NotNull UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();

        if (level.isClientSide || !HammerUtils.isModeActive(context.getItemInHand(), TAG_MODE)) {
            return InteractionResult.SUCCESS;
        }

        // respect creative mode: don't drop block items if player is in creative
        boolean dropItems = !(player != null && player.isCreative());

        HammerUtils.strikeBlockWithLightning(level, pos, dropItems);

        return InteractionResult.SUCCESS;
    }

    @Override
    public @NotNull InteractionResult interactLivingEntity(@NotNull ItemStack stack, @NotNull Player player, @NotNull LivingEntity interactionTarget, @NotNull InteractionHand usedHand) {
        if (player.level().isClientSide || !HammerUtils.isModeActive(stack, TAG_MODE)) {
            return InteractionResult.SUCCESS;
        }

        HammerUtils.spawnLightningAt(player.level(), interactionTarget.position());
        HammerUtils.forceKill(interactionTarget, player.level());

        return InteractionResult.SUCCESS;

    }

    @Override
    public boolean onLeftClickEntity(@NotNull ItemStack stack, @NotNull Player player, @NotNull Entity entity) {
        if (player.level().isClientSide) return super.onLeftClickEntity(stack, player, entity);
        if (!(entity instanceof LivingEntity target)) return super.onLeftClickEntity(stack, player, entity);
        if (!HammerUtils.isModeActive(stack, TAG_MODE)) return super.onLeftClickEntity(stack, player, entity);

        HammerUtils.spawnLightningAt(player.level(), target.position());
        HammerUtils.forceKill(target, player.level(), player.level().damageSources().lightningBolt());
        return true;
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (player.isCrouching()) {
            if (!level.isClientSide) {
                HammerUtils.toggleMode(level, player, stack, TAG_MODE);
            }
            return InteractionResultHolder.consume(stack);
        }
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public boolean hurtEnemy(@NotNull ItemStack stack, @NotNull LivingEntity target, @NotNull LivingEntity attacker) {
        Level level = target.level();
        if (level.isClientSide) {
            return super.hurtEnemy(stack, target, attacker);
        }

        Vec3 targetPos = target.position();
        HammerUtils.spawnLightningAt(level, targetPos);
        HammerUtils.forceKill(target, level);

        return super.hurtEnemy(stack, target, attacker);
    }

    @Override
    public @NotNull UseAnim getUseAnimation(@NotNull ItemStack stack) {
        return UseAnim.NONE;
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged && newStack.getItem() != oldStack.getItem();
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return HammerUtils.isModeActive(stack, TAG_MODE);
    }
}