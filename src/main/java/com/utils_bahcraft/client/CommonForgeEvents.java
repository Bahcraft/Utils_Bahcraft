package com.utils_bahcraft.client;

import com.utils_bahcraft.UtilsBahCraft;
import com.utils_bahcraft.utils.HammerUtils;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = UtilsBahCraft.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CommonForgeEvents {
    @SubscribeEvent
    public static void onLivingFall(LivingFallEvent event) {
        // 1. Check if it is a player
        if (!(event.getEntity() instanceof Player player)) return;

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
}