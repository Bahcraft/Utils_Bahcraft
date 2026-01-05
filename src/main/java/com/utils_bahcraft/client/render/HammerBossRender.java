package com.utils_bahcraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.utils_bahcraft.entities.HammerBossEntity;
import com.utils_bahcraft.geomodel.HammerBossModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.layer.*;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public class HammerBossRender extends GeoEntityRenderer<HammerBossEntity> {
    public HammerBossRender(EntityRendererProvider.Context renderManager) {
        super(renderManager, new HammerBossModel());

        addRenderLayer(new AutoGlowingGeoLayer<>(this));

        addRenderLayer(new HammerBossHeldItemLayer(this));

        this.shadowRadius = 2.0f;
    }

    @Override
    public void render(HammerBossEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {

        poseStack.pushPose();

        poseStack.scale(3.0f, 3.0f, 3.0f);

        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);

        poseStack.popPose();
    }

    @Override
    protected float getDeathMaxRotation(HammerBossEntity entityLivingBaseIn) {
        return 0.0F;
    }
}
