package com.twily.mythos.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.twily.mythos.network.ToggleFairyFlightModePayload;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.lwjgl.glfw.GLFW;

public final class FairyFlightModeKeyHandler {

    private static final KeyMapping FAIRY_FLIGHT_MODE_KEY = new KeyMapping(
        "key.mythos.fairy_flight_mode",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_R,
        MythosKeyCategory.MYTHOS
    );

    private FairyFlightModeKeyHandler() {
    }

    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(FAIRY_FLIGHT_MODE_KEY);
    }

    public static Component keyName() {
        return FAIRY_FLIGHT_MODE_KEY.getTranslatedKeyMessage();
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

            while (FAIRY_FLIGHT_MODE_KEY.consumeClick()) {
                ClientPacketDistributor.sendToServer(new ToggleFairyFlightModePayload());
            }
        }
    }
}
