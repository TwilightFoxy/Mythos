package com.twily.mythos.gameplay;

import com.twily.mythos.Mythos;
import com.twily.mythos.data.MythItemMarkerHelper;
import com.twily.mythos.myth.MythState;
import com.twily.mythos.registry.MythosAttachments;
import com.twily.mythos.registry.MythosEffects;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
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
    private static final Identifier LEGACY_DWARF_SCALE = Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "dwarf_scale");
    private static final String DWARVEN_PICKAXE_MARKER = "mythos_dwarven_pickaxe";
    private static final double NORMAL_SCALE = 1.0D;
    private static final double DWARF_SCALE = 0.75D;
    private static final double FAIRY_SCALE = 0.5D;
    private static final int BLINDNESS_THRESHOLD_TICKS = 20 * 60 * 30;

    private DwarfMythHandler() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        var player = event.getEntity();
        if (player.level().isClientSide()) {
            return;
        }

        syncScale(player.getAttribute(Attributes.SCALE), targetScale(player));

        if (MythState.is(player, DWARF)) {
            if (player.hasEffect(MythosEffects.DWARVEN_ALE)) {
                resetAlePenalty(player);
            } else {
                int soberTicks = player.getData(MythosAttachments.DWARF_SOBER_TICKS) + 1;
                player.setData(MythosAttachments.DWARF_SOBER_TICKS, soberTicks);

                if (player.tickCount % 40 == 0) {
                    player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 0, false, true, true));
                    if (soberTicks >= BLINDNESS_THRESHOLD_TICKS) {
                        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0, false, true, true));
                    }
                }
            }
        } else {
            resetAlePenalty(player);
        }

        if (player.containerMenu instanceof SmithingMenu smithingMenu) {
            handleSmithing(player, smithingMenu, MythState.is(player, DWARF));
        }
    }

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (!MythState.is(event.getEntity(), DWARF)) {
            return;
        }

        float multiplier = event.getPosition()
            .map(pos -> pos.getY() < 0 ? 1.4F : 1.2F)
            .orElse(1.2F);
        event.setNewSpeed(event.getNewSpeed() * multiplier);
    }

    private static void syncScale(AttributeInstance scale, double targetScale) {
        if (scale == null) {
            return;
        }

        scale.removeModifier(LEGACY_DWARF_SCALE);

        if (scale.getBaseValue() != targetScale) {
            scale.setBaseValue(targetScale);
        }
    }

    private static double targetScale(net.minecraft.world.entity.player.Player player) {
        if (MythState.is(player, DWARF)) {
            return DWARF_SCALE;
        }

        if (MythState.is(player, FAIRY)) {
            return FAIRY_SCALE;
        }

        return NORMAL_SCALE;
    }

    private static void resetAlePenalty(net.minecraft.world.entity.player.Player player) {
        player.setData(MythosAttachments.DWARF_SOBER_TICKS, 0);
        player.removeEffect(MobEffects.BLINDNESS);
        if (player.hasEffect(MythosEffects.DWARVEN_ALE)) {
            player.removeEffect(MobEffects.SLOWNESS);
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
}
