package com.utils_bahcraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.utils_bahcraft.entities.HammerBossEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer;

import javax.annotation.Nullable;

public class HammerBossHeldItemLayer extends BlockAndItemGeoLayer<HammerBossEntity> {
    public HammerBossHeldItemLayer(GeoRenderer<HammerBossEntity> renderer) {
        super(renderer);
    }

    @Nullable
    @Override
    protected ItemStack getStackForBone(GeoBone bone, HammerBossEntity animatable) {
        // This connects the 'right_arm' bone from your screenshot to the item
        if (bone.getName().equals("right_arm")) {
            return animatable.getMainHandItem();
        }
        return null;
    }

    @Override
    protected ItemDisplayContext getTransformTypeForStack(GeoBone bone, ItemStack stack, HammerBossEntity animatable) {
        if (bone.getName().equals("right_arm")) {
            return ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
        }
        return ItemDisplayContext.NONE;
    }

    @Override
    protected void renderStackForBone(PoseStack poseStack, GeoBone bone, ItemStack stack, HammerBossEntity animatable, MultiBufferSource bufferSource, float partialTick, int packedLight, int packedOverlay) {
        poseStack.pushPose();

        // 1. FIX SIZE: Make the hammer GIANT
        poseStack.mulPose(Axis.XP.rotationDegrees(-90f));
        float scale = 2.5f;
        poseStack.scale(scale, scale, scale);
        poseStack.translate(0, 0.15, 0);


        super.renderStackForBone(poseStack, bone, stack, animatable, bufferSource, partialTick, packedLight, packedOverlay);

        poseStack.popPose();
    }


}