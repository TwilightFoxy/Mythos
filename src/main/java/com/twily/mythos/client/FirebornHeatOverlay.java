package com.twily.mythos.client;

import com.twily.mythos.Mythos;
import com.twily.mythos.gameplay.FirebornMythHandler;
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
public final class FirebornHeatOverlay {

    private static final Identifier FIREBORN = Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "fireborn");
    private static final int BAR_WIDTH = 82;
    private static final int BAR_HEIGHT = 7;
    private static final int SEGMENTS = 20;
    private static final int BACKGROUND = 0xAA210A05;
    private static final int BORDER = 0xCC6E2A17;
    private static final int EMPTY = 0xAA3A1611;
    private static final int[] FILLS = {
        0xFF5C2A24,
        0xFF8C4B2A,
        0xFFBD7620,
        0xFFFF7F11,
        0xFF59C7FF
    };
    private static final int[] LABELS = {
        0xFFB88A83,
        0xFFD59A73,
        0xFFE7D7B0,
        0xFFFFB347,
        0xFF8DDBFF
    };

    private FirebornHeatOverlay() {
    }

    @SubscribeEvent
    public static void onFoodLayer(RenderGuiLayerEvent.Pre event) {
        if (!VanillaGuiLayers.FOOD_LEVEL.equals(event.getName())) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !MythState.matches(minecraft.player, FIREBORN)) {
            return;
        }

        event.setCanceled(true);
        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        int x = width / 2 + 10;
        int y = height - 49;
        int heat = minecraft.player.getData(MythosAttachments.FIREBORN_HEAT);
        int stage = FirebornMythHandler.heatStage(heat);

        event.getGuiGraphics().fill(x - 1, y - 1, x + BAR_WIDTH + 1, y + BAR_HEIGHT + 1, BORDER);
        event.getGuiGraphics().fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, BACKGROUND);

        int filled = Math.round((heat / 100.0F) * SEGMENTS);
        int segmentWidth = 3;
        int gap = 1;
        for (int i = 0; i < SEGMENTS; i++) {
            int segmentX = x + 1 + i * (segmentWidth + gap);
            int color = i < filled ? FILLS[stage] : EMPTY;
            event.getGuiGraphics().fill(segmentX, y + 1, segmentX + segmentWidth, y + BAR_HEIGHT - 1, color);
        }

        Component label = Component.translatable(FirebornMythHandler.stageTranslationKey(stage));
        event.getGuiGraphics().text(minecraft.font, label, x, y - 9, LABELS[stage], false);
    }
}
