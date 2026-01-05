package com.utils_bahcraft.items.wind_wand;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class WindWand extends Item {
    public WindWand(Properties properties) {
        super(properties);
    }



    @Override
    public boolean onLeftClickEntity(ItemStack stack, Player player, Entity entity) {
        if (!player.level().isClientSide && entity instanceof LivingEntity livingTarget) {

            // 1. Initialize the "Wind Timer" to 0
            livingTarget.getPersistentData().putInt("WindWandTimer", 0);

            player.level().playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.PHANTOM_FLAP, net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.0f);

            return true;
        }
        return super.onLeftClickEntity(stack, player, entity);
    }
}
