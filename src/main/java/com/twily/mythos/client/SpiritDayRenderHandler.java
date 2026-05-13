package com.twily.mythos.client;

import com.twily.mythos.Mythos;
import com.twily.mythos.gameplay.SpiritMythHandler;
import com.twily.mythos.myth.MythState;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;

@EventBusSubscriber(modid = Mythos.MOD_ID, value = Dist.CLIENT)
public final class SpiritDayRenderHandler {

    private SpiritDayRenderHandler() {
    }

    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Pre<?> event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        if (!(minecraft.level.getEntity(event.getRenderState().id) instanceof Player player)) {
            return;
        }

        if (MythState.matches(player, SpiritMythHandler.SPIRIT_ID) && SpiritMythHandler.isDaytimeSpirit(player.level())) {
            event.getRenderState().isInvisible = true;
        }
    }
}
