package com.twily.mythos.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.List;

public record OpenMythGuidePayload(List<MythGuideEntry> myths, Identifier currentMyth) implements CustomPacketPayload {

    public static final Type<OpenMythGuidePayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("mythos", "open_guide"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenMythGuidePayload> STREAM_CODEC = StreamCodec.composite(
        MythGuideEntry.STREAM_CODEC.apply(ByteBufCodecs.list()),
        OpenMythGuidePayload::myths,
        Identifier.STREAM_CODEC,
        OpenMythGuidePayload::currentMyth,
        OpenMythGuidePayload::new
    );

    @Override
    public Type<OpenMythGuidePayload> type() {
        return TYPE;
    }
}
