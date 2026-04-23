package com.twily.mythos.network;

import com.twily.mythos.Mythos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record UseOniActionPayload() implements CustomPacketPayload {

    public static final Type<UseOniActionPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "use_oni_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, UseOniActionPayload> STREAM_CODEC = StreamCodec.unit(new UseOniActionPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
