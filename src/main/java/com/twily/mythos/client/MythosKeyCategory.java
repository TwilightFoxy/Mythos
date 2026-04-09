package com.twily.mythos.client;

import com.twily.mythos.Mythos;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

public final class MythosKeyCategory {

    public static final KeyMapping.Category MYTHOS = new KeyMapping.Category(Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "mythos"));

    private MythosKeyCategory() {
    }

    public static void register(RegisterKeyMappingsEvent event) {
        event.registerCategory(MYTHOS);
    }
}
