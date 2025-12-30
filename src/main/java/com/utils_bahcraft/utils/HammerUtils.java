package com.utils_bahcraft.utils;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class HammerUtils {

    // Shared Constants
    public static final String TAG_MODE = "LightningMode";
    public static final String TAG_LAUNCH = "HammerLaunch";

    /**
     * LOGIC: Handles the Super Jump / Launch physics and sound.
     */
    public static void launchPlayer(Level level, Player player, ItemStack stack, float launchAmount) {
        // 1. Launch Physics
        Vec3 currentVel = player.getDeltaMovement();
        player.setDeltaMovement(currentVel.x, launchAmount, currentVel.z);
        player.hurtMarked = true;

        // 2. Sound
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.DRAGON_FIREBALL_EXPLODE, SoundSource.PLAYERS, 0.5f, 0.5f);

        // 3. Set Tag
        stack.getOrCreateTag().putBoolean(TAG_LAUNCH, true);
    }

    /**
     * LOGIC: Toggles the mode on/off and plays the sound.
     */
    public static void toggleMode(Level level, Player player, ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        boolean newMode = !tag.getBoolean(TAG_MODE);
        tag.putBoolean(TAG_MODE, newMode);

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.5f, 1.0f);
    }

    /**
     * LOGIC: Checks if the player has landed and executes the smash attack.
     * Returns TRUE if the smash happened (so we can reset the tag).
     */
    public static boolean checkAndExecuteSmash(Level level, Player player) {
        player.resetFallDistance();

        // If still in air, do nothing
        if (!player.onGround() || player.getDeltaMovement().y >= 0.1) {
            return false;
        }

        if (!level.isClientSide) {
            executeSmashAttack(level, player);
        }
        return true;
    }

    /**
     * LOGIC: Area of Effect Lightning Attack.
     */
    private static void executeSmashAttack(Level level, Player player) {
        double radius = 100.0;
        AABB area = player.getBoundingBox().inflate(radius);
        List<LivingEntity> nearbyEntities = level.getEntitiesOfClass(LivingEntity.class, area, e -> true);

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.TRIDENT_THUNDER, SoundSource.PLAYERS, 2.0f, 1.0f);

        for (LivingEntity target : nearbyEntities) {
            if (target == player) continue;

            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
            if (bolt == null) continue;

            bolt.moveTo(target.position());
            level.addFreshEntity(bolt);

            forceKill(target, level);
        }
    }

    /**
     * LOGIC: Instant Kill + Head Drop.
     */
    public static void forceKill(LivingEntity target, Level level) {
        if (level.isClientSide) return;

        dropHead(target, level);

        if (target instanceof ServerPlayer player) {
            player.getAbilities().invulnerable = false;
            player.onUpdateAbilities();
        }
        target.invulnerableTime = 0;

        target.hurt(level.damageSources().lightningBolt(), Float.MAX_VALUE);
    }

    /**
     * LOGIC: Determines which head to drop and spawns it.
     */
    private static void dropHead(LivingEntity target, Level level) {
        ItemStack headStack = null;

        if (target instanceof Player player) {
            headStack = new ItemStack(Items.PLAYER_HEAD);
            CompoundTag tag = headStack.getOrCreateTag();
            CompoundTag skullOwner = new CompoundTag();
            skullOwner.putUUID("Id", player.getUUID());
            skullOwner.putString("Name", player.getGameProfile().getName());
            tag.put("SkullOwner", skullOwner);
        }
        else if (target instanceof net.minecraft.world.entity.monster.Zombie) {
            headStack = new ItemStack(Items.ZOMBIE_HEAD);
        }
        else if (target instanceof net.minecraft.world.entity.monster.Skeleton) {
            headStack = new ItemStack(Items.SKELETON_SKULL);
        }
        else if (target instanceof net.minecraft.world.entity.monster.WitherSkeleton) {
            headStack = new ItemStack(Items.WITHER_SKELETON_SKULL);
        }
        else if (target instanceof net.minecraft.world.entity.monster.Creeper) {
            headStack = new ItemStack(Items.CREEPER_HEAD);
        }
        else if (target instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon) {
            headStack = new ItemStack(Items.DRAGON_HEAD);
        }

        if (headStack != null) {
            net.minecraft.world.entity.item.ItemEntity drop =
                    new net.minecraft.world.entity.item.ItemEntity(level, target.getX(), target.getY(), target.getZ(), headStack);
            drop.setPickUpDelay(0);
            drop.setInvulnerable(true);
            level.addFreshEntity(drop);
        }
    }

    public static void applyGodModeEffects(Player player) {
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 40, 20, false, false));
    }

    public static boolean isModeActive(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.getBoolean(TAG_MODE);
    }
}