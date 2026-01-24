package com.utils_bahcraft.items.hammer;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.utils_bahcraft.utils.HammerUtils;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class FlyingHammerItem extends LightningHammerItem {
    public static final String FLYING_TAG_MODE = "FlyingMode";

    public FlyingHammerItem(Properties properties) {
        super(properties);
    }

    @Override
    protected boolean shouldSpawnLightning() {
        return false;
    }

    @Override
    public @NotNull String getModeTag() {
        return FLYING_TAG_MODE;
    }

    @Override
    protected @NotNull String getHammerName() {
        return "Martelao que Voa";
    }

    @Override
    public @NotNull InteractionResult interactLivingEntity(@NotNull ItemStack stack, @NotNull Player player, @NotNull LivingEntity interactionTarget, @NotNull InteractionHand usedHand) {
        return InteractionResult.PASS;
    }

    @Override
    public boolean onLeftClickEntity(@NotNull ItemStack stack, @NotNull Player player, @NotNull Entity entity) {
        return false;
    }

    @Override
    public boolean hurtEnemy(@NotNull ItemStack stack, @NotNull LivingEntity target, @NotNull LivingEntity attacker) {
        return false;
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        if (slot != EquipmentSlot.MAINHAND) {
            return super.getDefaultAttributeModifiers(slot);
        }

        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        builder.put(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_UUID, "Tool modifier",
                0.0f, AttributeModifier.Operation.ADDITION));
        builder.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Tool modifier",
                0.0f, AttributeModifier.Operation.ADDITION));
        builder.put(Attributes.ATTACK_KNOCKBACK, new AttributeModifier(KNOCKBACK_UUID, "Weapon knockback",
                0.0f, AttributeModifier.Operation.ADDITION));

        if (HammerUtils.isModeActive(stack, getModeTag())) {
            builder.put(Attributes.MOVEMENT_SPEED, new AttributeModifier(MOVEMENT_UUID, "Weapon speed",
                    0.15f, AttributeModifier.Operation.ADDITION));
        }

        return builder.build();
    }

}
