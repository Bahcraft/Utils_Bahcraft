package com.utils_bahcraft.client;

import com.utils_bahcraft.UtilsBahCraft;
import com.utils_bahcraft.utils.HammerUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = UtilsBahCraft.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CommonForgeEvents {
    @SubscribeEvent
    public static void onLivingFall(LivingFallEvent event) {
        // 1. Check if it is a player
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();

        // 2. Check if Server Side (Damage logic is server-only)
        if (player.level().isClientSide) return;

        // 3. Get the item in hand
        ItemStack stack = player.getMainHandItem();

        // 4. Logic: If holding Hammer AND Launch is Active -> NO DAMAGE
        if (stack.getItem() == UtilsBahCraft.LIGHTNING_HAMMER.get() && stack.getOrCreateTag().getBoolean(HammerUtils.TAG_LAUNCH)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        // 1. We only care if the target is a Player
        if (!(event.getEntity() instanceof Player player)) return;

        // 2. We only care if the damage is Lightning
        if (!event.getSource().is(DamageTypes.LIGHTNING_BOLT) && !event.getSource().is(DamageTypes.ON_FIRE)) {
            return;
        }

        // 3. Check if Player is holding/using the Hammer
        ItemStack stack = player.getUseItem();
        if (stack.getItem() == UtilsBahCraft.LIGHTNING_HAMMER.get() && HammerUtils.isModeActive(stack)) {
            // 4. CANCEL THE EVENT
            event.setCanceled(true);

            // 5. Cleanup Fire
            player.clearFire();
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        Player player = event.player;

        if (player.level().isClientSide) return;
        if (event.phase != TickEvent.Phase.END) return;

        boolean launchActive = player.getPersistentData().getBoolean(HammerUtils.PD_LAUNCH);
        if (!launchActive) return;

        if (player.onGround()) {
            return;
        }

        // Keep elytra stable
        if (!player.isFallFlying()) {
            player.startFallFlying();
        }

        // Count only while fall-flying (elytra)
        if (player.isFallFlying()) {
            HammerUtils.tickLaunch(player);
        }
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        // Only handle server side
        Level level = event.getLevel();
        if (level.isClientSide) return;

        Entity entity = event.getEntity();
        if (!(entity instanceof Player)) return;
        Player player = (Player) entity;

        ItemStack stack = player.getMainHandItem();
        if (stack.getItem() == UtilsBahCraft.SIMPLE_LIGHTNING_HAMMER.get()) {
            BlockPos pos = event.getPos();

            boolean dropItems = !player.isCreative();
            HammerUtils.strikeBlockWithLightning(level, pos, dropItems);

            event.setCanceled(true);
        }
    }
}