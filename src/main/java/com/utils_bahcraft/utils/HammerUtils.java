package com.utils_bahcraft.utils;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
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

    public static void launchPlayer(Level level, Player player, ItemStack stack, float amount) {
        Vec3 currentVel = player.getDeltaMovement();
        player.setDeltaMovement(currentVel.x, amount, currentVel.z);
        player.hurtMarked = true;

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.DRAGON_FIREBALL_EXPLODE, SoundSource.PLAYERS, 0.5f, 0.5f);

        stack.getOrCreateTag().putBoolean(TAG_LAUNCH, true);
    }

    public static void toggleMode(Level level, Player player, ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        boolean newMode = !tag.getBoolean(TAG_MODE);
        tag.putBoolean(TAG_MODE, newMode);

        if (!newMode) {
            removeGodMode(player);
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.5f, 1.0f);
    }

    /**
     * Checks if the player landed. Returns TRUE if the tag should be removed (cleared).
     */
    public static boolean checkAndExecuteSmash(Level level, Player player, ItemStack stack) {
        // Flight: If they started flying in Creative, Cancel.
        if (player.getAbilities().flying) {
            return true;
        }

        // Death: If they died mid-air, Cancel.
        if (player.isDeadOrDying()) {
            return true;
        }

        // Physics Check: Still in air? Keep waiting.
        if (!player.onGround() || player.getDeltaMovement().y >= 0.1) {
            return false;
        }

        //Landed successfully: Smash!
        if (!level.isClientSide) {
            executeSmashAttack(level, player);
        }
        return true;
    }

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
            ItemEntity drop =
                    new ItemEntity(level, target.getX(), target.getY(), target.getZ(), headStack);
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
        if (player.hasEffect(MobEffects.DAMAGE_RESISTANCE))
            player.removeEffect(MobEffects.DAMAGE_RESISTANCE);
        if (player.hasEffect(MobEffects.REGENERATION))
            player.removeEffect(MobEffects.REGENERATION);
        if (player.hasEffect(MobEffects.ABSORPTION))
            player.removeEffect(MobEffects.ABSORPTION);
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