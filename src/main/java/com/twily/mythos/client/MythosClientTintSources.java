package com.twily.mythos.client;

import com.twily.mythos.Mythos;
import com.twily.mythos.client.color.KitsuneAdornmentTintSource;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

public final class MythosClientTintSources {

    private MythosClientTintSources() {
    }

    public static void registerTintSources(RegisterColorHandlersEvent.ItemTintSources event) {
        event.register(Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "kitsune_adornment"), KitsuneAdornmentTintSource.MAP_CODEC);
    }
}
