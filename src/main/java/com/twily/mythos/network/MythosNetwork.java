package com.twily.mythos.network;

import com.twily.mythos.Mythos;
import com.twily.mythos.data.MythDataManager;
import com.twily.mythos.gameplay.ArchFairyMythHandler;
import com.twily.mythos.gameplay.FairyMythHandler;
import com.twily.mythos.gameplay.FirebornMythHandler;
import com.twily.mythos.gameplay.KitsuneMythHandler;
import com.twily.mythos.gameplay.OniMythHandler;
import com.twily.mythos.gameplay.ShulkerbornInventoryHandler;
import com.twily.mythos.gameplay.SpiritMythHandler;
import com.twily.mythos.gameplay.StarWandererMythHandler;
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
        registrar.playToServer(ToggleArchFairySizePayload.TYPE, ToggleArchFairySizePayload.STREAM_CODEC, MythosNetwork::handleToggleArchFairySize);
        registrar.playToServer(ToggleFairyFlightModePayload.TYPE, ToggleFairyFlightModePayload.STREAM_CODEC, MythosNetwork::handleToggleFairyFlightMode);
        registrar.playToServer(UseFairyVisionPayload.TYPE, UseFairyVisionPayload.STREAM_CODEC, MythosNetwork::handleUseFairyVision);
        registrar.playToServer(UseFirebornActionPayload.TYPE, UseFirebornActionPayload.STREAM_CODEC, MythosNetwork::handleUseFirebornAction);
        registrar.playToServer(UseKitsuneActionPayload.TYPE, UseKitsuneActionPayload.STREAM_CODEC, MythosNetwork::handleUseKitsuneAction);
        registrar.playToServer(UseOniActionPayload.TYPE, UseOniActionPayload.STREAM_CODEC, MythosNetwork::handleUseOniAction);
        registrar.playToServer(UseSpiritActionPayload.TYPE, UseSpiritActionPayload.STREAM_CODEC, MythosNetwork::handleUseSpiritAction);
        registrar.playToServer(UseStarWandererActionPayload.TYPE, UseStarWandererActionPayload.STREAM_CODEC, MythosNetwork::handleUseStarWandererAction);
        registrar.playToServer(ClickShulkerbornSlotPayload.TYPE, ClickShulkerbornSlotPayload.STREAM_CODEC, MythosNetwork::handleClickShulkerbornSlot);
    }

    public static void openGuide(ServerPlayer player) {
        List<MythGuideEntry> myths = MythDataManager.visibleMythsInOrder().stream()
            .map(MythGuideEntry::fromDefinition)
            .toList();
        PacketDistributor.sendToPlayer(player, new OpenMythGuidePayload(myths, MythState.get(player)));
    }

    public static void openSelection(ServerPlayer player, boolean canClose) {
        List<MythSelectionEntry> myths = MythDataManager.selectableMythsInOrder(player).stream()
            .map(MythSelectionEntry::fromDefinition)
            .toList();
        PacketDistributor.sendToPlayer(player, new OpenMythSelectionPayload(myths, MythState.get(player), canClose));
    }

    private static void handleChooseMyth(ChooseMythPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player contextPlayer = context.player();
            if (!(contextPlayer instanceof ServerPlayer player)) {
                return;
            }

            if (!MythDataManager.selectableInMenu(player, payload.mythId()) || MythState.NONE.equals(payload.mythId())) {
                player.sendSystemMessage(Component.translatable("command.mythos.unknown_myth", payload.mythId().toString()));
                return;
            }

            MythState.set(player, payload.mythId());
            player.sendSystemMessage(Component.translatable("command.mythos.set_myth", MythState.displayName(payload.mythId())));
        });
    }

    private static void handleUseFairyVision(UseFairyVisionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player contextPlayer = context.player();
            if (contextPlayer instanceof ServerPlayer player) {
                FairyMythHandler.activateFairyVision(player);
            }
        });
    }

    private static void handleToggleFairyFlightMode(ToggleFairyFlightModePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player contextPlayer = context.player();
            if (contextPlayer instanceof ServerPlayer player) {
                FairyMythHandler.toggleFlightMode(player);
            }
        });
    }

    private static void handleToggleArchFairySize(ToggleArchFairySizePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player contextPlayer = context.player();
            if (contextPlayer instanceof ServerPlayer player) {
                ArchFairyMythHandler.toggleSize(player);
            }
        });
    }

    private static void handleUseKitsuneAction(UseKitsuneActionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player contextPlayer = context.player();
            if (!(contextPlayer instanceof ServerPlayer player)) {
                return;
            }

            switch (payload.action()) {
                case "toggle_mask" -> KitsuneMythHandler.toggleMask(player);
                case "dash" -> KitsuneMythHandler.performDash(player);
                case "foxfire" -> KitsuneMythHandler.castFoxfire(player);
                default -> {
                }
            }
        });
    }

    private static void handleUseFirebornAction(UseFirebornActionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player contextPlayer = context.player();
            if (!(contextPlayer instanceof ServerPlayer player)) {
                return;
            }

            switch (payload.action()) {
                case "fireball" -> FirebornMythHandler.castFireball(player);
                case "ring" -> FirebornMythHandler.castFireRing(player);
                default -> {
                }
            }
        });
    }

    private static void handleUseOniAction(UseOniActionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player contextPlayer = context.player();
            if (contextPlayer instanceof ServerPlayer player) {
                OniMythHandler.activateBattleForm(player);
            }
        });
    }

    private static void handleUseSpiritAction(UseSpiritActionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player contextPlayer = context.player();
            if (contextPlayer instanceof ServerPlayer player) {
                SpiritMythHandler.performPhaseTransition(player);
            }
        });
    }

    private static void handleUseStarWandererAction(UseStarWandererActionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player contextPlayer = context.player();
            if (!(contextPlayer instanceof ServerPlayer player)) {
                return;
            }

            switch (payload.action()) {
                case "step" -> StarWandererMythHandler.performStarStep(player);
                case "beam_start" -> StarWandererMythHandler.setBeamActive(player, true);
                case "beam_stop" -> StarWandererMythHandler.setBeamActive(player, false);
                case "wave" -> StarWandererMythHandler.castStarWave(player);
                default -> {
                }
            }
        });
    }

    private static void handleClickShulkerbornSlot(ClickShulkerbornSlotPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player contextPlayer = context.player();
            if (contextPlayer instanceof ServerPlayer player) {
                ShulkerbornInventoryHandler.handleSlotClick(player, payload.slot(), payload.secondary(), payload.quickMove());
            }
        });
    }
}
