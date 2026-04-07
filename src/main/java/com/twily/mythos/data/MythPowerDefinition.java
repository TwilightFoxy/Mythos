package com.twily.mythos.data;

import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;
import net.minecraft.util.GsonHelper;

import java.util.Optional;

public record MythPowerDefinition(
    Identifier id,
    MythPowerType type,
    double multiplier,
    int amplifier,
    Optional<Identifier> biomeTag,
    Optional<Identifier> itemTag,
    Optional<String> resultMarker,
    Optional<String> denyMessage
) {

    public static MythPowerDefinition fromJson(Identifier id, JsonObject json) {
        MythPowerType type = MythPowerType.byId(GsonHelper.getAsString(json, "type"));
        double multiplier = GsonHelper.getAsDouble(json, "multiplier", 1.0D);
        int amplifier = GsonHelper.getAsInt(json, "amplifier", 0);
        Optional<Identifier> biomeTag = optionalIdentifier(json, "biome_tag");
        Optional<Identifier> itemTag = optionalIdentifier(json, "item_tag");
        Optional<String> resultMarker = optionalString(json, "result_marker");
        Optional<String> denyMessage = optionalString(json, "deny_message");

        switch (type) {
            case BIOME_SPEED -> requireField(id, type, biomeTag.isPresent(), "biome_tag");
            case ITEM_DAMAGE_MULTIPLIER -> requireField(id, type, itemTag.isPresent(), "item_tag");
            case SMITHING_RECIPE_UNLOCK -> requireField(id, type, resultMarker.isPresent(), "result_marker");
            case RANGED_DAMAGE_MULTIPLIER -> {
            }
        }

        return new MythPowerDefinition(id, type, multiplier, amplifier, biomeTag, itemTag, resultMarker, denyMessage);
    }

    private static Optional<Identifier> optionalIdentifier(JsonObject json, String key) {
        return json.has(key) ? Optional.of(Identifier.parse(GsonHelper.getAsString(json, key))) : Optional.empty();
    }

    private static Optional<String> optionalString(JsonObject json, String key) {
        return json.has(key) ? Optional.of(GsonHelper.getAsString(json, key)) : Optional.empty();
    }

    private static void requireField(Identifier id, MythPowerType type, boolean present, String fieldName) {
        if (!present) {
            throw new IllegalArgumentException("Power " + id + " of type " + type.id() + " requires field '" + fieldName + "'");
        }
    }
}
