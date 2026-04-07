package com.twily.mythos.network;

import com.twily.mythos.Mythos;
import com.twily.mythos.data.MythDataManager;
import com.twily.mythos.myth.MythState;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.List;

public final class MythosNetwork {

    private static final String NETWORK_VERSION = "1";

    private MythosNetwork() {
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(NETWORK_VERSION);
        registrar.playToClient(OpenMythGuidePayload.TYPE, OpenMythGuidePayload.STREAM_CODEC);
        registrar.playToClient(OpenMythSelectionPayload.TYPE, OpenMythSelectionPayload.STREAM_CODEC);
        registrar.playToServer(ChooseMythPayload.TYPE, ChooseMythPayload.STREAM_CODEC, MythosNetwork::handleChooseMyth);
    }

    public static void openGuide(ServerPlayer player) {
        List<MythGuideEntry> myths = MythDataManager.mythsInOrder().stream()
            .map(MythGuideEntry::fromDefinition)
            .toList();
        PacketDistributor.sendToPlayer(player, new OpenMythGuidePayload(myths, MythState.get(player)));
    }

    public static void openSelection(ServerPlayer player, boolean canClose) {
        List<MythSelectionEntry> myths = MythDataManager.mythsInOrder().stream()
            .map(MythSelectionEntry::fromDefinition)
            .toList();
        PacketDistributor.sendToPlayer(player, new OpenMythSelectionPayload(myths, MythState.get(player), canClose));
    }

    private static void handleChooseMyth(ChooseMythPayload payload, IPayloadContext context) {
        Player contextPlayer = context.player();
        if (!(contextPlayer instanceof ServerPlayer player)) {
            return;
        }

        if (!MythDataManager.hasMyth(payload.mythId()) || MythState.NONE.equals(payload.mythId())) {
            player.sendSystemMessage(Component.translatable("command.mythos.unknown_myth", payload.mythId().toString()));
            return;
        }

        MythState.set(player, payload.mythId());
        player.sendSystemMessage(Component.translatable("command.mythos.set_myth", MythState.displayName(payload.mythId())));
    }
}
