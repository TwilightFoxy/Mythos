package com.twily.mythos.client;

import com.twily.mythos.Mythos;
import com.twily.mythos.gameplay.StarWandererMythHandler;
import com.twily.mythos.myth.MythState;
import com.twily.mythos.registry.MythosAttachments;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

@EventBusSubscriber(modid = Mythos.MOD_ID, value = Dist.CLIENT)
public final class StarWandererEnergyOverlay {

    private static final Identifier STAR_WANDERER = Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "star_wanderer");
    private static final int BAR_WIDTH = 82;
    private static final int BAR_HEIGHT = 6;
    private static final int SEGMENTS = 20;
    private static final int BACKGROUND = 0xAA0D1120;
    private static final int BORDER = 0xCC596AA6;
    private static final int EMPTY = 0xAA1C2238;
    private static final int FILLED = 0xFF9AD8FF;
    private static final int FULL = 0xFFFFFFFF;

    private StarWandererEnergyOverlay() {
    }

    @SubscribeEvent
    public static void onExperienceLayer(RenderGuiLayerEvent.Pre event) {
        if (!VanillaGuiLayers.EXPERIENCE_LEVEL.equals(event.getName())) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !MythState.matches(minecraft.player, STAR_WANDERER)) {
            return;
        }

        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        int x = width / 2 - BAR_WIDTH / 2;
        int y = height - 55;
        int energy = minecraft.player.getData(MythosAttachments.STAR_WANDERER_ENERGY);
        boolean full = energy >= StarWandererMythHandler.MAX_ENERGY;

        event.getGuiGraphics().fill(x - 1, y - 1, x + BAR_WIDTH + 1, y + BAR_HEIGHT + 1, BORDER);
        event.getGuiGraphics().fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, BACKGROUND);

        int filled = Math.round((energy / (float) StarWandererMythHandler.MAX_ENERGY) * SEGMENTS);
        int segmentWidth = 3;
        int gap = 1;
        for (int i = 0; i < SEGMENTS; i++) {
            int segmentX = x + 1 + i * (segmentWidth + gap);
            int color = i < filled ? (full ? FULL : FILLED) : EMPTY;
            event.getGuiGraphics().fill(segmentX, y + 1, segmentX + segmentWidth, y + BAR_HEIGHT - 1, color);
        }

        event.getGuiGraphics().text(minecraft.font, Component.translatable("gui.mythos.star_wanderer_energy"), x, y - 9, full ? FULL : FILLED, false);
    }
}
