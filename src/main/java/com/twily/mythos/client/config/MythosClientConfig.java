package com.twily.mythos.client.config;

import com.electronwill.nightconfig.core.EnumGetMethod;
import java.util.Locale;
import net.minecraft.util.ARGB;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class MythosClientConfig {

    private static final int DEFAULT_KITSUNE_COLOR = ARGB.color(72, 35, 118);
    private static final String DEFAULT_KITSUNE_COLOR_HEX = "#482376";
    private static final String HEX_PATTERN = "^#?[0-9a-fA-F]{6}$";
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec.EnumValue<KitsuneAdornmentColorMode> KITSUNE_COLOR_MODE;
    public static final ModConfigSpec.ConfigValue<String> KITSUNE_MANUAL_COLOR;
    public static final ModConfigSpec SPEC;

    static {
        BUILDER.translation("config.mythos.kitsune").push("kitsune");
        KITSUNE_COLOR_MODE = BUILDER.translation("config.mythos.kitsune.adornment_color_mode").comment(
            "How kitsune mask and tail colors are chosen.",
            "DEFAULT keeps the original purple tone.",
            "AUTO samples a pixel from the player's base skin layer.",
            "MANUAL uses the hex value from manual_color."
        ).defineEnum("adornment_color_mode", KitsuneAdornmentColorMode.DEFAULT, EnumGetMethod.NAME_IGNORECASE);
        KITSUNE_MANUAL_COLOR = BUILDER.translation("config.mythos.kitsune.manual_adornment_color").comment(
            "Manual color for kitsune mask and tail in #RRGGBB format."
        ).define("manual_adornment_color", DEFAULT_KITSUNE_COLOR_HEX, value -> value instanceof String text && text.matches(HEX_PATTERN));
        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    private MythosClientConfig() {
    }

    public static int defaultKitsuneColor() {
        return DEFAULT_KITSUNE_COLOR;
    }

    public static int manualKitsuneColor() {
        String raw = KITSUNE_MANUAL_COLOR.get();
        if (raw == null) {
            return DEFAULT_KITSUNE_COLOR;
        }

        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        if (normalized.startsWith("#")) {
            normalized = normalized.substring(1);
        }

        if (normalized.length() != 6) {
            return DEFAULT_KITSUNE_COLOR;
        }

        try {
            return ARGB.opaque(Integer.parseInt(normalized, 16));
        } catch (NumberFormatException ignored) {
            return DEFAULT_KITSUNE_COLOR;
        }
    }
}
