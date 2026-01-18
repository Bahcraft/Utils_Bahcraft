package com.utils_bahcraft.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class MoveCheckBypassMixin {

    @Shadow public ServerPlayer player;

    @Redirect(
            method = "handleMovePlayer",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;teleport(DDDFF)V"
            )
    )
    private void utils_bahcraft$skipCorrectionTeleport(ServerGamePacketListenerImpl self,
                                                       double x, double y, double z,
                                                       float yaw, float pitch) {
        if (player != null && player.getPersistentData().contains("WindWandTimer")) {
            return;
        }
        self.teleport(x, y, z, yaw, pitch);
    }
}
