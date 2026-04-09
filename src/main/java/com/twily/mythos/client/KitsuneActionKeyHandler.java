package com.twily.mythos.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.twily.mythos.network.UseKitsuneActionPayload;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.lwjgl.glfw.GLFW;

public final class KitsuneActionKeyHandler {

    private static final KeyMapping TOGGLE_MASK_KEY = new KeyMapping(
        "key.mythos.kitsune_mask",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_G,
        MythosKeyCategory.MYTHOS
    );
    private static final KeyMapping DASH_KEY = new KeyMapping(
        "key.mythos.kitsune_dash",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_R,
        MythosKeyCategory.MYTHOS
    );
    private static final KeyMapping FOXFIRE_KEY = new KeyMapping(
        "key.mythos.kitsune_foxfire",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_B,
        MythosKeyCategory.MYTHOS
    );
    private KitsuneActionKeyHandler() {
    }

    public static void registerKeyMappings(net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent event) {
        event.register(TOGGLE_MASK_KEY);
        event.register(DASH_KEY);
        event.register(FOXFIRE_KEY);
    }

    public static Component maskKeyName() {
        return TOGGLE_MASK_KEY.getTranslatedKeyMessage();
    }

    public static Component dashKeyName() {
        return DASH_KEY.getTranslatedKeyMessage();
    }

    public static Component foxfireKeyName() {
        return FOXFIRE_KEY.getTranslatedKeyMessage();
    }

    public static final class Handler {

        private Handler() {
        }

        @SubscribeEvent
        public static void onKeyInput(InputEvent.Key event) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == null || minecraft.screen != null) {
                return;
            }

            while (TOGGLE_MASK_KEY.consumeClick()) {
                ClientPacketDistributor.sendToServer(new UseKitsuneActionPayload("toggle_mask"));
            }
            while (DASH_KEY.consumeClick()) {
                ClientPacketDistributor.sendToServer(new UseKitsuneActionPayload("dash"));
            }
            while (FOXFIRE_KEY.consumeClick()) {
                ClientPacketDistributor.sendToServer(new UseKitsuneActionPayload("foxfire"));
            }
        }
    }
}
