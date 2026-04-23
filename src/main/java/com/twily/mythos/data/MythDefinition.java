package com.twily.mythos.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

import java.util.List;

public record MythDefinition(
    Identifier id,
    Identifier icon,
    boolean hidden,
    int order,
    int complexity,
    String description,
    String growth,
    List<String> advantages,
    List<String> disadvantages,
    List<String> guideFeatures,
    List<String> guideCrafting,
    List<Identifier> powers
) {

    public static final Codec<MythFile> FILE_CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Identifier.CODEC.optionalFieldOf("icon", Identifier.fromNamespaceAndPath("minecraft", "book")).forGetter(MythFile::icon),
        Codec.BOOL.optionalFieldOf("hidden", false).forGetter(MythFile::hidden),
        Codec.INT.optionalFieldOf("order", 0).forGetter(MythFile::order),
        Codec.INT.optionalFieldOf("complexity", 1).forGetter(MythFile::complexity),
        Codec.STRING.optionalFieldOf("description", "").forGetter(MythFile::description),
        Codec.STRING.optionalFieldOf("growth", "").forGetter(MythFile::growth),
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
        String growth = file.growth().isBlank()
            ? "myth." + id.getNamespace() + "." + id.getPath() + ".growth"
            : file.growth();

        return new MythDefinition(
            id,
            file.icon(),
            file.hidden(),
            file.order(),
            Math.max(1, Math.min(file.complexity(), 3)),
            description,
            growth,
            List.copyOf(file.advantages()),
            List.copyOf(file.disadvantages()),
            List.copyOf(file.guideFeatures()),
            List.copyOf(file.guideCrafting()),
            List.copyOf(file.powers())
        );
    }

    public record MythFile(
        Identifier icon,
        boolean hidden,
        int order,
        int complexity,
        String description,
        String growth,
        List<String> advantages,
        List<String> disadvantages,
        List<String> guideFeatures,
        List<String> guideCrafting,
        List<Identifier> powers
    ) {
    }
}
