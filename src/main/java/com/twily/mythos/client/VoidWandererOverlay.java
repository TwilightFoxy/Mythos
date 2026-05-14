package com.twily.mythos.client;

import com.twily.mythos.Mythos;
import com.twily.mythos.gameplay.VoidWandererMythHandler;
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
public final class VoidWandererOverlay {

    private static final Identifier VOID_WANDERER = Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "void_wanderer");
    private static final int BAR_WIDTH = 82;
    private static final int BAR_HEIGHT = 7;
    private static final int SEGMENTS = 20;
    private static final int BACKGROUND = 0xAA0A0814;
    private static final int BORDER = 0xCC564283;
    private static final int EMPTY = 0xAA151025;
    private static final int FILLED = 0xFF7F66D9;
    private static final int CHARGED = 0xFFB8A2FF;

    private VoidWandererOverlay() {
    }

    @SubscribeEvent
    public static void onFoodLayer(RenderGuiLayerEvent.Pre event) {
        if (!VanillaGuiLayers.FOOD_LEVEL.equals(event.getName())) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !MythState.matches(minecraft.player, VOID_WANDERER)) {
            return;
        }

        event.setCanceled(true);
        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        int x = width / 2 + 10;
        int y = height - 49;
        int energy = minecraft.player.getData(MythosAttachments.VOID_WANDERER_ENERGY);
        boolean charged = energy >= VoidWandererMythHandler.MAX_VOID / 2;

        event.getGuiGraphics().fill(x - 1, y - 1, x + BAR_WIDTH + 1, y + BAR_HEIGHT + 1, BORDER);
        event.getGuiGraphics().fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, BACKGROUND);

        int filled = Math.round((energy / (float) VoidWandererMythHandler.MAX_VOID) * SEGMENTS);
        int segmentWidth = 3;
        int gap = 1;
        for (int i = 0; i < SEGMENTS; i++) {
            int segmentX = x + 1 + i * (segmentWidth + gap);
            int color = i < filled ? (charged ? CHARGED : FILLED) : EMPTY;
            event.getGuiGraphics().fill(segmentX, y + 1, segmentX + segmentWidth, y + BAR_HEIGHT - 1, color);
        }

        event.getGuiGraphics().text(minecraft.font, Component.translatable("gui.mythos.void_wanderer_energy"), x, y - 9, charged ? CHARGED : FILLED, false);
    }
}
