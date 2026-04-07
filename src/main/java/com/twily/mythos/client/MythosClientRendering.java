package com.twily.mythos.client;

import com.twily.mythos.registry.MythosEntities;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.MinecartRenderer;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

public final class MythosClientRendering {

    private MythosClientRendering() {
    }

    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(MythosEntities.FAIRY_MINECART.get(), context -> new MinecartRenderer(context, ModelLayers.MINECART));
    }

    public static void addLayers(EntityRenderersEvent.AddLayers event) {
        for (var skin : event.getSkins()) {
            var renderer = event.getPlayerRenderer(skin);
            if (renderer != null) {
                renderer.addLayer(new FairyWingsLayer(renderer, event.getEntityModels()));
            }
        }
    }
}
