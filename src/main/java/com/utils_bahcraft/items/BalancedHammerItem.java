package com.utils_bahcraft.items;

import com.utils_bahcraft.utils.HammerUtils;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import org.jetbrains.annotations.NotNull;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.EquipmentSlot;

public class BalancedHammerItem extends LightningHammerItem {

    public BalancedHammerItem(Properties properties) {
        super(properties);
    }

    @Override
    protected @NotNull String getModeTag() {
        return HammerUtils.BALANCED_TAG_MODE;
    }

    @Override
    protected @NotNull String getHammerName() {
        return "Martelão Balanceado";
    }

    @Override
    protected boolean shouldStartUsingWhenModeActive() {
        // Balanced hammer behaves like main hammer in that it can start using
        return true;
    }

    @Override
    public @NotNull UseAnim getUseAnimation(@NotNull ItemStack stack) {
        return UseAnim.SPEAR;
    }

    @Override
    public boolean hurtEnemy(@NotNull ItemStack stack, @NotNull LivingEntity target, @NotNull LivingEntity attacker) {
        // Adjust damage/behavior: apply less extreme force than LightningHammer
        if (!HammerUtils.isModeActive(stack)) {
            return super.hurtEnemy(stack, target, attacker);
        }

        var level = target.level();
        if (!level.isClientSide()) {
            // spawn lightning but less lethal - do not attempt to force kill directly
            HammerUtils.spawnLightningAt(level, target.position());
            // apply a strong but not instant-kill damage
            target.hurt(level.damageSources().magic(), 20.0f);
        }

        return true;
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        if (slot != EquipmentSlot.MAINHAND) {
            return super.getDefaultAttributeModifiers(slot);
        }

        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();

        if (HammerUtils.isModeActive(stack)) {
            // Balanced mode: moderate enhancements
            builder.put(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_UUID, "Tool modifier",
                    1.5f, AttributeModifier.Operation.ADDITION));
            builder.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Tool modifier",
                    8.0f, AttributeModifier.Operation.ADDITION));
            builder.put(Attributes.ATTACK_KNOCKBACK, new AttributeModifier(KNOCKBACK_UUID, "Weapon knockback",
                    2.0f, AttributeModifier.Operation.ADDITION));
            builder.put(Attributes.MOVEMENT_SPEED, new AttributeModifier(MOVEMENT_UUID, "Weapon speed",
                    0.05f, AttributeModifier.Operation.ADDITION));
        } else {
            // Normal mode: slightly above baseline
            builder.put(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_UUID, "Tool modifier",
                    0.5f, AttributeModifier.Operation.ADDITION));
            builder.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Tool modifier",
                    4.0f, AttributeModifier.Operation.ADDITION));
            builder.put(Attributes.ATTACK_KNOCKBACK, new AttributeModifier(KNOCKBACK_UUID, "Weapon knockback",
                    1.0f, AttributeModifier.Operation.ADDITION));
        }

        return builder.build();
    }

}
