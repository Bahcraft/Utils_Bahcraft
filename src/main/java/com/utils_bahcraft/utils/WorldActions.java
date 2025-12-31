package com.utils_bahcraft.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public interface WorldActions {
    void spawnLightningAt(@NotNull Level level, @NotNull Vec3 position, boolean playThunder);
    void spawnThunderAt(@NotNull Level level, @NotNull Vec3 position, float volume, float pitch);
    void strikeBlockWithLightning(@NotNull Level level, @NotNull BlockPos pos, boolean dropItems);
    void forceKill(@NotNull LivingEntity target, @NotNull Level level, @NotNull DamageSource source);
    void dropHead(@NotNull LivingEntity target, @NotNull Level level);
}
