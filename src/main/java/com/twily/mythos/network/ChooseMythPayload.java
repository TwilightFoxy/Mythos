package com.twily.mythos.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ChooseMythPayload(Identifier mythId) implements CustomPacketPayload {

    public static final Type<ChooseMythPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("mythos", "choose_myth"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ChooseMythPayload> STREAM_CODEC = StreamCodec.composite(
        Identifier.STREAM_CODEC,
        ChooseMythPayload::mythId,
        ChooseMythPayload::new
    );

    @Override
    public Type<ChooseMythPayload> type() {
        return TYPE;
    }
}
