package com.twily.mythos.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

import java.util.List;

public record MythDefinition(
    Identifier id,
    Identifier icon,
    int order,
    int complexity,
    String description,
    List<String> advantages,
    List<String> disadvantages,
    List<String> guideFeatures,
    List<String> guideCrafting,
    List<Identifier> powers
) {

    public static final Codec<MythFile> FILE_CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Identifier.CODEC.optionalFieldOf("icon", Identifier.fromNamespaceAndPath("minecraft", "book")).forGetter(MythFile::icon),
        Codec.INT.optionalFieldOf("order", 0).forGetter(MythFile::order),
        Codec.INT.optionalFieldOf("complexity", 1).forGetter(MythFile::complexity),
        Codec.STRING.optionalFieldOf("description", "").forGetter(MythFile::description),
        Codec.STRING.listOf().optionalFieldOf("advantages", List.of()).forGetter(MythFile::advantages),
        Codec.STRING.listOf().optionalFieldOf("disadvantages", List.of()).forGetter(MythFile::disadvantages),
        Codec.STRING.listOf().optionalFieldOf("guide_features", List.of()).forGetter(MythFile::guideFeatures),
        Codec.STRING.listOf().optionalFieldOf("guide_crafting", List.of()).forGetter(MythFile::guideCrafting),
        Identifier.CODEC.listOf().fieldOf("powers").forGetter(MythFile::powers)
    ).apply(instance, MythFile::new));

    public static MythDefinition fromFile(Identifier id, MythFile file) {
        String description = file.description().isBlank()
            ? "myth." + id.getNamespace() + "." + id.getPath() + ".description"
            : file.description();

        return new MythDefinition(
            id,
            file.icon(),
            file.order(),
            Math.max(1, Math.min(file.complexity(), 3)),
            description,
            List.copyOf(file.advantages()),
            List.copyOf(file.disadvantages()),
            List.copyOf(file.guideFeatures()),
            List.copyOf(file.guideCrafting()),
            List.copyOf(file.powers())
        );
    }

    public record MythFile(
        Identifier icon,
        int order,
        int complexity,
        String description,
        List<String> advantages,
        List<String> disadvantages,
        List<String> guideFeatures,
        List<String> guideCrafting,
        List<Identifier> powers
    ) {
    }
}
