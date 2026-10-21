package com.godze.fischfishing;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.SimpleChannel;

public class Network {

    public static final SimpleChannel CHANNEL = ChannelBuilder
            .named(ResourceLocation.fromNamespaceAndPath(FischMod.MODID, "main"))
            .networkProtocolVersion(1)
            .simpleChannel();

    // Server -> Client: open the minigame
    public record StartPacket(float speed, float bar) {
        public static final StreamCodec<RegistryFriendlyByteBuf, StartPacket> CODEC = StreamCodec.composite(
                ByteBufCodecs.FLOAT, StartPacket::speed,
                ByteBufCodecs.FLOAT, StartPacket::bar,
                StartPacket::new);
    }

    // Client -> Server: did the player win?
    public record ResultPacket(boolean won) {
        public static final StreamCodec<RegistryFriendlyByteBuf, ResultPacket> CODEC = StreamCodec.composite(
                ByteBufCodecs.BOOL, ResultPacket::won,
                ResultPacket::new);
    }

    public static void register() {
        CHANNEL.play()
                .clientbound()
                .add(StartPacket.class, StartPacket.CODEC, Network::handleStart)
                .serverbound()
                .add(ResultPacket.class, ResultPacket.CODEC, Network::handleResult);
    }

    private static void handleStart(StartPacket pkt, CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> FishingScreen.open(pkt.speed(), pkt.bar()));
        ctx.setPacketHandled(true);
    }

    private static void handleResult(ResultPacket pkt, CustomPayloadEvent.Context ctx) {
        ServerPlayer player = ctx.getSender();
        ctx.enqueueWork(() -> {
            if (player != null) FishEvents.finish(player, pkt.won());
        });
        ctx.setPacketHandled(true);
    }
}