package com.twily.mythos.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.twily.mythos.network.ToggleArchFairySizePayload;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.lwjgl.glfw.GLFW;

public final class ArchFairySizeKeyHandler {

    private static final KeyMapping ARCH_FAIRY_SIZE_KEY = new KeyMapping(
        "key.mythos.arch_fairy_size",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_X,
        MythosKeyCategory.MYTHOS
    );

    private ArchFairySizeKeyHandler() {
    }

    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(ARCH_FAIRY_SIZE_KEY);
    }

    public static Component keyName() {
        return ARCH_FAIRY_SIZE_KEY.getTranslatedKeyMessage();
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

            while (ARCH_FAIRY_SIZE_KEY.consumeClick()) {
                ClientPacketDistributor.sendToServer(new ToggleArchFairySizePayload());
            }
        }
    }
}
