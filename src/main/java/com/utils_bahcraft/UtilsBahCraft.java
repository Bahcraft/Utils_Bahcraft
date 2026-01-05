package com.utils_bahcraft;

import com.mojang.logging.LogUtils;
import net.minecraftforge.common.ForgeSpawnEggItem;
import com.utils_bahcraft.client.render.HammerBossRender;
import com.utils_bahcraft.entities.HammerBossEntity;
import com.utils_bahcraft.items.LightningHammerItem; // Make sure this import matches your class name
import com.utils_bahcraft.items.SimpleLightningHammerItem;
import com.utils_bahcraft.utils.HammerUtils;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;
import software.bernie.geckolib.GeckoLib;

@Mod(UtilsBahCraft.MODID)
public class UtilsBahCraft
{
    public static final String MODID = "utils_bahcraft";
    public static final Logger LOGGER = LogUtils.getLogger();

    // Item Registry
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    // Registering the Lightning Hammer
    public static final RegistryObject<Item> LIGHTNING_HAMMER = ITEMS.register("lightning_hammer",
            () -> new LightningHammerItem(new Item.Properties()
                    .stacksTo(1)
                    .fireResistant()));

    // Registering the Simple Lightning Hammer
    public static final RegistryObject<Item> SIMPLE_LIGHTNING_HAMMER = ITEMS.register("simple_lightning_hammer",
            () -> new SimpleLightningHammerItem(new Item.Properties()
                    .stacksTo(1)
                    .fireResistant()));

    // Registering the Balanced Hammer
    public static final RegistryObject<Item> BALANCED_HAMMER = ITEMS.register("balanced_hammer",
            () -> new com.utils_bahcraft.items.BalancedHammerItem(new Item.Properties()
                    .stacksTo(1)
                    .fireResistant()));

    public static final RegistryObject<EntityType<HammerBossEntity>> HAMMER_BOSS =
            ENTITY_TYPES.register("boss",
                    () -> EntityType.Builder.of(HammerBossEntity::new, MobCategory.MONSTER)
                            .sized(5f, 13.0f)
                            .fireImmune()
                            .build(new ResourceLocation(MODID, "boss").toString()));

    // SPAWN EGG REGISTRATION
    public static final RegistryObject<Item> HAMMER_BOSS_SPAWN_EGG = ITEMS.register("hammer_boss_spawn_egg",
            () -> new ForgeSpawnEggItem(HAMMER_BOSS, 0x222222, 0x00D9FF,
                    new Item.Properties()));

    // 3. Register the Custom Tab
    public static final RegistryObject<CreativeModeTab> BAHCRAFT_TAB = CREATIVE_MODE_TABS.register("bahcraft_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("creativetab.utils_bahcraft"))
                    .icon(() -> LIGHTNING_HAMMER.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(LIGHTNING_HAMMER.get());
                        output.accept(SIMPLE_LIGHTNING_HAMMER.get());
                        output.accept(BALANCED_HAMMER.get());
                        output.accept(HAMMER_BOSS_SPAWN_EGG.get());
                    })
                    .build());

    public UtilsBahCraft()
    {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register Items
        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        ENTITY_TYPES.register(modEventBus);

        // Register Creative Tab listener
        modEventBus.addListener(this::addCreative);

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        GeckoLib.initialize();

        // Register global event bus
        MinecraftForge.EVENT_BUS.register(this);
    }

    // Add item to the Creative Tab
    private void addCreative(BuildCreativeModeTabContentsEvent event)
    {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
//            event.accept(LIGHTNING_HAMMER);
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

                // Register item property for model overrides
                ItemProperties.register(
                        LIGHTNING_HAMMER.get(),
                        new ResourceLocation(MODID, "mode_active"),
                        (stack, level, entity, seed) -> {
                            boolean isActive = stack.hasTag() && stack.getTag().getBoolean("LightningMode");
                            float value = isActive ? 1.0F : 0.0F;
                            return value;
                        }
                );

                // Register property for the simple hammer as well so it can share the same model behavior
                ItemProperties.register(
                        SIMPLE_LIGHTNING_HAMMER.get(),
                        new ResourceLocation(MODID, "mode_active"),
                        (stack, level, entity, seed) -> {
                            boolean isActive = stack.hasTag() && stack.getTag().getBoolean("SIMPLE_HAMMER_MODE");
                            float value = isActive ? 1.0F : 0.0F;
                            return value;
                        }
                );

                // Balanced hammer property registration
                ItemProperties.register(
                        BALANCED_HAMMER.get(),
                        new ResourceLocation(MODID, "mode_active"),
                        (stack, level, entity, seed) -> {
                            boolean isActive = stack.hasTag() && stack.getTag().getBoolean(HammerUtils.BALANCED_TAG_MODE);
                            return isActive ? 1.0F : 0.0F;
                        }
                );
            });
        }
        @SubscribeEvent
        public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(UtilsBahCraft.HAMMER_BOSS.get(), HammerBossRender::new);
        }
    }
}