package com.twily.mythos.data;

import com.mojang.serialization.Codec;

import java.util.Arrays;

public enum MythPowerType {
    BIOME_SPEED("biome_speed"),
    RANGED_DAMAGE_MULTIPLIER("ranged_damage_multiplier"),
    ITEM_DAMAGE_MULTIPLIER("item_damage_multiplier"),
    SMITHING_RECIPE_UNLOCK("smithing_recipe_unlock");

    public static final Codec<MythPowerType> CODEC = Codec.STRING.xmap(MythPowerType::byId, MythPowerType::id);

    private final String id;

    MythPowerType(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static MythPowerType byId(String id) {
        return Arrays.stream(values())
            .filter(type -> type.id.equals(id))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown myth power type: " + id));
    }
}
