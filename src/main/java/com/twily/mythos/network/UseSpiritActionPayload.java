package com.twily.mythos.network;

import com.twily.mythos.Mythos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record UseSpiritActionPayload() implements CustomPacketPayload {

    public static final Type<UseSpiritActionPayload> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "use_spirit_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, UseSpiritActionPayload> STREAM_CODEC =
        StreamCodec.unit(new UseSpiritActionPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
