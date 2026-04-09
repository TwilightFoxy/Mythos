package com.twily.mythos.network;

import com.twily.mythos.Mythos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record UseKitsuneActionPayload(String action) implements CustomPacketPayload {

    public static final Type<UseKitsuneActionPayload> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "use_kitsune_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, UseKitsuneActionPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8,
        UseKitsuneActionPayload::action,
        UseKitsuneActionPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
