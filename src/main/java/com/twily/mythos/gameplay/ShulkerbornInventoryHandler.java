package com.twily.mythos.gameplay;

import com.twily.mythos.Mythos;
import com.twily.mythos.data.ShulkerbornInventoryData;
import com.twily.mythos.myth.MythState;
import com.twily.mythos.registry.MythosAttachments;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.resources.Identifier;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.item.ItemPredicateArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.TriState;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.gamerules.GameRules;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.CommandEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.function.Predicate;

@EventBusSubscriber(modid = Mythos.MOD_ID)
public final class ShulkerbornInventoryHandler {

    public static final Identifier SHULKERBORN = Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "shulkerborn");

    private ShulkerbornInventoryHandler() {
    }

    public static boolean isShulkerborn(Player player) {
        return MythState.matches(player, SHULKERBORN);
    }

    public static ShulkerbornInventoryData getData(Player player) {
        return player.getData(MythosAttachments.SHULKERBORN_EXTRA_SLOTS);
    }

    public static void handleSlotClick(ServerPlayer player, int slotIndex, boolean secondary, boolean quickMove) {
        if (slotIndex < 0 || slotIndex >= ShulkerbornInventoryData.SLOT_COUNT || !isShulkerborn(player)) {
            return;
        }

        ShulkerbornInventoryData data = getData(player);
        ItemStack slotStack = data.get(slotIndex);
        ItemStack carried = player.containerMenu.getCarried();

        if (quickMove) {
            if (slotStack.isEmpty()) {
                return;
            }

            ItemStack moving = slotStack.copy();
            player.getInventory().add(moving);
            if (moving.getCount() == slotStack.getCount()) {
                return;
            }

            player.setData(
                MythosAttachments.SHULKERBORN_EXTRA_SLOTS,
                data.withSlot(slotIndex, moving.isEmpty() ? ItemStack.EMPTY : moving)
            );
            player.containerMenu.broadcastChanges();
            player.inventoryMenu.broadcastChanges();
            player.getInventory().setChanged();
            ShulkerbornMythHandler.syncOverloadNow(player);
            return;
        }

        ShulkerbornInventoryData nextData = data;
        ItemStack nextCarried = carried;

        if (secondary) {
            if (carried.isEmpty()) {
                if (!slotStack.isEmpty()) {
                    int takenCount = (slotStack.getCount() + 1) / 2;
                    nextCarried = slotStack.copyWithCount(takenCount);
                    ItemStack remaining = slotStack.copy();
                    remaining.shrink(takenCount);
                    nextData = data.withSlot(slotIndex, remaining);
                }
            } else if (slotStack.isEmpty()) {
                nextData = data.withSlot(slotIndex, carried.copyWithCount(1));
                nextCarried = carried.copy();
                nextCarried.shrink(1);
            } else if (ItemStack.isSameItemSameComponents(slotStack, carried) && slotStack.getCount() < slotStack.getMaxStackSize()) {
                ItemStack grown = slotStack.copy();
                grown.grow(1);
                nextData = data.withSlot(slotIndex, grown);
                nextCarried = carried.copy();
                nextCarried.shrink(1);
            }
        } else {
            if (carried.isEmpty()) {
                if (!slotStack.isEmpty()) {
                    nextCarried = slotStack.copy();
                    nextData = data.withSlot(slotIndex, ItemStack.EMPTY);
                }
            } else if (slotStack.isEmpty()) {
                nextData = data.withSlot(slotIndex, carried);
                nextCarried = ItemStack.EMPTY;
            } else if (ItemStack.isSameItemSameComponents(slotStack, carried) && slotStack.getCount() < slotStack.getMaxStackSize()) {
                int transfer = Math.min(carried.getCount(), slotStack.getMaxStackSize() - slotStack.getCount());
                if (transfer > 0) {
                    ItemStack grown = slotStack.copy();
                    grown.grow(transfer);
                    nextData = data.withSlot(slotIndex, grown);
                    nextCarried = carried.copy();
                    nextCarried.shrink(transfer);
                }
            } else {
                nextData = data.withSlot(slotIndex, carried);
                nextCarried = slotStack.copy();
            }
        }

        if (nextCarried.isEmpty()) {
            nextCarried = ItemStack.EMPTY;
        }

        if (nextData == data && ItemStack.matches(nextCarried, carried)) {
            return;
        }

        player.setData(MythosAttachments.SHULKERBORN_EXTRA_SLOTS, nextData);
        player.containerMenu.setCarried(nextCarried);
        player.containerMenu.broadcastChanges();
        player.inventoryMenu.broadcastChanges();
        player.getInventory().setChanged();
        ShulkerbornMythHandler.syncOverloadNow(player);
    }

    @SubscribeEvent
    public static void onCommand(CommandEvent event) {
        if (event.getParseResults().getContext().getNodes().isEmpty()
            || !"clear".equals(event.getParseResults().getContext().getNodes().getFirst().getNode().getName())) {
            return;
        }

        try {
            CommandContext<CommandSourceStack> context =
                event.getParseResults().getContext().build(event.getParseResults().getReader().getString());
            Map<String, com.mojang.brigadier.context.ParsedArgument<CommandSourceStack, ?>> arguments =
                event.getParseResults().getContext().getArguments();

            Collection<ServerPlayer> targets = arguments.containsKey("targets")
                ? EntityArgument.getPlayers(context, "targets")
                : Collections.singleton(context.getSource().getPlayerOrException());
            Predicate<ItemStack> predicate = arguments.containsKey("item")
                ? ItemPredicateArgument.getItemPredicate(context, "item")
                : stack -> true;
            int maxCount = arguments.containsKey("maxCount")
                ? IntegerArgumentType.getInteger(context, "maxCount")
                : -1;

            int totalRemoved = 0;
            for (ServerPlayer target : targets) {
                int removedVanilla = target.getInventory().clearOrCountMatchingItems(predicate, maxCount, target.inventoryMenu.getCraftSlots());
                int remainingLimit = maxCount < 0 ? -1 : Math.max(0, maxCount - removedVanilla);
                int removedExtra = clearOrCountMatchingExtraSlots(target, predicate, remainingLimit);
                int removed = removedVanilla + removedExtra;
                totalRemoved += removed;

                if (removed > 0 && maxCount != 0) {
                    target.containerMenu.broadcastChanges();
                    target.inventoryMenu.slotsChanged(target.getInventory());
                    target.getInventory().setChanged();
                }
            }

            if (totalRemoved == 0) {
                if (targets.size() == 1) {
                    ServerPlayer target = targets.iterator().next();
                    context.getSource().sendFailure(Component.translatable("clear.failed.single", target.getName()));
                } else {
                    context.getSource().sendFailure(Component.translatable("clear.failed.multiple", targets.size()));
                }
            } else if (maxCount == 0) {
                final int clearedCount = totalRemoved;
                if (targets.size() == 1) {
                    ServerPlayer target = targets.iterator().next();
                    context.getSource().sendSuccess(
                        () -> Component.translatable("commands.clear.test.single", clearedCount, target.getDisplayName()),
                        true
                    );
                } else {
                    context.getSource().sendSuccess(
                        () -> Component.translatable("commands.clear.test.multiple", clearedCount, targets.size()),
                        true
                    );
                }
            } else {
                final int clearedCount = totalRemoved;
                if (targets.size() == 1) {
                    ServerPlayer target = targets.iterator().next();
                    context.getSource().sendSuccess(
                        () -> Component.translatable("commands.clear.success.single", clearedCount, target.getDisplayName()),
                        true
                    );
                } else {
                    context.getSource().sendSuccess(
                        () -> Component.translatable("commands.clear.success.multiple", clearedCount, targets.size()),
                        true
                    );
                }
            }

            event.setCanceled(true);
        } catch (Exception ignored) {
            // Fall back to vanilla execution if parsing or source resolution fails.
        }
    }

    public static int occupiedSlotCount(Player player) {
        int occupied = 0;
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (!stack.isEmpty()) {
                occupied++;
            }
        }
        for (ItemStack stack : getData(player).slots()) {
            if (!stack.isEmpty()) {
                occupied++;
            }
        }
        return occupied;
    }

    public static boolean hasVanillaCapacity(Player player, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        int remaining = stack.getCount();
        for (ItemStack inventoryStack : player.getInventory().getNonEquipmentItems()) {
            if (inventoryStack.isEmpty()) {
                return true;
            }
            if (!ItemStack.isSameItemSameComponents(inventoryStack, stack)) {
                continue;
            }

            remaining -= Math.max(0, inventoryStack.getMaxStackSize() - inventoryStack.getCount());
            if (remaining <= 0) {
                return true;
            }
        }

        return false;
    }

    public static int insertIntoExtraSlots(Player player, ItemStack source) {
        if (source.isEmpty()) {
            return 0;
        }

        ShulkerbornInventoryData data = getData(player);
        ShulkerbornInventoryData updated = data;
        ItemStack remaining = source.copy();

        for (int slot = 0; slot < ShulkerbornInventoryData.SLOT_COUNT && !remaining.isEmpty(); slot++) {
            ItemStack slotStack = updated.get(slot);
            if (slotStack.isEmpty() || !ItemStack.isSameItemSameComponents(slotStack, remaining) || slotStack.getCount() >= slotStack.getMaxStackSize()) {
                continue;
            }

            int transfer = Math.min(remaining.getCount(), slotStack.getMaxStackSize() - slotStack.getCount());
            if (transfer <= 0) {
                continue;
            }

            ItemStack grown = slotStack.copy();
            grown.grow(transfer);
            updated = updated.withSlot(slot, grown);
            remaining.shrink(transfer);
        }

        for (int slot = 0; slot < ShulkerbornInventoryData.SLOT_COUNT && !remaining.isEmpty(); slot++) {
            ItemStack slotStack = updated.get(slot);
            if (!slotStack.isEmpty()) {
                continue;
            }

            int transfer = Math.min(remaining.getCount(), remaining.getMaxStackSize());
            updated = updated.withSlot(slot, remaining.copyWithCount(transfer));
            remaining.shrink(transfer);
        }

        int inserted = source.getCount() - remaining.getCount();
        if (inserted <= 0) {
            return 0;
        }

        player.setData(MythosAttachments.SHULKERBORN_EXTRA_SLOTS, updated);
        player.containerMenu.broadcastChanges();
        player.inventoryMenu.broadcastChanges();
        player.getInventory().setChanged();
        ShulkerbornMythHandler.syncOverloadNow(player);
        return inserted;
    }

    public static int clearOrCountMatchingExtraSlots(ServerPlayer player, Predicate<ItemStack> predicate, int maxCount) {
        ShulkerbornInventoryData data = getData(player);
        ShulkerbornInventoryData updated = data;
        int affected = 0;
        int remaining = maxCount;

        for (int slot = 0; slot < ShulkerbornInventoryData.SLOT_COUNT; slot++) {
            ItemStack stack = updated.get(slot);
            if (stack.isEmpty() || !predicate.test(stack)) {
                continue;
            }

            int removable = stack.getCount();
            if (remaining >= 0) {
                removable = Math.min(removable, remaining);
            }
            if (removable <= 0) {
                continue;
            }

            affected += removable;
            if (maxCount != 0) {
                if (removable == stack.getCount()) {
                    updated = updated.withSlot(slot, ItemStack.EMPTY);
                } else {
                    ItemStack reduced = stack.copy();
                    reduced.shrink(removable);
                    updated = updated.withSlot(slot, reduced);
                }
            }

            if (remaining >= 0) {
                remaining -= removable;
                if (remaining <= 0) {
                    break;
                }
            }
        }

        if (maxCount != 0 && affected > 0) {
            player.setData(MythosAttachments.SHULKERBORN_EXTRA_SLOTS, updated);
            ShulkerbornMythHandler.syncOverloadNow(player);
        }

        return affected;
    }

    @SubscribeEvent
    public static void onItemPickupPre(ItemEntityPickupEvent.Pre event) {
        Player player = event.getPlayer();
        if (!isShulkerborn(player)) {
            return;
        }

        ItemStack groundStack = event.getItemEntity().getItem();
        if (groundStack.isEmpty() || hasVanillaCapacity(player, groundStack)) {
            return;
        }

        int inserted = insertIntoExtraSlots(player, groundStack);
        if (inserted <= 0) {
            return;
        }

        groundStack.shrink(inserted);
        player.take(event.getItemEntity(), inserted);
        if (groundStack.isEmpty()) {
            event.getItemEntity().discard();
        }
        event.setCanPickup(TriState.FALSE);
    }

    @SubscribeEvent
    public static void onItemPickupPost(ItemEntityPickupEvent.Post event) {
        Player player = event.getPlayer();
        if (!isShulkerborn(player)) {
            return;
        }

        ItemStack remaining = event.getCurrentStack();
        if (remaining.isEmpty()) {
            return;
        }

        int inserted = insertIntoExtraSlots(player, remaining);
        if (inserted <= 0) {
            return;
        }

        remaining.shrink(inserted);
        player.take(event.getItemEntity(), inserted);
        if (remaining.isEmpty()) {
            event.getItemEntity().discard();
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        Player original = event.getOriginal();
        Player clone = event.getEntity();

        if (!event.isWasDeath() || ((ServerLevel) clone.level()).getGameRules().get(GameRules.KEEP_INVENTORY)) {
            clone.setData(MythosAttachments.SHULKERBORN_EXTRA_SLOTS, original.getData(MythosAttachments.SHULKERBORN_EXTRA_SLOTS));
        }
    }

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide()) {
            return;
        }

        if (((ServerLevel) player.level()).getGameRules().get(GameRules.KEEP_INVENTORY)) {
            return;
        }

        ShulkerbornInventoryData data = getData(player);
        if (data.isEmpty()) {
            return;
        }

        for (ItemStack stack : data.slots()) {
            if (stack.isEmpty()) {
                continue;
            }

            event.getDrops().add(new ItemEntity(player.level(), player.getX(), player.getY() + 0.5D, player.getZ(), stack.copy()));
        }

        player.setData(MythosAttachments.SHULKERBORN_EXTRA_SLOTS, ShulkerbornInventoryData.empty());
    }
}
