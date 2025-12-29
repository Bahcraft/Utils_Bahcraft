package com.utils_bahcraft;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = UtilsBahCraft.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config
{
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    // --- CONFIG OPTIONS ---

    private static final ForgeConfigSpec.IntValue HAMMER_DAMAGE = BUILDER
            .comment("How much attack damage the Lightning Hammer deals.")
            .defineInRange("hammerDamage", 8, 1, 100); // Default: 8, Min: 1, Max: 100

    private static final ForgeConfigSpec.BooleanValue ALLOW_MODE_SWITCH = BUILDER
            .comment("If set to false, players cannot toggle the Lightning Mode.")
            .define("allowModeSwitch", true);

    // --- BUILD THE SPEC ---
    static final ForgeConfigSpec SPEC = BUILDER.build();

    // --- PUBLIC VARIABLES (Use these in your code) ---
    public static int hammerDamage;
    public static boolean allowModeSwitch;

    // --- LOAD EVENT ---
    @SubscribeEvent
    static void onLoad(final ModConfigEvent event)
    {
        hammerDamage = HAMMER_DAMAGE.get();
        allowModeSwitch = ALLOW_MODE_SWITCH.get();
    }
}