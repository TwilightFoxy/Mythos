package com.twily.mythos.network;

import com.twily.mythos.data.MythDefinition;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

import java.util.List;

public record MythSelectionEntry(
    Identifier id,
    Identifier icon,
    int complexity,
    String description,
    String growth,
    List<String> advantages,
    List<String> disadvantages
) {

    public static final StreamCodec<RegistryFriendlyByteBuf, MythSelectionEntry> STREAM_CODEC = StreamCodec.composite(
        Identifier.STREAM_CODEC,
        MythSelectionEntry::id,
        Identifier.STREAM_CODEC,
        MythSelectionEntry::icon,
        ByteBufCodecs.VAR_INT,
        MythSelectionEntry::complexity,
        ByteBufCodecs.STRING_UTF8,
        MythSelectionEntry::description,
        ByteBufCodecs.STRING_UTF8,
        MythSelectionEntry::growth,
        ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()),
        MythSelectionEntry::advantages,
        ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()),
        MythSelectionEntry::disadvantages,
        MythSelectionEntry::new
    );

    public static MythSelectionEntry fromDefinition(MythDefinition definition) {
        return new MythSelectionEntry(
            definition.id(),
            definition.icon(),
            definition.complexity(),
            definition.description(),
            definition.growth(),
            definition.advantages(),
            definition.disadvantages()
        );
    }
}
