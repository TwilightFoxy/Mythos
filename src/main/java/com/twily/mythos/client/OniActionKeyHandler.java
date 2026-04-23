package com.twily.mythos.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.twily.mythos.network.UseOniActionPayload;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.lwjgl.glfw.GLFW;

public final class OniActionKeyHandler {

    private static final KeyMapping BATTLE_FORM_KEY = new KeyMapping(
        "key.mythos.oni_battle_form",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_G,
        MythosKeyCategory.MYTHOS
    );

    private OniActionKeyHandler() {
    }

    public static void registerKeyMappings(net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent event) {
        event.register(BATTLE_FORM_KEY);
    }

    public static Component battleFormKeyName() {
        return BATTLE_FORM_KEY.getTranslatedKeyMessage();
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

            while (BATTLE_FORM_KEY.consumeClick()) {
                ClientPacketDistributor.sendToServer(new UseOniActionPayload());
            }
        }
    }
}
