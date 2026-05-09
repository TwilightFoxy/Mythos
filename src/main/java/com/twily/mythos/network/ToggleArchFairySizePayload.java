package com.twily.mythos.network;

import com.twily.mythos.Mythos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ToggleArchFairySizePayload() implements CustomPacketPayload {

    public static final Type<ToggleArchFairySizePayload> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "toggle_arch_fairy_size"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ToggleArchFairySizePayload> STREAM_CODEC =
        StreamCodec.unit(new ToggleArchFairySizePayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
