package com.twily.mythos.gameplay;

import com.twily.mythos.Mythos;
import com.twily.mythos.myth.MythState;
import com.twily.mythos.registry.MythosEffects;
import com.twily.mythos.registry.MythosItems;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.inventory.AbstractCraftingMenu;
import net.minecraft.world.inventory.Slot;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = Mythos.MOD_ID)
public final class ShulkerbornMythHandler {

    private static final Identifier SHULKERBORN = Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "shulkerborn");
    private static final Identifier SHULKERBORN_OVERLOAD_SPEED = Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "shulkerborn_overload_speed");
    private static final int OVERLOAD_CHECK_INTERVAL_TICKS = 20 * 5;
    private static final int OVERLOAD_EFFECT_DURATION_TICKS = 20 * 20;
    private static final int OVERLOAD_EFFECT_REFRESH_THRESHOLD_TICKS = 20 * 5;
    private static final double OVERLOAD_SPEED_AMOUNT = -0.15D;
    private static final int OVERLOADED_OCCUPIED_SLOT_THRESHOLD = 37;
    private static final TagKey<Item> REINFORCED_SHULKER_BOXES = TagKey.create(net.minecraft.core.registries.Registries.ITEM, Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "reinforced_shulker_boxes"));

    private ShulkerbornMythHandler() {
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof net.minecraft.world.entity.player.Player player)) {
            return;
        }

        syncOverloadNow(player);
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        net.minecraft.world.entity.player.Player player = event.getEntity();
        if (player.level().isClientSide()) {
            return;
        }

        boolean isShulkerborn = MythState.matches(player, SHULKERBORN);
        if (!isShulkerborn) {
            clearOverload(player);
        } else if (player.tickCount % OVERLOAD_CHECK_INTERVAL_TICKS == 0) {
            syncOverloadNow(player);
        }

        syncShulkerCrafting(player, isShulkerborn);
    }

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        if (event.getEntity().level().isClientSide() || event.getEntity().getType() != EntityType.WARDEN) {
            return;
        }

        ItemStack stack = new ItemStack(MythosItems.RESONANCE_SHARD.asItem());
        ItemEntity drop = new ItemEntity(
            event.getEntity().level(),
            event.getEntity().getX(),
            event.getEntity().getY() + 0.5D,
            event.getEntity().getZ(),
            stack
        );
        event.getDrops().add(drop);
    }

    public static void syncOverloadNow(net.minecraft.world.entity.player.Player player) {
        if (!MythState.matches(player, SHULKERBORN)) {
            clearOverload(player);
            return;
        }

        boolean overloaded = ShulkerbornInventoryHandler.occupiedSlotCount(player) >= OVERLOADED_OCCUPIED_SLOT_THRESHOLD;
        MythStatusHelper.syncModifier(
            player,
            Attributes.MOVEMENT_SPEED,
            SHULKERBORN_OVERLOAD_SPEED,
            OVERLOAD_SPEED_AMOUNT,
            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
            overloaded
        );

        if (!overloaded) {
            player.removeEffect(MythosEffects.SHULKERBORN_OVERLOADED);
            return;
        }

        ensureDisplayEffect(player, MythosEffects.SHULKERBORN_OVERLOADED, OVERLOAD_EFFECT_DURATION_TICKS);
    }

    private static void clearOverload(net.minecraft.world.entity.player.Player player) {
        MythStatusHelper.syncModifier(
            player,
            Attributes.MOVEMENT_SPEED,
            SHULKERBORN_OVERLOAD_SPEED,
            OVERLOAD_SPEED_AMOUNT,
            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
            false
        );
        player.removeEffect(MythosEffects.SHULKERBORN_OVERLOADED);
    }

    private static void ensureDisplayEffect(net.minecraft.world.entity.player.Player player, Holder<net.minecraft.world.effect.MobEffect> effect, int duration) {
        MobEffectInstance current = player.getEffect(effect);
        if (current == null || current.getDuration() <= OVERLOAD_EFFECT_REFRESH_THRESHOLD_TICKS) {
            player.addEffect(new MobEffectInstance(effect, duration, 0, false, true, true));
        }
    }

    private static void syncShulkerCrafting(net.minecraft.world.entity.player.Player player, boolean isShulkerborn) {
        if (!(player.containerMenu instanceof AbstractCraftingMenu craftingMenu)) {
            return;
        }

        Slot resultSlot = craftingMenu.getResultSlot();
        ItemStack result = resultSlot.getItem();
        if (result.isEmpty() || !isShulkerbornRecipeResult(result)) {
            return;
        }

        if (!canUseShulkerRecipeResult(craftingMenu, result, isShulkerborn)) {
            if (!result.isEmpty()) {
                resultSlot.set(ItemStack.EMPTY);
                craftingMenu.broadcastChanges();
                if (player.tickCount % 40 == 0) {
                    player.sendSystemMessage(Component.translatable("message.mythos.shulkerborn_only_artifacts"));
                }
            }
            return;
        }

        ItemStack source = findReinforcedShulkerInput(craftingMenu);
        if (source.isEmpty() || !result.is(REINFORCED_SHULKER_BOXES)) {
            return;
        }

        ItemStack enrichedResult = result.copy();
        copyReinforcedShulkerComponents(source, enrichedResult);
        if (!ItemStack.matches(result, enrichedResult)) {
            resultSlot.set(enrichedResult);
            craftingMenu.broadcastChanges();
        }
    }

    private static boolean canUseShulkerRecipeResult(AbstractCraftingMenu craftingMenu, ItemStack result, boolean isShulkerborn) {
        if (result.is(REINFORCED_SHULKER_BOXES) && isReinforcedRecolorRecipe(craftingMenu)) {
            return true;
        }
        return isShulkerborn;
    }

    private static boolean isShulkerbornRecipeResult(ItemStack stack) {
        return stack.is(net.minecraft.world.item.Items.SHULKER_BOX) || stack.is(REINFORCED_SHULKER_BOXES);
    }

    private static ItemStack findReinforcedShulkerInput(AbstractCraftingMenu craftingMenu) {
        for (Slot slot : craftingMenu.getInputGridSlots()) {
            ItemStack input = slot.getItem();
            if (input.is(REINFORCED_SHULKER_BOXES)) {
                return input;
            }
        }
        return ItemStack.EMPTY;
    }

    private static boolean isReinforcedRecolorRecipe(AbstractCraftingMenu craftingMenu) {
        boolean foundReinforcedShulker = false;
        boolean foundDye = false;
        for (Slot slot : craftingMenu.getInputGridSlots()) {
            ItemStack input = slot.getItem();
            if (input.isEmpty()) {
                continue;
            }
            if (input.is(REINFORCED_SHULKER_BOXES)) {
                if (foundReinforcedShulker) {
                    return false;
                }
                foundReinforcedShulker = true;
                continue;
            }
            if (input.is(ItemTags.DYES)) {
                foundDye = true;
                continue;
            }
            return false;
        }
        return foundReinforcedShulker && foundDye;
    }

    private static void copyReinforcedShulkerComponents(ItemStack source, ItemStack target) {
        copyComponent(source, target, DataComponents.CUSTOM_NAME);
        copyComponent(source, target, DataComponents.CONTAINER);
        copyComponent(source, target, DataComponents.LOCK);
        copyComponent(source, target, DataComponents.CONTAINER_LOOT);
    }

    private static <T> void copyComponent(ItemStack source, ItemStack target, net.minecraft.core.component.DataComponentType<T> componentType) {
        if (source.has(componentType)) {
            target.set(componentType, source.get(componentType));
        } else {
            target.remove(componentType);
        }
    }
}
