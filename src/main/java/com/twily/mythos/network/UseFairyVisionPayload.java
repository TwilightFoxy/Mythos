package com.twily.mythos.network;

import com.twily.mythos.Mythos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record UseFairyVisionPayload() implements CustomPacketPayload {

    public static final Type<UseFairyVisionPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "use_fairy_vision"));
    public static final StreamCodec<RegistryFriendlyByteBuf, UseFairyVisionPayload> STREAM_CODEC = StreamCodec.unit(new UseFairyVisionPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
