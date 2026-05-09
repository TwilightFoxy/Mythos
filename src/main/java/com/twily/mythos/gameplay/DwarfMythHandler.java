package com.twily.mythos.gameplay;

import com.twily.mythos.Mythos;
import com.twily.mythos.data.MythItemMarkerHelper;
import com.twily.mythos.myth.MythState;
import com.twily.mythos.registry.MythosAttachments;
import com.twily.mythos.registry.MythosEffects;
import com.twily.mythos.registry.MythosItems;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.List;
import java.util.Optional;

@EventBusSubscriber(modid = Mythos.MOD_ID)
public final class DwarfMythHandler {

    private static final Identifier DWARF = Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "dwarf");
    private static final Identifier FAIRY = Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "fairy");
    private static final Identifier DWARF_ALE_SLOWNESS = Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "dwarf_ale_slowness");
    private static final String DWARVEN_PICKAXE_MARKER = "mythos_dwarven_pickaxe";
    private static final int BLINDNESS_THRESHOLD_TICKS = 20 * 60 * 5;
    private static final int DWARF_PERSISTENT_STATUS_TICKS = 20 * 60 * 60;
    private static final double DWARF_ALE_SLOWNESS_AMOUNT = -0.35D;

    private DwarfMythHandler() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        var player = event.getEntity();
        if (player.level().isClientSide()) {
            return;
        }

        if (MythState.matches(player, DWARF)) {
            int hasteAmplifier = player.getBlockY() < 0 ? 1 : 0;
            player.addEffect(new MobEffectInstance(MobEffects.HASTE, 40, hasteAmplifier, false, true, true));

            if (player.hasEffect(MythosEffects.DWARVEN_ALE)) {
                clearAlePenaltyState(player);
            } else {
                int soberTicks = player.getData(MythosAttachments.DWARF_SOBER_TICKS) + 1;
                player.setData(MythosAttachments.DWARF_SOBER_TICKS, soberTicks);

                syncAleSlowness(player, true);
                ensureDisplayEffect(player, MythosEffects.DWARF_ALE_SLOWED, DWARF_PERSISTENT_STATUS_TICKS);

                int remainingUntilBlindness = Math.max(0, BLINDNESS_THRESHOLD_TICKS - soberTicks);
                if (remainingUntilBlindness > 0) {
                    ensureDisplayEffect(player, MythosEffects.DWARF_ALE_WITHDRAWAL, remainingUntilBlindness);
                    player.removeEffect(MythosEffects.DWARF_ACUTE_ALE_WITHDRAWAL);
                } else {
                    player.removeEffect(MythosEffects.DWARF_ALE_WITHDRAWAL);
                    ensureDisplayEffect(player, MythosEffects.DWARF_ACUTE_ALE_WITHDRAWAL, DWARF_PERSISTENT_STATUS_TICKS);
                    ensureDisplayEffect(player, MobEffects.BLINDNESS, DWARF_PERSISTENT_STATUS_TICKS);
                }
            }
        } else {
            clearAlePenaltyState(player);
        }

        if (player.containerMenu instanceof SmithingMenu smithingMenu) {
            handleSmithing(player, smithingMenu, MythState.matches(player, DWARF));
        }
    }

    public static void clearAlePenaltyState(net.minecraft.world.entity.player.Player player) {
        player.setData(MythosAttachments.DWARF_SOBER_TICKS, 0);
        player.removeEffect(MythosEffects.DWARF_ALE_SLOWED);
        player.removeEffect(MythosEffects.DWARF_ALE_WITHDRAWAL);
        player.removeEffect(MythosEffects.DWARF_ACUTE_ALE_WITHDRAWAL);
        player.removeEffect(MobEffects.BLINDNESS);
        syncAleSlowness(player, false);
    }

    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (!event.getCrafting().is(MythosItems.DWARVEN_ALE.get())) {
            return;
        }

        if (removeSingleItem(event.getInventory(), Items.GLASS_BOTTLE)) {
            return;
        }

        removeSingleItem(event.getEntity().getInventory(), Items.GLASS_BOTTLE);
    }

    private static void syncAleSlowness(net.minecraft.world.entity.player.Player player, boolean active) {
        MythStatusHelper.syncModifier(player, Attributes.MOVEMENT_SPEED, DWARF_ALE_SLOWNESS, DWARF_ALE_SLOWNESS_AMOUNT, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, active);
    }

    private static void ensureDisplayEffect(net.minecraft.world.entity.player.Player player, Holder<MobEffect> effect, int duration) {
        MobEffectInstance current = player.getEffect(effect);
        if (current == null) {
            player.addEffect(new MobEffectInstance(effect, duration, 0, false, true, true));
        }
    }

    private static void handleSmithing(net.minecraft.world.entity.player.Player player, SmithingMenu smithingMenu, boolean isDwarf) {
        ItemStack template = smithingMenu.getSlot(SmithingMenu.TEMPLATE_SLOT).getItem();
        ItemStack base = smithingMenu.getSlot(SmithingMenu.BASE_SLOT).getItem();
        ItemStack addition = smithingMenu.getSlot(SmithingMenu.ADDITIONAL_SLOT).getItem();
        if (!template.isEmpty() || !isDwarvenTemperingInputs(base, addition)) {
            return;
        }

        Optional<ItemStack> temperedPickaxe = createTemperedPickaxe(base);
        if (!isDwarf) {
            clearSmithingResult(smithingMenu);
            if (temperedPickaxe.isPresent()) {
                if (player.tickCount % 40 == 0) {
                    player.sendSystemMessage(Component.translatable("message.mythos.dwarf_only_tempering"));
                }
            }
            return;
        }

        if (temperedPickaxe.isPresent()) {
            ItemStack result = smithingMenu.getSlot(SmithingMenu.RESULT_SLOT).getItem();
            if (!ItemStack.matches(result, temperedPickaxe.get())) {
                smithingMenu.getSlot(SmithingMenu.RESULT_SLOT).set(temperedPickaxe.get());
                smithingMenu.broadcastChanges();
            }
        } else {
            clearSmithingResult(smithingMenu);
        }
    }

    private static boolean isDwarvenTemperingInputs(ItemStack base, ItemStack addition) {
        return !base.isEmpty() && base.is(ItemTags.PICKAXES) && addition.is(Items.NETHERITE_INGOT);
    }

    private static Optional<ItemStack> createTemperedPickaxe(ItemStack base) {
        if (!base.isEnchanted() || MythItemMarkerHelper.hasMarker(base, DWARVEN_PICKAXE_MARKER)) {
            return Optional.empty();
        }

        ItemStack result = base.copyWithCount(1);
        ItemEnchantments.Mutable enchantments = new ItemEnchantments.Mutable(result.getEnchantments());
        boolean changed = false;

        for (Holder<Enchantment> enchantment : List.copyOf(enchantments.keySet())) {
            int currentLevel = enchantments.getLevel(enchantment);
            if (!canTemper(enchantment, currentLevel)) {
                continue;
            }

            int upgradedLevel = Math.min(currentLevel + 1, enchantment.value().getMaxLevel() + 1);
            if (upgradedLevel > currentLevel) {
                enchantments.set(enchantment, upgradedLevel);
                changed = true;
            }
        }

        if (!changed) {
            return Optional.empty();
        }

        result.set(DataComponents.ENCHANTMENTS, enchantments.toImmutable());
        result.set(DataComponents.CUSTOM_NAME, Component.translatable("item.mythos.dwarven_pickaxe").withStyle(ChatFormatting.BLUE));
        MythItemMarkerHelper.setMarker(result, DWARVEN_PICKAXE_MARKER);
        return Optional.of(result);
    }

    private static boolean canTemper(Holder<Enchantment> enchantment, int currentLevel) {
        if (currentLevel <= 0 || enchantment.is(EnchantmentTags.CURSE)) {
            return false;
        }

        if (enchantment.is(Enchantments.SILK_TOUCH) || enchantment.is(Enchantments.MENDING)) {
            return false;
        }

        int maxLevel = enchantment.value().getMaxLevel();
        return maxLevel > 1 && currentLevel < maxLevel + 1;
    }

    private static void clearSmithingResult(SmithingMenu smithingMenu) {
        if (!smithingMenu.getSlot(SmithingMenu.RESULT_SLOT).getItem().isEmpty()) {
            smithingMenu.getSlot(SmithingMenu.RESULT_SLOT).set(ItemStack.EMPTY);
            smithingMenu.broadcastChanges();
        }
    }

    private static boolean removeSingleItem(net.minecraft.world.Container container, net.minecraft.world.item.Item item) {
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (stack.is(item)) {
                container.removeItem(slot, 1);
                return true;
            }
        }
        return false;
    }
}
