package com.utils_bahcraft.utils;

import org.jetbrains.annotations.NotNull;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class HammerUtils {

    private static WorldActions worldActions = new DefaultWorldActions();

    public static void setWorldActions(WorldActions actions) {
        if (actions != null) worldActions = actions;
    }

    public static WorldActions getWorldActions() {
        return worldActions;
    }

    public static final String TAG_MODE = "LightningMode";
    public static final String BALANCED_TAG_MODE = "BALANCED_HAMMER_MODE";
    public static final String TAG_LAUNCH = "HammerLaunch";

    public static final String PD_LAUNCH = "bahcraft_launch_pd";
    public static final String PD_FLIGHT_TICKS = "bahcraft_flight_ticks";

    public static final int BOMB_SPEED = -5;

    public static void beginLaunch(@NotNull Player player) {
        if (!player.getPersistentData().getBoolean(PD_LAUNCH)) {
            player.getPersistentData().putInt(PD_FLIGHT_TICKS, 0);
        }
        player.getPersistentData().putBoolean(PD_LAUNCH, true);
    }

    public static void tickLaunch(@NotNull Player player) {
        int t = player.getPersistentData().getInt(PD_FLIGHT_TICKS);
        player.getPersistentData().putInt(PD_FLIGHT_TICKS, t + 1);
    }

    public static int getLaunchTicks(@NotNull Player player) {
        return player.getPersistentData().getInt(PD_FLIGHT_TICKS);
    }

    public static void endLaunch(@NotNull Player player) {
        player.getPersistentData().putBoolean(PD_LAUNCH, false);
        player.getPersistentData().putInt(PD_FLIGHT_TICKS, 0);
    }

    public static void spinTick(@NotNull Level level, @NotNull Player player, @NotNull ItemStack stack, int timer) {

        if (player.onGround()) {
            if (!level.isClientSide) {
                clearLaunchState(player);
            }
            return;
        }

        // Server: authoritative flight state + motion
        if (!level.isClientSide) {
            beginLaunch(player);
            stack.getOrCreateTag().putBoolean(TAG_LAUNCH, true);

            if (!player.isFallFlying()) {
                player.startFallFlying();
            }

            Vec3 look = player.getLookAngle();
            double speed = 1.8;
            player.setDeltaMovement(look.x * speed, look.y * speed, look.z * speed);
            player.hurtMarked = true;

            int ticksUsed = 72000 - timer;
            if (ticksUsed % 20 == 0) {
                spawnThunderAt(level, Vec3.atCenterOf(player.blockPosition()), 1.0f, 1.0f);
            }
        }

        // Client: particles only
        if (level.isClientSide) {
            for (int i = 0; i < 3; i++) {
                level.addParticle(
                        ParticleTypes.FIREWORK,
                        player.getX() + (level.random.nextDouble() - 0.5),
                        player.getY(),
                        player.getZ() + (level.random.nextDouble() - 0.5),
                        0, 0, 0
                );
            }
        }
    }

    public static void clearLaunchState(@NotNull Player player) {
        player.getPersistentData().putBoolean(PD_LAUNCH, false);
    }

    public static void toggleMode(@NotNull Level level, @NotNull Player player, @NotNull ItemStack stack, @NotNull String tag) {
        CompoundTag cTag = stack.getOrCreateTag();
        boolean newMode = !cTag.getBoolean(tag);
        cTag.putBoolean(tag, newMode);

        if (!newMode) {
            removeGodMode(player);
        }

        level.playSound(
                null,
                player.getX(), player.getY(), player.getZ(),
                SoundEvents.EXPERIENCE_ORB_PICKUP,
                SoundSource.PLAYERS,
                0.5f, 1.0f
        );
    }

    /**
     * Checks if the player landed. Returns TRUE if the tag should be removed (cleared).
     */
    public static boolean checkAndExecuteSmash(@NotNull Level level, @NotNull Player player, @NotNull ItemStack stack) {
        if (player.isDeadOrDying() || player.getAbilities().flying || !isModeActive(stack)) {
            return true;
        }

        // Still in air
        if (!player.onGround()) {
            return false;
        }

        int ticks = getLaunchTicks(player);
        boolean ticksOK = true;  // TODO REMOVE LATER: ticks >= 10;
        boolean crouch = player.isCrouching();
        boolean naturalFall = player.getDeltaMovement().y < -0.07;
        boolean usingThis = player.isUsingItem() && player.getUseItem() == stack;

        if ((((crouch || naturalFall) && !usingThis) || ticksOK) && !level.isClientSide) {
            executeSmashAttack(level, player);
            return true;
        }
        else{
            endLaunch(player);
        }
        return true;
    }

    private static void executeSmashAttack(@NotNull Level level, @NotNull Player player) {
        double radius = 100.0;
        AABB area = player.getBoundingBox().inflate(radius);
        List<LivingEntity> nearbyEntities = level.getEntitiesOfClass(LivingEntity.class, area, e -> true);

        spawnLightningAt(level, Vec3.atCenterOf(player.blockPosition()));

        for (LivingEntity target : nearbyEntities) {
            if (target == player) continue;

            spawnLightningAt(level, target.position());
            forceKill(target, level, level.damageSources().lightningBolt());
        }
    }

    /**
     * Force-kill helpers: allow specifying a custom DamageSource or use the default lightning damage source.
     */
    public static void forceKill(@NotNull LivingEntity target, @NotNull Level level) {
        DamageSource src = level.damageSources().lightningBolt();
        worldActions.forceKill(target, level, src);
    }

    public static void forceKill(@NotNull LivingEntity target, @NotNull Level level, @NotNull DamageSource source) {
        worldActions.forceKill(target, level, source);
    }

    public static void applyGodModeEffects(@NotNull Player player) {
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 1, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 40, 1, false, false));

        if (!player.hasEffect(MobEffects.ABSORPTION)) {
            player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 600, 50, false, false));
        }
    }

    public static void removeGodMode(@NotNull Player player) {
        if (player.hasEffect(MobEffects.DAMAGE_RESISTANCE)) player.removeEffect(MobEffects.DAMAGE_RESISTANCE);
        if (player.hasEffect(MobEffects.REGENERATION)) player.removeEffect(MobEffects.REGENERATION);
        if (player.hasEffect(MobEffects.ABSORPTION)) player.removeEffect(MobEffects.ABSORPTION);
    }

    public static boolean isModeActive(@NotNull ItemStack stack) {
        return isModeActive(stack, TAG_MODE);
    }

    public static boolean isModeActive(@NotNull ItemStack stack, @NotNull String modeTag) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.getBoolean(modeTag);
    }

    public static void cancelSmash(@NotNull ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null) {
            tag.putBoolean(TAG_LAUNCH, false);
        }
    }

    /**
     * Spawns a lightning bolt at the given position (server-side only).
     */
    public static void spawnLightningAt(@NotNull Level level, @NotNull Vec3 position) {
        worldActions.spawnLightningAt(level, position, false);
    }

    /**
     * Spawns a lightning bolt and optionally plays thunder at the given position (server-side only).
     */
    public static void spawnLightningAt(@NotNull Level level, @NotNull Vec3 position, boolean playThunder) {
        worldActions.spawnLightningAt(level, position, playThunder);
    }

    /**
     * Spawn thunder sound at position (server-side only). Convenience helper.
     */

    public static void spawnThunderAt(@NotNull Level level, @NotNull Vec3 position, float volume, float pitch) {
        worldActions.spawnThunderAt(level, position, volume, pitch);
    }

    /**
     * Convenience method: spawn lightning at block center and optionally destroy the block (dropping loot).
     */
    public static void strikeBlockWithLightning(@NotNull Level level, @NotNull BlockPos pos, boolean dropItems) {
        worldActions.strikeBlockWithLightning(level, pos, dropItems);
    }
}
