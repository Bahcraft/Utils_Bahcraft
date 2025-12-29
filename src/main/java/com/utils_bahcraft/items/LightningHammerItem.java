package com.utils_bahcraft.items;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
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

public class LightningHammerItem extends Item {
    private static final String TAG_MODE = "LightningMode";
    private static final String TAG_LAUNCH = "HammerLaunch"; // Internal tag to track the jump

    public LightningHammerItem(Properties properties) {
        super(properties);
    }

    /**
     * 1. HANDLE AIR CLICKS (Mode Toggle + Super Jump)
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (player.getXRot() > 80 && player.onGround() && !isModeActive(stack) ) {
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
            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
            if (bolt != null) {
                bolt.moveTo(target.position());
                level.addFreshEntity(bolt);
            }
            new Thread(() -> {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                level.getServer().execute(() -> {
                    if (target.isAlive()) {
                        target.invulnerableTime = 0;
                        target.hurt(level.damageSources().lightningBolt(), Float.MAX_VALUE);
                    }
                });
            }).start();
        }
        return true;
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        if (slot != EquipmentSlot.MAINHAND) {
            return super.getDefaultAttributeModifiers(slot);
        }

        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();

        // Attack Speed (Infinite = Instant Cooldown)
        builder.put(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_UUID, "Tool modifier",
                Float.POSITIVE_INFINITY,
                AttributeModifier.Operation.ADDITION));

        return builder.build();
    }

    // --- HELPER METHODS ---

    @Override
    public @NotNull Component getName(ItemStack stack) {
        if (isModeActive(stack)) {
            return Component.literal("Martelão On")
                    .withStyle(ChatFormatting.GOLD)
                    .withStyle(ChatFormatting.BOLD);
        } else {
            return Component.literal("Martelão Off")
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


    private void forceKill(LivingEntity target, Level level) {
        if (level.isClientSide) return;

        target.invulnerableTime = 0;

        boolean damaged = target.hurt(level.damageSources().lightningBolt(), Float.MAX_VALUE);

        if (!damaged) {
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