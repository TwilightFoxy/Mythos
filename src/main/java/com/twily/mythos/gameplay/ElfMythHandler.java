package com.twily.mythos.gameplay;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.twily.mythos.Mythos;
import com.twily.mythos.data.MythDataManager;
import com.twily.mythos.myth.MythState;
import com.twily.mythos.network.MythosNetwork;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
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

    /*
     * FROZEN CONTRACT
     * Scope:
     * - elf biome speed, elf combat tuning, and elven smithing access checks
     * Guarantees:
     * - forest speed stays data-driven
     * - ranged damage bonus and axe penalty remain readable and stable
     * - elven bow access remains gated through myth checks
     * Note:
     * - admin commands and mandatory myth selection flow share this class for now and are not part of this frozen contract.
     * Do not change the frozen gameplay pieces without explicit request.
     */
    private ElfMythHandler() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
            literal("mythos")
                .then(literal("current")
                    .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_ADMIN))
                    .then(Commands.argument("target", EntityArgument.player())
                        .executes(context -> {
                            ServerPlayer target = EntityArgument.getPlayer(context, "target");
                            context.getSource().sendSuccess(
                                () -> Component.translatable("command.mythos.current_target", target.getDisplayName(), MythState.displayName(MythState.get(target))),
                                false
                            );
                            return 1;
                        }))
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
                    .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_ADMIN))
                    .then(Commands.argument("target", EntityArgument.player())
                        .executes(context -> {
                            ServerPlayer target = EntityArgument.getPlayer(context, "target");
                            MythosNetwork.openSelection(target, false);
                            context.getSource().sendSuccess(
                                () -> Component.translatable("command.mythos.force_choose", target.getDisplayName()),
                                false
                            );
                            return 1;
                        }))
                    .executes(context -> {
                        if (context.getSource().getPlayer() == null) {
                            return 0;
                        }

                        ServerPlayer player = context.getSource().getPlayerOrException();
                        MythosNetwork.openSelection(player, true);
                        return 1;
                    }))
                .then(literal("guide")
                    .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_ADMIN))
                    .then(Commands.argument("target", EntityArgument.player())
                        .executes(context -> {
                            ServerPlayer target = EntityArgument.getPlayer(context, "target");
                            MythosNetwork.openGuide(target);
                            context.getSource().sendSuccess(
                                () -> Component.translatable("command.mythos.open_guide", target.getDisplayName()),
                                false
                            );
                            return 1;
                        }))
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
                    .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_ADMIN) || source.getEntity() == null)
                    .then(Commands.argument("targets", EntityArgument.players())
                        .then(Commands.argument("myth", StringArgumentType.word())
                            .suggests((context, builder) -> SharedSuggestionProvider.suggest(MythDataManager.mythCommandIds(), builder))
                        .executes(context -> {
                            var targets = EntityArgument.getPlayers(context, "targets");
                            String rawMyth = StringArgumentType.getString(context, "myth");
                            Identifier mythId = parseMythId(rawMyth);
                            if (!MythDataManager.hasMyth(mythId)) {
                                context.getSource().sendFailure(Component.translatable("command.mythos.unknown_myth", rawMyth));
                                return 0;
                            }

                            for (ServerPlayer target : targets) {
                                MythState.set(target, mythId);
                            }

                            context.getSource().sendSuccess(
                                () -> Component.translatable("command.mythos.set_target_myth", targets.size(), MythState.displayName(mythId)),
                                false
                            );
                            return targets.size();
                        }))))
                .then(literal("clear")
                    .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_ADMIN) || source.getEntity() == null)
                    .then(Commands.argument("targets", EntityArgument.players())
                        .executes(context -> {
                            var targets = EntityArgument.getPlayers(context, "targets");
                            for (ServerPlayer target : targets) {
                                MythState.clear(target);
                            }
                            context.getSource().sendSuccess(
                                () -> Component.translatable("command.mythos.clear_target", targets.size()),
                                false
                            );
                            return targets.size();
                        }))
                    .executes(context -> {
                        if (context.getSource().getPlayer() == null) {
                            return 0;
                        }

                        ServerPlayer player = context.getSource().getPlayerOrException();
                        MythState.clear(player);
                        context.getSource().sendSuccess(() -> Component.translatable("command.mythos.clear"), false);
                        return 1;
                    }))
                .then(literal("femboy_login_lightning")
                    .requires(source -> source.getEntity() instanceof ServerPlayer || source.permissions().hasPermission(Permissions.COMMANDS_ADMIN))
                    .executes(context -> {
                        if (context.getSource().getPlayer() == null) {
                            return 0;
                        }

                        ServerPlayer player = context.getSource().getPlayerOrException();
                        boolean enabled = FemboyKitsuneMythHandler.toggleLoginLightningEnabled(player);
                        context.getSource().sendSuccess(
                            () -> Component.translatable(
                                enabled ? "command.mythos.femboy_login_lightning.enabled" : "command.mythos.femboy_login_lightning.disabled"
                            ),
                            false
                        );
                        return 1;
                    })
                    .then(literal("on")
                        .executes(context -> {
                            if (context.getSource().getPlayer() == null) {
                                return 0;
                            }

                            ServerPlayer player = context.getSource().getPlayerOrException();
                            FemboyKitsuneMythHandler.setLoginLightningEnabled(player, true);
                            context.getSource().sendSuccess(
                                () -> Component.translatable("command.mythos.femboy_login_lightning.enabled"),
                                false
                            );
                            return 1;
                        }))
                    .then(literal("off")
                        .executes(context -> {
                            if (context.getSource().getPlayer() == null) {
                                return 0;
                            }

                            ServerPlayer player = context.getSource().getPlayerOrException();
                            FemboyKitsuneMythHandler.setLoginLightningEnabled(player, false);
                            context.getSource().sendSuccess(
                                () -> Component.translatable("command.mythos.femboy_login_lightning.disabled"),
                                false
                            );
                            return 1;
                        }))
                    .then(literal("set")
                        .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_ADMIN))
                        .then(Commands.argument("target", EntityArgument.player())
                            .then(literal("on")
                                .executes(context -> {
                                    ServerPlayer target = EntityArgument.getPlayer(context, "target");
                                    FemboyKitsuneMythHandler.setLoginLightningEnabled(target, true);
                                    context.getSource().sendSuccess(
                                        () -> Component.translatable("command.mythos.femboy_login_lightning.target_enabled", target.getDisplayName()),
                                        false
                                    );
                                    return 1;
                                }))
                            .then(literal("off")
                                .executes(context -> {
                                    ServerPlayer target = EntityArgument.getPlayer(context, "target");
                                    FemboyKitsuneMythHandler.setLoginLightningEnabled(target, false);
                                    context.getSource().sendSuccess(
                                        () -> Component.translatable("command.mythos.femboy_login_lightning.target_disabled", target.getDisplayName()),
                                        false
                                    );
                                    return 1;
                                })))))
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
