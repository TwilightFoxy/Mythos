package com.twily.mythos.gameplay;

import com.twily.mythos.Mythos;
import com.twily.mythos.myth.MythState;
import com.twily.mythos.registry.MythosAttachments;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = Mythos.MOD_ID)
public final class ArchFairyMythHandler {

    private static final Identifier ARCH_FAIRY = Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "arch_fairy");
    private static final Identifier ARCH_FAIRY_SMALL_SCALE = Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "arch_fairy_small_scale");
    private static final double ARCH_FAIRY_SMALL_SCALE_AMOUNT = -0.2222222222D;
    private static final int SMALL_SPEED_DURATION_TICKS = 40;

    private ArchFairyMythHandler() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) {
            return;
        }

        boolean isArchFairy = MythState.is(player, ARCH_FAIRY);
        boolean small = isArchFairy && player.getData(MythosAttachments.ARCH_FAIRY_SMALL);

        MythStatusHelper.syncModifier(
            player,
            Attributes.SCALE,
            ARCH_FAIRY_SMALL_SCALE,
            ARCH_FAIRY_SMALL_SCALE_AMOUNT,
            AttributeModifier.Operation.ADD_VALUE,
            small
        );

        if (!isArchFairy) {
            if (player.getData(MythosAttachments.ARCH_FAIRY_SMALL)) {
                player.setData(MythosAttachments.ARCH_FAIRY_SMALL, false);
            }
            return;
        }

        if (small) {
            player.addEffect(new MobEffectInstance(MobEffects.SPEED, SMALL_SPEED_DURATION_TICKS, 0, false, false, true));
        }
    }

    public static void toggleSize(ServerPlayer player) {
        if (!MythState.is(player, ARCH_FAIRY)) {
            return;
        }

        boolean nowSmall = !player.getData(MythosAttachments.ARCH_FAIRY_SMALL);
        player.setData(MythosAttachments.ARCH_FAIRY_SMALL, nowSmall);
        player.sendSystemMessage(Component.translatable(nowSmall
            ? "message.mythos.arch_fairy_size_small"
            : "message.mythos.arch_fairy_size_normal"));
    }
}
