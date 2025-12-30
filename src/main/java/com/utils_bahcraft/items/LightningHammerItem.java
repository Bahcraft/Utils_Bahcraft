package com.utils_bahcraft.items;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.EquipmentSlot;

import java.util.List;
import java.util.UUID;

public class LightningHammerItem extends Item {
    private static final String TAG_MODE = "LightningMode";
    private static final String TAG_LAUNCH = "HammerLaunch";
    private static final UUID KNOCKBACK_UUID = UUID.fromString("2f3d9178-6f6f-45d2-a3c3-5684534f4342");
    private static final UUID MOVEMENT_UUID = UUID.fromString("1a2b3c4d-1e2f-3a4b-5c6d-7e8f9a0b1c2d");
    private static final UUID HEALTH_UUID = UUID.fromString("7d8e9f0a-1b2c-3d4e-5f6a-7b8c9d0e1f2a");

    public LightningHammerItem(Properties properties) {
        super(properties);
    }

    /**
     * 1. HANDLE AIR CLICKS (Mode Toggle + Super Jump)
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (player.getXRot() > 80 && player.onGround() && isModeActive(stack) ) {
            if (!level.isClientSide) {
                // 1. Launch the player Upward
                Vec3 currentVel = player.getDeltaMovement();
                // Set Y to 2.5 (Very High Jump), keep X and Z momentum
                player.setDeltaMovement(currentVel.x, 2.5, currentVel.z);
                player.hurtMarked = true;

                // 2. Play Sound
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.DRAGON_FIREBALL_EXPLODE, SoundSource.PLAYERS, 0.5f, 0.5f);

                stack.getOrCreateTag().putBoolean(TAG_LAUNCH, true);
            }
            return InteractionResultHolder.success(stack);
        }
        // -------------------------------

        if (!player.isCrouching()) {
            return InteractionResultHolder.pass(stack);
        }

        if (!level.isClientSide) {
            toggleMode(stack, player);
            return InteractionResultHolder.consume(stack);
        }

        return InteractionResultHolder.pass(stack);
    }

    /**
     * 4. PREVENT FALL DAMAGE (The Trick)
     * This runs every tick while the item is in your inventory.
     */
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (entity instanceof Player player && isSelected) {
            CompoundTag tag = stack.getOrCreateTag();

            if(isModeActive(stack)){
                player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 40, 20, false, false));
            }

            if (tag.getBoolean(TAG_MODE)) {
                player.setHealth(Float.POSITIVE_INFINITY);
            }

            if (tag.getBoolean(TAG_LAUNCH)) {
                player.resetFallDistance();
                if (!player.onGround() || player.getDeltaMovement().y >= 0.1)
                    return;

                if (!level.isClientSide) {
                    executeSmashAttack(level, player);
                }

                // Turn off the launch state
                tag.putBoolean(TAG_LAUNCH, false);

            }
        }
    }

    /**
     * 2. HANDLE BLOCK CLICKS (For Lightning)
     */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();

        if (player != null && player.isCrouching()) {
            return InteractionResult.PASS;
        }

        if (!isModeActive(context.getItemInHand())) {
            return InteractionResult.PASS;
        }

        Level level = context.getLevel();

        if (level.isClientSide) {
            return InteractionResult.PASS;
        }

        Vec3 positionClicked = Vec3.atBottomCenterOf(context.getClickedPos().above());
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt != null) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 5, 5, false, false));
            bolt.moveTo(positionClicked);
            level.addFreshEntity(bolt);
        }

        return InteractionResult.SUCCESS;
    }

    /**
     * 3. HANDLE ENTITY HITS (Lightning on Mobs/Players)
     */
    @Override
    public boolean hurtEnemy(@NotNull ItemStack stack, @NotNull LivingEntity target, @NotNull LivingEntity attacker) {
        if (!isModeActive(stack)) {
            return super.hurtEnemy(stack, target, attacker);
        }

        Level level = target.level();
        if (!level.isClientSide()) {

            // 2. Spawn Lightning Visuals
            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
            if (bolt != null) {
                bolt.moveTo(target.position());
                level.addFreshEntity(bolt);
            }

            // 3. Handle the Damage Logic
            new Thread(() -> {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                level.getServer().execute(() -> {
                    if (target.isAlive()) {
                        target.invulnerableTime = 0;
                        forceKill(target, level);
                    }
                });
            }).start();
        }

        return true;
    }

    @Override
    public boolean onLeftClickEntity(ItemStack stack, Player player, Entity entity) {
        // 1. Guard: If client-side, let vanilla handle it
        if (player.level().isClientSide) {
            return super.onLeftClickEntity(stack, player, entity);
        }

        // 2. Guard: If target isn't a LivingEntity, let vanilla handle it
        if (!(entity instanceof LivingEntity target)) {
            return super.onLeftClickEntity(stack, player, entity);
        }

        // 3. Guard: If mode is OFF, let vanilla handle it
        if (!isModeActive(stack)) {
            return super.onLeftClickEntity(stack, player, entity);
        }

        // --- EXECUTE GOD MODE LOGIC ---

        // 1. Reset I-Frames so they take damage immediately
        target.invulnerableTime = 0;

        // 2. Visuals: Spawn lightning for effect
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(player.level());
        if (bolt != null) {
            bolt.moveTo(target.position());
            player.level().addFreshEntity(bolt);
        }

        // 3. Force Kill Logic
        forceKill(target, player.level());

        // 4. Return true to cancel the vanilla attack (we handled it manually)
        return true;
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        if (slot != EquipmentSlot.MAINHAND) {
            return super.getDefaultAttributeModifiers(slot);
        }

        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();

        // Attack Speed (Infinite = Instant Cooldown)
        if (isModeActive(stack)) {
            builder.put(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_UUID, "Tool modifier",
                    Float.POSITIVE_INFINITY,
                    AttributeModifier.Operation.ADDITION));
            builder.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Tool modifier",
                    Float.POSITIVE_INFINITY,
                    AttributeModifier.Operation.ADDITION));
            builder.put(Attributes.ATTACK_KNOCKBACK, new AttributeModifier(KNOCKBACK_UUID, "Weapon knockback",
                    50.0f, AttributeModifier.Operation.ADDITION));
            builder.put(Attributes.MOVEMENT_SPEED, new AttributeModifier(MOVEMENT_UUID, "Weapon speed",
                    0.15f, AttributeModifier.Operation.ADDITION));
        }
        else{
            builder.put(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_UUID, "Tool modifier",
                    15.0f,
                    AttributeModifier.Operation.ADDITION));
            builder.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Tool modifier",
                    15.0f,
                    AttributeModifier.Operation.ADDITION));
            builder.put(Attributes.ATTACK_KNOCKBACK, new AttributeModifier(KNOCKBACK_UUID, "Weapon knockback",
                    5.0f, AttributeModifier.Operation.ADDITION));
        }


        return builder.build();
    }

    // --- HELPER METHODS ---

    @Override
    public @NotNull Component getName(ItemStack stack) {
        if (isModeActive(stack)) {
            return Component.literal("Martelão")
                    .withStyle(ChatFormatting.GOLD)
                    .withStyle(ChatFormatting.BOLD);
        } else {
            return Component.literal("Martelão")
                    .withStyle(ChatFormatting.GRAY);
        }
    }

    private void executeSmashAttack(Level level, Player player) {
        double radius = 100.0;

        AABB area = player.getBoundingBox().inflate(radius);

        List<LivingEntity> nearbyEntities = level.getEntitiesOfClass(LivingEntity.class, area);

        // 4. Play a massive thunder sound for impact
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

    private void dropHead(LivingEntity target, Level level) {
        ItemStack headStack = null;

        // 1. Check for Player (Get their skin)
        if (target instanceof Player player) {
            headStack = new ItemStack(net.minecraft.world.item.Items.PLAYER_HEAD);
            headStack.getOrCreateTag().putString("SkullOwner", player.getGameProfile().getName());
        }
        // 2. Check for Mobs
        else if (target instanceof net.minecraft.world.entity.monster.Zombie) {
            headStack = new ItemStack(net.minecraft.world.item.Items.ZOMBIE_HEAD);
        }
        else if (target instanceof net.minecraft.world.entity.monster.Skeleton) {
            headStack = new ItemStack(net.minecraft.world.item.Items.SKELETON_SKULL);
        }
        else if (target instanceof net.minecraft.world.entity.monster.WitherSkeleton) {
            headStack = new ItemStack(net.minecraft.world.item.Items.WITHER_SKELETON_SKULL);
        }
        else if (target instanceof net.minecraft.world.entity.monster.Creeper) {
            headStack = new ItemStack(net.minecraft.world.item.Items.CREEPER_HEAD);
        }
        else if (target instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon) {
            headStack = new ItemStack(net.minecraft.world.item.Items.DRAGON_HEAD);
        }

        // 3. Spawn the item
        if (headStack != null) {
            net.minecraft.world.entity.item.ItemEntity drop =
                    new net.minecraft.world.entity.item.ItemEntity(level, target.getX(), target.getY(), target.getZ(), headStack);

            drop.setPickUpDelay(0);
            drop.setInvulnerable(true);
            level.addFreshEntity(drop);
        }
    }


    private void forceKill(LivingEntity target, Level level) {
        if (level.isClientSide) return;

        target.invulnerableTime = 0;

        // 2. Try Standard Damage (for visuals/sound)
        dropHead(target, level);
        boolean damaged = target.hurt(level.damageSources().lightningBolt(), Float.MAX_VALUE);
        if (!damaged) {
            target.invulnerableTime = 0;
            target.hurt(level.damageSources().fellOutOfWorld(), Float.MAX_VALUE);

            target.setHealth(0.0F);
            target.kill();
        }

    }

    public boolean isModeActive(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.getBoolean(TAG_MODE);
    }

    private void toggleMode(ItemStack stack, Player player) {
        CompoundTag tag = stack.getOrCreateTag();
        boolean currentMode = tag.getBoolean(TAG_MODE);
        boolean newMode = !currentMode;

        tag.putBoolean(TAG_MODE, newMode);

        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.5f, 1.0f);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return isModeActive(stack);
    }
}