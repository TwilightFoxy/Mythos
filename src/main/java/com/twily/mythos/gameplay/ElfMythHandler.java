package com.twily.mythos.gameplay;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.twily.mythos.Mythos;
import com.twily.mythos.data.MythDataManager;
import com.twily.mythos.myth.MythState;
import com.twily.mythos.network.MythosNetwork;
import com.twily.mythos.registry.MythosItems;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Optional;

import static net.minecraft.commands.Commands.literal;

@EventBusSubscriber(modid = Mythos.MOD_ID)
public final class ElfMythHandler {

    private ElfMythHandler() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
            literal("mythos")
                .then(literal("current")
                    .executes(context -> {
                        if (context.getSource().getPlayer() == null) {
                            return 0;
                        }

                        ServerPlayer player = context.getSource().getPlayerOrException();
                        context.getSource().sendSuccess(
                            () -> Component.translatable("command.mythos.current", MythState.displayName(MythState.get(player))),
                            false
                        );
                        return 1;
                    }))
                .then(literal("choose")
                    .executes(context -> {
                        if (context.getSource().getPlayer() == null) {
                            return 0;
                        }

                        ServerPlayer player = context.getSource().getPlayerOrException();
                        MythosNetwork.openSelection(player, true);
                        return 1;
                    }))
                .then(literal("guide")
                    .executes(context -> {
                        if (context.getSource().getPlayer() == null) {
                            return 0;
                        }

                        ServerPlayer player = context.getSource().getPlayerOrException();
                        MythosNetwork.openGuide(player);
                        return 1;
                    }))
                // Tail debug command is intentionally disabled in normal builds.
                // Keep the old item-grant branch nearby if we need fast in-game tail tuning again.
                .then(literal("set")
                    .then(Commands.argument("myth", StringArgumentType.word())
                        .executes(context -> {
                            if (context.getSource().getPlayer() == null) {
                                return 0;
                            }

                            ServerPlayer player = context.getSource().getPlayerOrException();
                            String rawMyth = StringArgumentType.getString(context, "myth");
                            Identifier mythId = parseMythId(rawMyth);
                            if (!MythDataManager.hasMyth(mythId)) {
                                context.getSource().sendFailure(Component.translatable("command.mythos.unknown_myth", rawMyth));
                                return 0;
                            }

                            MythState.set(player, mythId);
                            context.getSource().sendSuccess(() -> Component.translatable("command.mythos.set_myth", MythState.displayName(mythId)), false);
                            return 1;
                        })))
                .then(literal("elf")
                    .executes(context -> {
                        if (context.getSource().getPlayer() == null) {
                            return 0;
                        }

                        ServerPlayer player = context.getSource().getPlayerOrException();
                        Identifier elf = Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "elf");
                        MythState.set(player, elf);
                        context.getSource().sendSuccess(() -> Component.translatable("command.mythos.set_myth", MythState.displayName(elf)), false);
                        return 1;
                    }))
                .then(literal("dwarf")
                    .executes(context -> {
                        if (context.getSource().getPlayer() == null) {
                            return 0;
                        }

                        ServerPlayer player = context.getSource().getPlayerOrException();
                        Identifier dwarf = Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "dwarf");
                        MythState.set(player, dwarf);
                        context.getSource().sendSuccess(() -> Component.translatable("command.mythos.set_myth", MythState.displayName(dwarf)), false);
                        return 1;
                    }))
                .then(literal("human")
                    .executes(context -> {
                        if (context.getSource().getPlayer() == null) {
                            return 0;
                        }

                        ServerPlayer player = context.getSource().getPlayerOrException();
                        Identifier human = Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "human");
                        MythState.set(player, human);
                        context.getSource().sendSuccess(() -> Component.translatable("command.mythos.set_myth", MythState.displayName(human)), false);
                        return 1;
                    }))
                .then(literal("fairy")
                    .executes(context -> {
                        if (context.getSource().getPlayer() == null) {
                            return 0;
                        }

                        ServerPlayer player = context.getSource().getPlayerOrException();
                        Identifier fairy = Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "fairy");
                        MythState.set(player, fairy);
                        context.getSource().sendSuccess(() -> Component.translatable("command.mythos.set_myth", MythState.displayName(fairy)), false);
                        return 1;
                    }))
                .then(literal("kitsune")
                    .executes(context -> {
                        if (context.getSource().getPlayer() == null) {
                            return 0;
                        }

                        ServerPlayer player = context.getSource().getPlayerOrException();
                        Identifier kitsune = Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "kitsune");
                        MythState.set(player, kitsune);
                        context.getSource().sendSuccess(() -> Component.translatable("command.mythos.set_myth", MythState.displayName(kitsune)), false);
                        return 1;
                    }))
                .then(literal("siren")
                    .executes(context -> {
                        if (context.getSource().getPlayer() == null) {
                            return 0;
                        }

                        ServerPlayer player = context.getSource().getPlayerOrException();
                        Identifier siren = Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "siren");
                        MythState.set(player, siren);
                        context.getSource().sendSuccess(() -> Component.translatable("command.mythos.set_myth", MythState.displayName(siren)), false);
                        return 1;
                    }))
                .then(literal("clear")
                    .executes(context -> {
                        if (context.getSource().getPlayer() == null) {
                            return 0;
                        }

                        ServerPlayer player = context.getSource().getPlayerOrException();
                        MythState.clear(player);
                        context.getSource().sendSuccess(() -> Component.translatable("command.mythos.clear"), false);
                        return 1;
                    }))
        );
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && MythState.get(player).equals(MythState.NONE)) {
            player.sendSystemMessage(Component.translatable("message.mythos.choose_prompt"));
            MythosNetwork.openSelection(player, false);
            player.connection.teleport(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (MythState.get(player).equals(MythState.NONE)) {
            if (player.tickCount < 200 && player.tickCount % 20 == 5) {
                MythosNetwork.openSelection(player, false);
            } else if (player.tickCount % 40 == 5) {
                MythosNetwork.openSelection(player, false);
            }
            if (player.tickCount % 200 == 5) {
                player.sendSystemMessage(Component.translatable("message.mythos.choose_prompt"));
            }
            return;
        }

        int speedAmplifier = MythDataManager.biomeSpeedAmplifier(player, player.level().getBiome(player.blockPosition()));
        if (speedAmplifier >= 0 && player.tickCount % 20 == 0) {
            player.addEffect(new MobEffectInstance(MobEffects.SPEED, 40, speedAmplifier, true, false, true));
        }

        if (player.containerMenu instanceof SmithingMenu smithingMenu) {
            ItemStack result = smithingMenu.getSlot(SmithingMenu.RESULT_SLOT).getItem();
            Optional<Component> denyMessage = MythDataManager.deniedSmithingMessage(player, result);
            if (denyMessage.isPresent()) {
                smithingMenu.getSlot(SmithingMenu.RESULT_SLOT).set(ItemStack.EMPTY);
                smithingMenu.broadcastChanges();
                if (player.tickCount % 40 == 0) {
                    player.sendSystemMessage(denyMessage.get());
                }
            }
        }
    }

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Pre event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) {
            return;
        }

        float damage = event.getNewDamage();
        if (event.getSource().getDirectEntity() instanceof AbstractArrow) {
            event.setNewDamage(damage * (float)MythDataManager.rangedDamageMultiplier(player));
            return;
        }

        if (player.getMainHandItem().getItem() instanceof AxeItem) {
            event.setNewDamage(damage * (float)MythDataManager.itemDamageMultiplier(player, player.getMainHandItem()));
        }
    }

    private static Identifier parseMythId(String raw) {
        return raw.contains(":")
            ? Identifier.parse(raw)
            : Identifier.fromNamespaceAndPath(Mythos.MOD_ID, raw);
    }
}
