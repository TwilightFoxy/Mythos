package com.twily.mythos.gameplay;

import com.twily.mythos.Mythos;
import com.twily.mythos.myth.MythState;
import com.twily.mythos.registry.MythosAttachments;
import com.twily.mythos.registry.MythosItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Optional;

@EventBusSubscriber(modid = Mythos.MOD_ID)
public final class SirenMythHandler {

    private static final Identifier SIREN = Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "siren");
    private static final int SIREN_MAX_DRY_AIR = 300;
    private static final int WATER_BUFF_DURATION_TICKS = 40;
    private static final int LAND_DEBUFF_DURATION_TICKS = 40;

    private SirenMythHandler() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide()) {
            return;
        }

        boolean isSiren = MythState.is(player, SIREN);
        if (player.containerMenu instanceof SmithingMenu smithingMenu) {
            handleSirenSmithing(player, smithingMenu, isSiren);
        }

        if (!isSiren) {
            if (player.getData(MythosAttachments.SIREN_DRY_TICKS) != -1) {
                player.setData(MythosAttachments.SIREN_DRY_TICKS, -1);
            }
            if (player.getAirSupply() != player.getMaxAirSupply()) {
                player.setAirSupply(player.getMaxAirSupply());
            }
            player.removeEffect(MobEffects.SLOWNESS);
            player.removeEffect(MobEffects.MINING_FATIGUE);
            return;
        }

        int dryAir = player.getData(MythosAttachments.SIREN_DRY_TICKS);
        if (dryAir < 0) {
            dryAir = SIREN_MAX_DRY_AIR;
        }

        if (isHydrated(player)) {
            dryAir = SIREN_MAX_DRY_AIR;
            player.removeEffect(MobEffects.SLOWNESS);
            player.removeEffect(MobEffects.MINING_FATIGUE);
        } else {
            applyDryLandDebuffs(player);
            int drainInterval = getDryAirDrainInterval(player);
            if (player.tickCount % drainInterval == 0) {
                dryAir = Math.max(0, dryAir - 1);
            }
            if (dryAir == 0) {
                player.hurtServer((ServerLevel) player.level(), player.damageSources().dryOut(), 2.0F);
                if (player instanceof ServerPlayer serverPlayer) {
                    if (player.tickCount % 100 == 0) {
                        serverPlayer.sendSystemMessage(Component.translatable("message.mythos.siren_drying_out"));
                    }
                }
            }
        }

        player.setData(MythosAttachments.SIREN_DRY_TICKS, dryAir);
        player.setAirSupply(Math.min(dryAir, player.getMaxAirSupply()));

        if (isInWater(player)) {
            player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, WATER_BUFF_DURATION_TICKS, 0, false, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, WATER_BUFF_DURATION_TICKS, 0, false, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, WATER_BUFF_DURATION_TICKS, 0, false, false, true));
        }
    }

    @SubscribeEvent
    public static void onItemFinished(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof Player player) || !(player.level() instanceof ServerLevel)) {
            return;
        }

        if (!MythState.is(player, SIREN)) {
            return;
        }

        ItemStack stack = event.getItem();
        if (isWaterBottle(stack)) {
            player.setData(MythosAttachments.SIREN_DRY_TICKS, SIREN_MAX_DRY_AIR);
            player.setAirSupply(player.getMaxAirSupply());
            player.sendSystemMessage(Component.translatable("message.mythos.siren_water_restored"));
        }
    }

    private static void handleSirenSmithing(Player player, SmithingMenu smithingMenu, boolean isSiren) {
        ItemStack template = smithingMenu.getSlot(SmithingMenu.TEMPLATE_SLOT).getItem();
        ItemStack base = smithingMenu.getSlot(SmithingMenu.BASE_SLOT).getItem();
        ItemStack addition = smithingMenu.getSlot(SmithingMenu.ADDITIONAL_SLOT).getItem();
        if (!template.isEmpty()) {
            return;
        }

        if (!isSirenElixirInputs(base, addition)) {
            return;
        }

        if (!isSiren) {
            clearSmithingResult(smithingMenu);
            if (player.tickCount % 40 == 0) {
                player.sendSystemMessage(Component.translatable("message.mythos.siren_only_elixir"));
            }
            return;
        }

        ItemStack result = new ItemStack(MythosItems.SIREN_ELIXIR.asItem());
        result.set(DataComponents.CUSTOM_NAME, Component.translatable("item.mythos.siren_elixir").withStyle(ChatFormatting.AQUA));
        ItemStack current = smithingMenu.getSlot(SmithingMenu.RESULT_SLOT).getItem();
        if (!ItemStack.matches(current, result)) {
            smithingMenu.getSlot(SmithingMenu.RESULT_SLOT).set(result);
            smithingMenu.broadcastChanges();
        }
    }

    private static boolean isSirenElixirInputs(ItemStack base, ItemStack addition) {
        return isWaterBottle(base) && addition.is(Items.NAUTILUS_SHELL);
    }

    private static boolean isWaterBottle(ItemStack stack) {
        if (!stack.is(Items.POTION)) {
            return false;
        }

        PotionContents contents = stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
        return contents.is(Potions.WATER);
    }

    private static boolean isHydrated(Player player) {
        return isInWater(player) || player.level().isRainingAt(player.blockPosition().above());
    }

    private static boolean isInWater(Player player) {
        return player.isInWater() || player.isEyeInFluid(FluidTags.WATER);
    }

    private static void applyDryLandDebuffs(Player player) {
        int depthStrider = getEquipmentEnchantmentLevel(player, EquipmentSlot.FEET, Enchantments.DEPTH_STRIDER);
        int aquaAffinity = getEquipmentEnchantmentLevel(player, EquipmentSlot.HEAD, Enchantments.AQUA_AFFINITY);

        int slownessAmplifier = 2 - depthStrider;
        if (slownessAmplifier >= 0) {
            player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, LAND_DEBUFF_DURATION_TICKS, slownessAmplifier, false, false, true));
        } else {
            player.removeEffect(MobEffects.SLOWNESS);
        }

        if (aquaAffinity <= 0) {
            player.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, LAND_DEBUFF_DURATION_TICKS, 0, false, false, true));
        } else {
            player.removeEffect(MobEffects.MINING_FATIGUE);
        }
    }

    private static int getDryAirDrainInterval(Player player) {
        int respiration = getEquipmentEnchantmentLevel(player, EquipmentSlot.HEAD, Enchantments.RESPIRATION);
        return Math.max(1, 1 + respiration);
    }

    private static int getEquipmentEnchantmentLevel(Player player, EquipmentSlot slot, ResourceKey<Enchantment> enchantmentKey) {
        ItemStack stack = player.getItemBySlot(slot);
        if (stack.isEmpty()) {
            return 0;
        }

        var enchantments = player.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        return EnchantmentHelper.getItemEnchantmentLevel(enchantments.getOrThrow(enchantmentKey), stack);
    }

    private static void clearSmithingResult(SmithingMenu smithingMenu) {
        if (!smithingMenu.getSlot(SmithingMenu.RESULT_SLOT).getItem().isEmpty()) {
            smithingMenu.getSlot(SmithingMenu.RESULT_SLOT).set(ItemStack.EMPTY);
            smithingMenu.broadcastChanges();
        }
    }
}
