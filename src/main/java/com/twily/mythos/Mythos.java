package com.twily.mythos;

import com.twily.mythos.client.HumanTradeOverlay;
import com.twily.mythos.client.FairyVisionKeyHandler;
import com.twily.mythos.client.KitsuneActionKeyHandler;
import com.twily.mythos.client.OniActionKeyHandler;
import com.twily.mythos.client.MythosKeyCategory;
import com.twily.mythos.client.MythosClientRendering;
import com.twily.mythos.client.MythosClientNetworking;
import com.twily.mythos.client.MythosClientTintSources;
import com.twily.mythos.client.config.MythosClientConfig;
import com.twily.mythos.gameplay.SlimeMythHandler;
import com.twily.mythos.data.MythDataManager;
import com.twily.mythos.network.MythosNetwork;
import com.twily.mythos.registry.MythosAttachments;
import com.twily.mythos.registry.MythosBlocks;
import com.twily.mythos.registry.MythosEntities;
import com.twily.mythos.registry.MythosEffects;
import com.twily.mythos.registry.MythosItems;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

@Mod(Mythos.MOD_ID)
public final class Mythos {

    public static final String MOD_ID = "mythos";

    public Mythos(IEventBus modBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.CLIENT, MythosClientConfig.SPEC);
        MythosAttachments.register(modBus);
        MythosBlocks.register(modBus);
        MythosEntities.register(modBus);
        MythosEffects.register(modBus);
        MythosItems.register(modBus);
        modBus.addListener(MythosNetwork::registerPayloads);
        if (FMLEnvironment.getDist() == Dist.CLIENT) {
            modContainer.registerExtensionPoint(IConfigScreenFactory.class, (IConfigScreenFactory) (container, parent) -> new ConfigurationScreen(container, parent));
            modBus.addListener(MythosClientNetworking::registerClientPayloads);
            modBus.addListener(MythosKeyCategory::register);
            modBus.addListener(FairyVisionKeyHandler::registerKeyMappings);
            modBus.addListener(KitsuneActionKeyHandler::registerKeyMappings);
            modBus.addListener(OniActionKeyHandler::registerKeyMappings);
            modBus.addListener(MythosClientRendering::registerRenderers);
            modBus.addListener(MythosClientRendering::registerLayerDefinitions);
            modBus.addListener(MythosClientRendering::addLayers);
            modBus.addListener(MythosClientTintSources::registerTintSources);
            NeoForge.EVENT_BUS.register(new HumanTradeOverlay());
            NeoForge.EVENT_BUS.register(FairyVisionKeyHandler.Handler.class);
            NeoForge.EVENT_BUS.register(KitsuneActionKeyHandler.Handler.class);
            NeoForge.EVENT_BUS.register(OniActionKeyHandler.Handler.class);
            // Tail debug bindings are intentionally disabled in normal builds.
            // To bring them back for asset tuning, re-enable KitsuneTailDebugKeyHandler here.
        }
        NeoForge.EVENT_BUS.register(MythDataManager.class);
        NeoForge.EVENT_BUS.register(SlimeMythHandler.class);
    }
}
