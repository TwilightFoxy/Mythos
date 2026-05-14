package com.twily.mythos.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record UseStarWandererActionPayload(String action) implements CustomPacketPayload {

    public static final Type<UseStarWandererActionPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("mythos", "use_star_wanderer_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, UseStarWandererActionPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8,
        UseStarWandererActionPayload::action,
        UseStarWandererActionPayload::new
    );

    @Override
    public Type<UseStarWandererActionPayload> type() {
        return TYPE;
    }
}
