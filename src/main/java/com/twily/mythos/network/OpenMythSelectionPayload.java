package com.twily.mythos.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.List;

public record OpenMythSelectionPayload(List<MythSelectionEntry> myths, Identifier currentMyth, boolean canClose) implements CustomPacketPayload {

    public static final Type<OpenMythSelectionPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("mythos", "open_selection"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenMythSelectionPayload> STREAM_CODEC = StreamCodec.composite(
        MythSelectionEntry.STREAM_CODEC.apply(ByteBufCodecs.list()),
        OpenMythSelectionPayload::myths,
        Identifier.STREAM_CODEC,
        OpenMythSelectionPayload::currentMyth,
        ByteBufCodecs.BOOL,
        OpenMythSelectionPayload::canClose,
        OpenMythSelectionPayload::new
    );

    @Override
    public Type<OpenMythSelectionPayload> type() {
        return TYPE;
    }
}
