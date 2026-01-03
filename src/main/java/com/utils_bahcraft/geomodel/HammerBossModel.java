package com.utils_bahcraft.geomodel;

import com.utils_bahcraft.UtilsBahCraft;
import com.utils_bahcraft.entities.HammerBossEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class HammerBossModel extends GeoModel<HammerBossEntity> {
    @Override
    public ResourceLocation getModelResource(HammerBossEntity animatable) {
        return new ResourceLocation(UtilsBahCraft.MODID, "geo/boss.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(HammerBossEntity animatable) {
        return new ResourceLocation(UtilsBahCraft.MODID, "textures/entity/boss_full.png");
    }

    @Override
    public ResourceLocation getAnimationResource(HammerBossEntity animatable) {
        return new ResourceLocation(UtilsBahCraft.MODID, "animations/hammer_boss.animation.json");
    }
}
