package com.utils_bahcraft.network;

import com.utils_bahcraft.network.packets.WindImpulseS2CPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class ModNet {
    private static final String PROTOCOL = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation("utils_bahcraft", "main"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals
    );

    public static void register() {
        int id = 0;
        CHANNEL.messageBuilder(WindImpulseS2CPacket.class, id++)
                .encoder(WindImpulseS2CPacket::encode)
                .decoder(WindImpulseS2CPacket::decode)
                .consumerMainThread(WindImpulseS2CPacket::handle)
                .add();
    }
}
