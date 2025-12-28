package com.utils_bahcraft;

import com.utils_bahcraft.items.LightningHammerItem; // Make sure this import matches your class name
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod(UtilsBahCraft.MODID)
public class UtilsBahCraft
{
    public static final String MODID = "utils_bahcraft";

    // Item Registry
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);

    // Registering the Lightning Hammer
    public static final RegistryObject<Item> LIGHTNING_HAMMER = ITEMS.register("lightning_hammer",
            () -> new LightningHammerItem(new Item.Properties()));

    public UtilsBahCraft(FMLJavaModLoadingContext context)
    {
        IEventBus modEventBus = context.getModEventBus();

        // Register Items
        ITEMS.register(modEventBus);

        // Register Creative Tab listener
        modEventBus.addListener(this::addCreative);

        // Register global event bus
        MinecraftForge.EVENT_BUS.register(this);
    }

    // Add item to the Creative Tab
    private void addCreative(BuildCreativeModeTabContentsEvent event)
    {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(LIGHTNING_HAMMER);
        }
    }

    // Client-Side Event Bus
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents
    {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event)
        {
            event.enqueueWork(() -> {

                ItemProperties.register(
                        LIGHTNING_HAMMER.get(),
                        new ResourceLocation(MODID, "mode_active"),
                        (stack, level, entity, seed) -> {
                            return stack.getOrCreateTag().getBoolean("LightningMode") ? 1.0F : 0.0F;
                        }
                );
            });
        }
    }
}