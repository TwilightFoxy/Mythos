package com.twily.mythos.network;

import com.twily.mythos.data.MythDefinition;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

import java.util.List;

public record MythGuideEntry(
    Identifier id,
    Identifier icon,
    int complexity,
    String description,
    List<String> advantages,
    List<String> disadvantages,
    List<String> features,
    List<String> crafting
) {

    public static final StreamCodec<RegistryFriendlyByteBuf, MythGuideEntry> STREAM_CODEC = StreamCodec.composite(
        Identifier.STREAM_CODEC,
        MythGuideEntry::id,
        Identifier.STREAM_CODEC,
        MythGuideEntry::icon,
        ByteBufCodecs.VAR_INT,
        MythGuideEntry::complexity,
        ByteBufCodecs.STRING_UTF8,
        MythGuideEntry::description,
        ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()),
        MythGuideEntry::advantages,
        ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()),
        MythGuideEntry::disadvantages,
        ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()),
        MythGuideEntry::features,
        ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()),
        MythGuideEntry::crafting,
        MythGuideEntry::new
    );

    public static MythGuideEntry fromDefinition(MythDefinition definition) {
        return new MythGuideEntry(
            definition.id(),
            definition.icon(),
            definition.complexity(),
            definition.description(),
            definition.advantages(),
            definition.disadvantages(),
            definition.guideFeatures(),
            definition.guideCrafting()
        );
    }
}
