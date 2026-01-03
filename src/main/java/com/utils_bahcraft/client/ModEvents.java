package com.utils_bahcraft.client;

import com.utils_bahcraft.entities.HammerBossEntity;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import static com.utils_bahcraft.UtilsBahCraft.HAMMER_BOSS;
import static com.utils_bahcraft.UtilsBahCraft.MODID;

@Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModEvents {
    @SubscribeEvent
    public static void entityAttributeEvent(EntityAttributeCreationEvent event) {
        event.put(HAMMER_BOSS.get(), HammerBossEntity.createAttributes().build());
    }
}
