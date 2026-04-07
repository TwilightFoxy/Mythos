package com.twily.mythos.gameplay;

import com.twily.mythos.Mythos;
import com.twily.mythos.data.MythItemMarkerHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

@EventBusSubscriber(modid = Mythos.MOD_ID)
public final class MythosTooltipHandler {

    private static final String ELVEN_BOW_MARKER = "mythos_elven_bow";
    private static final String DWARVEN_PICKAXE_MARKER = "mythos_dwarven_pickaxe";

    private MythosTooltipHandler() {
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        if (MythItemMarkerHelper.hasMarker(event.getItemStack(), ELVEN_BOW_MARKER)) {
            event.getToolTip().add(Component.translatable("tooltip.mythos.elven_bow").withStyle(ChatFormatting.GRAY));
        }

        if (MythItemMarkerHelper.hasMarker(event.getItemStack(), DWARVEN_PICKAXE_MARKER)) {
            event.getToolTip().add(Component.translatable("tooltip.mythos.dwarven_pickaxe").withStyle(ChatFormatting.GRAY));
        }
    }
}
