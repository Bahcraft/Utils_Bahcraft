package com.utils_bahcraft.client.render;

import com.utils_bahcraft.entities.HammerBossEntity;
import com.utils_bahcraft.geomodel.HammerBossModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public class HammerBossRender extends GeoEntityRenderer<HammerBossEntity> {
    public HammerBossRender(EntityRendererProvider.Context renderManager) {
        super(renderManager, new HammerBossModel());
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }


}
