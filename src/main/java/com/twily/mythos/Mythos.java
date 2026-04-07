package com.twily.mythos;

import com.twily.mythos.client.HumanTradeOverlay;
import com.twily.mythos.client.FairyVisionKeyHandler;
import com.twily.mythos.client.MythosClientRendering;
import com.twily.mythos.client.MythosClientNetworking;
import com.twily.mythos.data.MythDataManager;
import com.twily.mythos.network.MythosNetwork;
import com.twily.mythos.registry.MythosAttachments;
import com.twily.mythos.registry.MythosEntities;
import com.twily.mythos.registry.MythosEffects;
import com.twily.mythos.registry.MythosItems;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;

@Mod(Mythos.MOD_ID)
public final class Mythos {

    public static final String MOD_ID = "mythos";

    public Mythos(IEventBus modBus, ModContainer modContainer) {
        MythosAttachments.register(modBus);
        MythosEntities.register(modBus);
        MythosEffects.register(modBus);
        MythosItems.register(modBus);
        modBus.addListener(MythosNetwork::registerPayloads);
        if (FMLEnvironment.getDist() == Dist.CLIENT) {
            modBus.addListener(MythosClientNetworking::registerClientPayloads);
            modBus.addListener(FairyVisionKeyHandler::registerKeyMappings);
            modBus.addListener(MythosClientRendering::registerRenderers);
            modBus.addListener(MythosClientRendering::addLayers);
            NeoForge.EVENT_BUS.register(new HumanTradeOverlay());
            NeoForge.EVENT_BUS.register(FairyVisionKeyHandler.Handler.class);
        }
        NeoForge.EVENT_BUS.register(MythDataManager.class);
    }
}
