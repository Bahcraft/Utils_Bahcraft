package com.utils_bahcraft.utils;

import com.utils_bahcraft.UtilsBahCraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class HammerUtils {

    public static final String TAG_MODE = "LightningMode";
    public static final String TAG_LAUNCH = "HammerLaunch";

    public static final String PD_LAUNCH = "bahcraft_launch_pd";
    public static final String PD_FLIGHT_TICKS = "bahcraft_flight_ticks";

    public static final int BOMB_SPEED = -5;

    public static void beginLaunch(Player player) {
        if (!player.getPersistentData().getBoolean(PD_LAUNCH)) {
            player.getPersistentData().putInt(PD_FLIGHT_TICKS, 0);
        }
        player.getPersistentData().putBoolean(PD_LAUNCH, true);
    }

    public static void tickLaunch(Player player) {
        int t = player.getPersistentData().getInt(PD_FLIGHT_TICKS);
        player.getPersistentData().putInt(PD_FLIGHT_TICKS, t + 1);
    }

    public static int getLaunchTicks(Player player) {
        return player.getPersistentData().getInt(PD_FLIGHT_TICKS);
    }

    public static void endLaunch(Player player) {
        player.getPersistentData().putBoolean(PD_LAUNCH, false);
        player.getPersistentData().putInt(PD_FLIGHT_TICKS, 0);
    }

    public static void spinTick(Level level, Player player, ItemStack stack, int timer) {

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
                level.playSound(
                        null,
                        player.getX(), player.getY(), player.getZ(),
                        SoundEvents.LIGHTNING_BOLT_THUNDER,
                        SoundSource.PLAYERS,
                        1.0f, 1.0f
                );
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

    public static void clearLaunchState(Player player) {
        player.getPersistentData().putBoolean(PD_LAUNCH, false);
    }

    public static void clearLaunchTagIfHoldingHammer(Player player) {
        ItemStack main = player.getMainHandItem();
        if (main.getItem() == UtilsBahCraft.LIGHTNING_HAMMER.get()) {
            main.getOrCreateTag().putBoolean(TAG_LAUNCH, false);
        }

        ItemStack off = player.getOffhandItem();
        if (off.getItem() == UtilsBahCraft.LIGHTNING_HAMMER.get()) {
            off.getOrCreateTag().putBoolean(TAG_LAUNCH, false);
        }
    }

    public static void toggleMode(Level level, Player player, ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        boolean newMode = !tag.getBoolean(TAG_MODE);
        tag.putBoolean(TAG_MODE, newMode);

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
    public static boolean checkAndExecuteSmash(Level level, Player player, ItemStack stack) {
        if (player.isDeadOrDying() || player.getAbilities().flying || !isModeActive(stack)) {
            return true;
        }

        // Still in air
        if (!player.onGround()) {
            return false;
        }

        int ticks = getLaunchTicks(player);
        boolean ticksOK = ticks >= 20;
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

    private static void executeSmashAttack(Level level, Player player) {
        double radius = 100.0;
        AABB area = player.getBoundingBox().inflate(radius);
        List<LivingEntity> nearbyEntities = level.getEntitiesOfClass(LivingEntity.class, area, e -> true);

        level.playSound(
                null,
                player.getX(), player.getY(), player.getZ(),
                SoundEvents.TRIDENT_THUNDER,
                SoundSource.PLAYERS,
                2.0f, 1.0f
        );

        for (LivingEntity target : nearbyEntities) {
            if (target == player) continue;

            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
            if (bolt == null) continue;

            bolt.moveTo(target.position());
            level.addFreshEntity(bolt);
            forceKill(target, level);
        }
    }

    public static void forceKill(LivingEntity target, Level level) {
        if (level.isClientSide) return;

        dropHead(target, level);

        target.invulnerableTime = 0;
        target.hurt(level.damageSources().lightningBolt(), Float.MAX_VALUE);
    }

    private static void dropHead(LivingEntity target, Level level) {
        ItemStack headStack = null;

        if (target instanceof Player player) {
            headStack = new ItemStack(Items.PLAYER_HEAD);
            CompoundTag tag = headStack.getOrCreateTag();
            CompoundTag skullOwner = new CompoundTag();
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

    public static void applyGodModeEffects(Player player) {
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 1, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 40, 1, false, false));

        if (!player.hasEffect(MobEffects.ABSORPTION)) {
            player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 600, 50, false, false));
        }
    }

    public static void removeGodMode(Player player) {
        if (player.hasEffect(MobEffects.DAMAGE_RESISTANCE)) player.removeEffect(MobEffects.DAMAGE_RESISTANCE);
        if (player.hasEffect(MobEffects.REGENERATION)) player.removeEffect(MobEffects.REGENERATION);
        if (player.hasEffect(MobEffects.ABSORPTION)) player.removeEffect(MobEffects.ABSORPTION);
    }

    public static boolean isModeActive(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.getBoolean(TAG_MODE);
    }

    public static void cancelSmash(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null) {
            tag.putBoolean(TAG_LAUNCH, false);
        }
    }
}
