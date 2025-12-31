package com.utils_bahcraft.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class RecordingWorldActions implements WorldActions {
    public static class Call {
        public final String name;
        public final Object[] args;
        public Call(String name, Object... args) { this.name = name; this.args = args; }
    }

    public final List<Call> calls = new ArrayList<>();

    @Override
    public void spawnLightningAt(@NotNull Level level, @NotNull Vec3 position, boolean playThunder) {
        calls.add(new Call("spawnLightningAt", level, position, playThunder));
    }

    @Override
    public void spawnThunderAt(@NotNull Level level, @NotNull Vec3 position, float volume, float pitch) {
        calls.add(new Call("spawnThunderAt", level, position, volume, pitch));
    }

    @Override
    public void strikeBlockWithLightning(@NotNull Level level, @NotNull BlockPos pos, boolean dropItems) {
        calls.add(new Call("strikeBlockWithLightning", level, pos, dropItems));
    }

    @Override
    public void forceKill(@NotNull LivingEntity target, @NotNull Level level, @NotNull DamageSource source) {
        calls.add(new Call("forceKill", target, level, source));
    }

    @Override
    public void dropHead(@NotNull LivingEntity target, @NotNull Level level) {
        calls.add(new Call("dropHead", target, level));
    }
}

