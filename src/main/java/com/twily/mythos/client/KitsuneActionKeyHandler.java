package com.twily.mythos.client;

import com.twily.mythos.Mythos;
import com.twily.mythos.myth.MythState;
import com.twily.mythos.network.UseKitsuneActionPayload;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

public final class KitsuneActionKeyHandler {

    private static final net.minecraft.resources.Identifier KITSUNE_ID = net.minecraft.resources.Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "kitsune");
    private KitsuneActionKeyHandler() {
    }

    public static void registerKeyMappings(net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent event) {
    }

    public static Component maskKeyName() {
        return MythosSkillKeys.skill1Name();
    }

    public static Component dashKeyName() {
        return MythosSkillKeys.skill2Name();
    }

    public static Component foxfireKeyName() {
        return MythosSkillKeys.skill3Name();
    }

    public static final class Handler {

        private Handler() {
        }

        @SubscribeEvent
        public static void onKeyInput(InputEvent.Key event) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == null || minecraft.screen != null || !MythState.matches(minecraft.player, KITSUNE_ID)) {
                return;
            }

            while (MythosSkillKeys.skill1().consumeClick()) {
                ClientPacketDistributor.sendToServer(new UseKitsuneActionPayload("toggle_mask"));
            }
            while (MythosSkillKeys.skill2().consumeClick()) {
                ClientPacketDistributor.sendToServer(new UseKitsuneActionPayload("dash"));
            }
            while (MythosSkillKeys.skill3().consumeClick()) {
                ClientPacketDistributor.sendToServer(new UseKitsuneActionPayload("foxfire"));
            }
        }
    }
}
