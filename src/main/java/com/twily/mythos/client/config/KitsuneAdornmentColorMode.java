package com.twily.mythos.client.config;

import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.common.TranslatableEnum;

public enum KitsuneAdornmentColorMode implements TranslatableEnum {
    DEFAULT,
    AUTO,
    MANUAL;

    @Override
    public Component getTranslatedName() {
        return Component.translatable("config.mythos.kitsune.adornment_color_mode." + name().toLowerCase());
    }
}
