package com.utils_bahcraft.items.wind_wand;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(net.minecraft.server.network.ServerGamePacketListenerImpl.class)
public abstract class MoveCheckMixin {
    @Shadow
    public net.minecraft.server.level.ServerPlayer player;

    @Inject(method = "handleMovePlayer", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;teleport(DDDFF)V"),
            cancellable = true)
    private void bahcraft$skipRubberband(net.minecraft.network.protocol.game.ServerboundMovePlayerPacket pkt,
                                         CallbackInfo ci) {
        if (player.getPersistentData().contains("WindWandTimer")) {
            // Skip the correction branch for this effect only
            ci.cancel();
        }
    }
}