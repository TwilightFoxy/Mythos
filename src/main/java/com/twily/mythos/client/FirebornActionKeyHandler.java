package com.twily.mythos.client;

import com.twily.mythos.gameplay.FirebornMythHandler;
import com.twily.mythos.myth.MythState;
import com.twily.mythos.network.UseFirebornActionPayload;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

public final class FirebornActionKeyHandler {

    private FirebornActionKeyHandler() {
    }

    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
    }

    public static Component fireballKeyName() {
        return MythosSkillKeys.skill1Name();
    }

    public static Component ringKeyName() {
        return MythosSkillKeys.skill2Name();
    }

    public static final class Handler {

        private Handler() {
        }

        @SubscribeEvent
        public static void onKeyInput(InputEvent.Key event) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == null || minecraft.screen != null || !MythState.matches(minecraft.player, FirebornMythHandler.FIREBORN_ID)) {
                return;
            }

            while (MythosSkillKeys.skill1().consumeClick()) {
                ClientPacketDistributor.sendToServer(new UseFirebornActionPayload("fireball"));
            }
            while (MythosSkillKeys.skill2().consumeClick()) {
                ClientPacketDistributor.sendToServer(new UseFirebornActionPayload("ring"));
            }
        }
    }
}
