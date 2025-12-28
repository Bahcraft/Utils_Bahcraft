package com.utils_bahcraft.items;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.EquipmentSlot;

public class LightningHammerItem extends Item {
    private static final String TAG_MODE = "LightningMode";

    public LightningHammerItem(Properties properties) {
        super(properties);
    }

    /**
     * 1. HANDLE AIR CLICKS (For Toggling Mode)
     * This fires when you click the air.
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isCrouching())
            return InteractionResultHolder.pass(stack);
        if (!level.isClientSide) {
            toggleMode(stack, player);
        }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
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
        Vec3 positionClicked = Vec3.atBottomCenterOf(context.getClickedPos().above());

        if (!level.isClientSide()) {
            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
            if (bolt != null) {
                bolt.moveTo(positionClicked);
                level.addFreshEntity(bolt);
            }
        }
        return InteractionResult.SUCCESS;
    }

    /**
     * 3. HANDLE ENTITY HITS (Lightning on Mobs/Players)
     * This fires when you Left-Click an enemy.
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

        // 1. ATTACK DAMAGE
        // 5.0 means it deals 5 hearts (plus base player strength)
//            builder.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Tool modifier",
//                    5.0,
//                    AttributeModifier.Operation.ADDITION));

        // 2. ATTACK SPEED
        // -2.4 is standard for a Sword (4.0 - 2.4 = 1.6 speed)
        // -3.0 is slow (Axe)
        // 0.0 is fast (Hand speed)
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