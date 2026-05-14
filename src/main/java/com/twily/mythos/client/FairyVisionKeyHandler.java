package com.twily.mythos.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.twily.mythos.Mythos;
import com.twily.mythos.gameplay.FairyMythHandler;
import com.twily.mythos.myth.MythState;
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

    private static final net.minecraft.resources.Identifier FAIRY_ID = net.minecraft.resources.Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "fairy");

    private FairyVisionKeyHandler() {
    }

    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
    }

    public static Component keyName() {
        return MythosSkillKeys.skill2Name();
    }

    public static final class Handler {

        private Handler() {
        }

        @SubscribeEvent
        public static void onKeyInput(InputEvent.Key event) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == null || minecraft.screen != null || !MythState.matches(minecraft.player, FAIRY_ID)) {
                return;
            }

            while (MythosSkillKeys.skill2().consumeClick()) {
                ClientPacketDistributor.sendToServer(new UseFairyVisionPayload());
            }
        }
    }
}
