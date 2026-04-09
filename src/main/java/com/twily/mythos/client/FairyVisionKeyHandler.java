package com.twily.mythos.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.twily.mythos.network.UseFairyVisionPayload;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

public final class FairyVisionKeyHandler {

    private static final KeyMapping FAIRY_VISION_KEY = new KeyMapping(
        "key.mythos.fairy_vision",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_V,
        MythosKeyCategory.MYTHOS
    );

    private FairyVisionKeyHandler() {
    }

    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(FAIRY_VISION_KEY);
    }

    public static Component keyName() {
        return FAIRY_VISION_KEY.getTranslatedKeyMessage();
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

            while (FAIRY_VISION_KEY.consumeClick()) {
                ClientPacketDistributor.sendToServer(new UseFairyVisionPayload());
            }
        }
    }
}
