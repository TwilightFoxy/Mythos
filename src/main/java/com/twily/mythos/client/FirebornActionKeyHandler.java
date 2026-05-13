package com.twily.mythos.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.twily.mythos.network.UseFirebornActionPayload;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.lwjgl.glfw.GLFW;

public final class FirebornActionKeyHandler {

    private static final KeyMapping FIREBALL_KEY = new KeyMapping(
        "key.mythos.fireborn_fireball",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_R,
        MythosKeyCategory.MYTHOS
    );
    private static final KeyMapping FIRE_RING_KEY = new KeyMapping(
        "key.mythos.fireborn_ring",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_G,
        MythosKeyCategory.MYTHOS
    );

    private FirebornActionKeyHandler() {
    }

    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(FIREBALL_KEY);
        event.register(FIRE_RING_KEY);
    }

    public static Component fireballKeyName() {
        return FIREBALL_KEY.getTranslatedKeyMessage();
    }

    public static Component ringKeyName() {
        return FIRE_RING_KEY.getTranslatedKeyMessage();
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

            while (FIREBALL_KEY.consumeClick()) {
                ClientPacketDistributor.sendToServer(new UseFirebornActionPayload("fireball"));
            }
            while (FIRE_RING_KEY.consumeClick()) {
                ClientPacketDistributor.sendToServer(new UseFirebornActionPayload("ring"));
            }
        }
    }
}
