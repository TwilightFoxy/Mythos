package com.twily.mythos.client;

import com.twily.mythos.Mythos;
import com.twily.mythos.client.screen.MythGuideScreen;
import com.twily.mythos.client.screen.MythSelectionScreen;
import com.twily.mythos.network.OpenMythGuidePayload;
import com.twily.mythos.network.OpenMythSelectionPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class MythosClientNetworking {

    private MythosClientNetworking() {
    }

    public static void registerClientPayloads(RegisterClientPayloadHandlersEvent event) {
        event.register(OpenMythGuidePayload.TYPE, MythosClientNetworking::handleOpenGuide);
        event.register(OpenMythSelectionPayload.TYPE, MythosClientNetworking::handleOpenSelection);
    }

    private static void handleOpenGuide(OpenMythGuidePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft minecraft = Minecraft.getInstance();
            minecraft.setScreen(new MythGuideScreen(payload.myths(), payload.currentMyth()));
        });
    }

    private static void handleOpenSelection(OpenMythSelectionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.screen instanceof MythSelectionScreen existing) {
                existing.refresh(payload.myths(), payload.currentMyth(), payload.canClose());
                return;
            }

            minecraft.setScreen(new MythSelectionScreen(payload.myths(), payload.currentMyth(), payload.canClose()));
        });
    }
}
