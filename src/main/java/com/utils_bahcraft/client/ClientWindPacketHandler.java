package com.utils_bahcraft.client;

import com.utils_bahcraft.network.packets.WindImpulseS2CPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

public class ClientWindPacketHandler {

    public static void handleWindImpulse(WindImpulseS2CPacket msg) {
        var mc = Minecraft.getInstance();
        var p = mc.player;
        if (p == null) return;

        p.setDeltaMovement(p.getDeltaMovement().add(new Vec3(msg.x(), msg.y(), msg.z())));
        p.hasImpulse = true;
    }
}
