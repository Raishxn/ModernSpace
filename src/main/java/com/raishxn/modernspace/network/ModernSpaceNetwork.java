package com.raishxn.modernspace.network;

import com.raishxn.modernspace.ModernSpace;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ModernSpaceNetwork {

    private static final String VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(ModernSpace.id("main"),
            () -> VERSION, VERSION::equals, VERSION::equals);

    private ModernSpaceNetwork() {}

    public static <T> void sendToPlayer(T packet, net.minecraft.server.level.ServerPlayer player) {
        INSTANCE.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static <T> void sendToAll(T packet) {
        INSTANCE.send(net.minecraftforge.network.PacketDistributor.ALL.noArg(), packet);
    }

    public static void register() {
        int id = 0;
        INSTANCE.messageBuilder(SPersonalSpaceWorldList.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SPersonalSpaceWorldList::encode).decoder(SPersonalSpaceWorldList::decode)
                .consumerNetworkThread(SPersonalSpaceWorldList::handle).add();
        INSTANCE.messageBuilder(CPersonalSpaceChangeSettings.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(CPersonalSpaceChangeSettings::encode).decoder(CPersonalSpaceChangeSettings::decode)
                .consumerNetworkThread(CPersonalSpaceChangeSettings::handle).add();
    }
}
