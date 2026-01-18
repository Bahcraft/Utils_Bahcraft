package com.utils_bahcraft.network.packets;

import com.utils_bahcraft.client.ClientWindPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record WindImpulseS2CPacket(double x, double y, double z) {

    public static void encode(WindImpulseS2CPacket msg, FriendlyByteBuf buf) {
        buf.writeDouble(msg.x);
        buf.writeDouble(msg.y);
        buf.writeDouble(msg.z);
    }

    public static WindImpulseS2CPacket decode(FriendlyByteBuf buf) {
        return new WindImpulseS2CPacket(buf.readDouble(), buf.readDouble(), buf.readDouble());
    }

    public static void handle(WindImpulseS2CPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientWindPacketHandler.handleWindImpulse(msg))
        );
        ctx.get().setPacketHandled(true);
    }
}
