package com.twily.mythos;

import com.twily.mythos.client.HumanTradeOverlay;
import com.twily.mythos.client.MythosClientNetworking;
import com.twily.mythos.data.MythDataManager;
import com.twily.mythos.network.MythosNetwork;
import com.twily.mythos.registry.MythosAttachments;
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
        MythosEffects.register(modBus);
        MythosItems.register(modBus);
        modBus.addListener(MythosNetwork::registerPayloads);
        if (FMLEnvironment.getDist() == Dist.CLIENT) {
            modBus.addListener(MythosClientNetworking::registerClientPayloads);
            NeoForge.EVENT_BUS.register(new HumanTradeOverlay());
        }
        NeoForge.EVENT_BUS.register(MythDataManager.class);
    }
}
