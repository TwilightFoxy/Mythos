package com.twily.mythos.network;

import com.twily.mythos.Mythos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ToggleFairyFlightModePayload() implements CustomPacketPayload {

    public static final Type<ToggleFairyFlightModePayload> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "toggle_fairy_flight_mode"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ToggleFairyFlightModePayload> STREAM_CODEC =
        StreamCodec.unit(new ToggleFairyFlightModePayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
