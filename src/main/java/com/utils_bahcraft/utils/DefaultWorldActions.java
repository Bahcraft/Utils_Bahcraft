package com.utils_bahcraft.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class DefaultWorldActions implements WorldActions {
    @Override
    public void spawnLightningAt(@NotNull Level level, @NotNull Vec3 position, boolean playThunder) {
        if (level.isClientSide) return;
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt != null) {
            bolt.moveTo(position);
            level.addFreshEntity(bolt);
        }
        if (playThunder) {
            level.playSound(null, position.x, position.y, position.z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 1.0f, 1.0f);
        }
    }

    @Override
    public void spawnThunderAt(@NotNull Level level, @NotNull Vec3 position, float volume, float pitch) {
        if (level.isClientSide) return;
        level.playSound(null, position.x, position.y, position.z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, volume, pitch);
    }

    @Override
    public void strikeBlockWithLightning(@NotNull Level level, @NotNull BlockPos pos, boolean dropItems) {
        if (level.isClientSide) return;
        spawnLightningAt(level, Vec3.atCenterOf(pos), true);
        BlockState state = level.getBlockState(pos);

        boolean isFire = state.is(Blocks.FIRE);

        pos = isFire ? pos.below() : pos;

        level.destroyBlock(pos, dropItems);
    }

    @Override
    public void forceKill(@NotNull LivingEntity target, @NotNull Level level, @NotNull DamageSource source) {
        if (level.isClientSide) return;

        if (target instanceof ServerPlayer player) {
            player.getAbilities().invulnerable = false;
            player.onUpdateAbilities();
        }

        dropHead(target, level);
        target.invulnerableTime = 0;

        boolean damaged = target.hurt(source, Float.MAX_VALUE);

        if (!damaged && target.isAlive()) {
            target.setHealth(0);
        }
    }

    public void dropHead(@NotNull LivingEntity target, @NotNull Level level) {
        ItemStack headStack = null;
        if (target instanceof Player player) {
            headStack = new ItemStack(Items.PLAYER_HEAD);
            var tag = headStack.getOrCreateTag();
            var skullOwner = new net.minecraft.nbt.CompoundTag();
            skullOwner.putUUID("Id", player.getUUID());
            skullOwner.putString("Name", player.getGameProfile().getName());
            tag.put("SkullOwner", skullOwner);
        } else if (target instanceof net.minecraft.world.entity.monster.Zombie) {
            headStack = new ItemStack(Items.ZOMBIE_HEAD);
        } else if (target instanceof net.minecraft.world.entity.monster.Skeleton) {
            headStack = new ItemStack(Items.SKELETON_SKULL);
        } else if (target instanceof net.minecraft.world.entity.monster.WitherSkeleton) {
            headStack = new ItemStack(Items.WITHER_SKELETON_SKULL);
        } else if (target instanceof net.minecraft.world.entity.monster.Creeper) {
            headStack = new ItemStack(Items.CREEPER_HEAD);
        } else if (target instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon) {
            headStack = new ItemStack(Items.DRAGON_HEAD);
        }

        if (headStack != null) {
            ItemEntity drop = new ItemEntity(level, target.getX(), target.getY(), target.getZ(), headStack);
            drop.setPickUpDelay(0);
            drop.setInvulnerable(true);
            level.addFreshEntity(drop);
        }
    }
}
