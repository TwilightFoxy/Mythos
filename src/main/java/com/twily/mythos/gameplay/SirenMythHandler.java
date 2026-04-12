package com.twily.mythos.gameplay;

import com.twily.mythos.Mythos;
import com.twily.mythos.myth.MythState;
import com.twily.mythos.registry.MythosAttachments;
import com.twily.mythos.registry.MythosEffects;
import com.twily.mythos.registry.MythosItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
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
    private static final Identifier SIREN_DRY_LAND_SLOW = Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "siren_dry_land_slow");
    private static final int SIREN_MAX_DRY_AIR = 300;
    private static final int SIREN_PERSISTENT_STATUS_TICKS = 20 * 60 * 60;
    private static final int WATER_BUFF_DURATION_TICKS = 20 * 10;
    private static final int WATER_BUFF_REAPPLY_THRESHOLD_TICKS = 20 * 5;
    private static final int DRY_DAMAGE_INTERVAL_TICKS = 20;
    private static final int SIREN_FATIGUE_DURATION_TICKS = 20 * 10;
    private static final int SIREN_FATIGUE_REAPPLY_THRESHOLD = 20 * 5;

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
                if (player.getAirSupply() != player.getMaxAirSupply()) {
                    player.setAirSupply(player.getMaxAirSupply());
                }
            }
            clearDryLandState(player);
            clearWaterBuffs(player);
            return;
        }

        int dryAir = player.getData(MythosAttachments.SIREN_DRY_TICKS);
        if (dryAir < 0) {
            dryAir = SIREN_MAX_DRY_AIR;
        }

        boolean inWater = isInWater(player);
        boolean moistureSupport = hasMoistureSupport(player);

        if (moistureSupport) {
            dryAir = SIREN_MAX_DRY_AIR;
            clearDryLandState(player);
            syncWaterBuffs(player, inWater);
        } else {
            syncWaterBuffs(player, false);
            int depthStrider = getEquipmentEnchantmentLevel(player, EquipmentSlot.FEET, Enchantments.DEPTH_STRIDER);
            syncDryLandSpeed(player, true, depthStrider);
            syncDryLandDisplay(player, depthStrider);
            int aquaAffinity = getEquipmentEnchantmentLevel(player, EquipmentSlot.HEAD, Enchantments.AQUA_AFFINITY);
            syncDryLandFatigue(player, aquaAffinity <= 0);
            int drainInterval = getDryAirDrainInterval(player);
            if (player.tickCount % drainInterval == 0) {
                dryAir = Math.max(0, dryAir - 1);
            }
            if (dryAir == 0 && player.tickCount % DRY_DAMAGE_INTERVAL_TICKS == 0) {
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

    private static boolean hasMoistureSupport(Player player) {
        return isInWater(player)
            || player.level().isRainingAt(player.blockPosition().above())
            || hasSirenElixirGrace(player);
    }

    private static boolean isInWater(Player player) {
        return player.isInWater() || player.isEyeInFluid(FluidTags.WATER);
    }

    private static boolean hasSirenElixirGrace(Player player) {
        return player.hasEffect(MythosEffects.SIREN_ELIXIR_GRACE);
    }

    private static void syncDryLandSpeed(Player player, boolean active, int depthStrider) {
        double amount = switch (depthStrider) {
            case 0 -> -0.45D; // Slowness III
            case 1 -> -0.30D; // Slowness II
            case 2 -> -0.15D; // Slowness I
            default -> 0.0D;
        };

        MythStatusHelper.syncModifier(
            player,
            Attributes.MOVEMENT_SPEED,
            SIREN_DRY_LAND_SLOW,
            amount,
            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
            active && amount < 0.0D
        );
    }

    private static void syncDryLandDisplay(Player player, int depthStrider) {
        int newStage = switch (depthStrider) {
            case 0 -> 3;
            case 1 -> 2;
            case 2 -> 1;
            default -> 0;
        };

        int currentStage = player.getData(MythosAttachments.SIREN_DRY_STAGE);
        if (currentStage == newStage) {
            switch (newStage) {
                case 1 -> ensureDisplayEffect(player, MythosEffects.SIREN_DRY_SLOW_I, SIREN_PERSISTENT_STATUS_TICKS);
                case 2 -> ensureDisplayEffect(player, MythosEffects.SIREN_DRY_SLOW_II, SIREN_PERSISTENT_STATUS_TICKS);
                case 3 -> ensureDisplayEffect(player, MythosEffects.SIREN_DRY_SLOW_III, SIREN_PERSISTENT_STATUS_TICKS);
                default -> {
                }
            }
            return;
        }

        removeDrySlowEffects(player);
        player.setData(MythosAttachments.SIREN_DRY_STAGE, newStage);

        switch (newStage) {
            case 1 -> ensureDisplayEffect(player, MythosEffects.SIREN_DRY_SLOW_I, SIREN_PERSISTENT_STATUS_TICKS);
            case 2 -> ensureDisplayEffect(player, MythosEffects.SIREN_DRY_SLOW_II, SIREN_PERSISTENT_STATUS_TICKS);
            case 3 -> ensureDisplayEffect(player, MythosEffects.SIREN_DRY_SLOW_III, SIREN_PERSISTENT_STATUS_TICKS);
            default -> {
            }
        }
    }

    private static void syncDryLandFatigue(Player player, boolean active) {
        boolean current = player.getData(MythosAttachments.SIREN_DRY_FATIGUE_ACTIVE);
        if (active) {
            MobEffectInstance currentFatigue = player.getEffect(MobEffects.MINING_FATIGUE);
            if (!current || currentFatigue == null || currentFatigue.getAmplifier() != 0 || currentFatigue.getDuration() <= SIREN_FATIGUE_REAPPLY_THRESHOLD) {
                player.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, SIREN_FATIGUE_DURATION_TICKS, 0, false, true, true));
            }
            player.setData(MythosAttachments.SIREN_DRY_FATIGUE_ACTIVE, true);
        } else {
            if (current) {
                player.removeEffect(MobEffects.MINING_FATIGUE);
            }
            player.setData(MythosAttachments.SIREN_DRY_FATIGUE_ACTIVE, false);
        }
    }

    private static void clearDryLandState(Player player) {
        syncDryLandSpeed(player, false, 0);
        removeDrySlowEffects(player);
        player.setData(MythosAttachments.SIREN_DRY_STAGE, 0);
        syncDryLandFatigue(player, false);
    }

    private static void removeDrySlowEffects(Player player) {
        player.removeEffect(MythosEffects.SIREN_DRY_SLOW_I);
        player.removeEffect(MythosEffects.SIREN_DRY_SLOW_II);
        player.removeEffect(MythosEffects.SIREN_DRY_SLOW_III);
    }

    private static void syncWaterBuffs(Player player, boolean active) {
        boolean current = player.getData(MythosAttachments.SIREN_WATER_BUFFS_ACTIVE);
        if (current == active) {
            if (active) {
                ensureOwnedWaterEffect(player, MobEffects.WATER_BREATHING, MythosAttachments.SIREN_WATER_BREATHING_OWNED.get());
                ensureOwnedWaterEffect(player, MobEffects.DOLPHINS_GRACE, MythosAttachments.SIREN_DOLPHINS_GRACE_OWNED.get());
                ensureOwnedWaterEffect(player, MobEffects.NIGHT_VISION, MythosAttachments.SIREN_NIGHT_VISION_OWNED.get());
            } else {
                clearOwnedWaterEffectFlagIfExpired(player, MobEffects.WATER_BREATHING, MythosAttachments.SIREN_WATER_BREATHING_OWNED.get());
                clearOwnedWaterEffectFlagIfExpired(player, MobEffects.DOLPHINS_GRACE, MythosAttachments.SIREN_DOLPHINS_GRACE_OWNED.get());
                clearOwnedWaterEffectFlagIfExpired(player, MobEffects.NIGHT_VISION, MythosAttachments.SIREN_NIGHT_VISION_OWNED.get());
            }
            return;
        }

        player.setData(MythosAttachments.SIREN_WATER_BUFFS_ACTIVE, active);
        if (active) {
            ensureOwnedWaterEffect(player, MobEffects.WATER_BREATHING, MythosAttachments.SIREN_WATER_BREATHING_OWNED.get());
            ensureOwnedWaterEffect(player, MobEffects.DOLPHINS_GRACE, MythosAttachments.SIREN_DOLPHINS_GRACE_OWNED.get());
            ensureOwnedWaterEffect(player, MobEffects.NIGHT_VISION, MythosAttachments.SIREN_NIGHT_VISION_OWNED.get());
        } else {
            clearOwnedWaterEffectFlagIfExpired(player, MobEffects.WATER_BREATHING, MythosAttachments.SIREN_WATER_BREATHING_OWNED.get());
            clearOwnedWaterEffectFlagIfExpired(player, MobEffects.DOLPHINS_GRACE, MythosAttachments.SIREN_DOLPHINS_GRACE_OWNED.get());
            clearOwnedWaterEffectFlagIfExpired(player, MobEffects.NIGHT_VISION, MythosAttachments.SIREN_NIGHT_VISION_OWNED.get());
        }
    }

    private static void clearWaterBuffs(Player player) {
        player.setData(MythosAttachments.SIREN_WATER_BUFFS_ACTIVE, false);
        player.setData(MythosAttachments.SIREN_WATER_BREATHING_OWNED.get(), false);
        player.setData(MythosAttachments.SIREN_DOLPHINS_GRACE_OWNED.get(), false);
        player.setData(MythosAttachments.SIREN_NIGHT_VISION_OWNED.get(), false);
    }

    private static void ensureDisplayEffect(Player player, Holder<MobEffect> effect, int duration) {
        MobEffectInstance current = player.getEffect(effect);
        if (current == null) {
            player.addEffect(new MobEffectInstance(effect, duration, 0, false, true, true));
        }
    }

    private static void ensureOwnedWaterEffect(Player player, Holder<MobEffect> effect, net.neoforged.neoforge.attachment.AttachmentType<Boolean> ownedAttachment) {
        MobEffectInstance current = player.getEffect(effect);
        boolean owned = player.getData(ownedAttachment);
        if (current == null) {
            player.addEffect(new MobEffectInstance(effect, WATER_BUFF_DURATION_TICKS, 0, false, false, true));
            player.setData(ownedAttachment, true);
            return;
        }

        if (owned && current.getDuration() <= WATER_BUFF_REAPPLY_THRESHOLD_TICKS) {
            player.addEffect(new MobEffectInstance(effect, WATER_BUFF_DURATION_TICKS, 0, false, false, true));
        } else if (!owned && current.getDuration() <= WATER_BUFF_REAPPLY_THRESHOLD_TICKS) {
            player.addEffect(new MobEffectInstance(effect, WATER_BUFF_DURATION_TICKS, 0, false, false, true));
            player.setData(ownedAttachment, true);
        }
    }

    private static void clearOwnedWaterEffectFlagIfExpired(Player player, Holder<MobEffect> effect, net.neoforged.neoforge.attachment.AttachmentType<Boolean> ownedAttachment) {
        if (player.getData(ownedAttachment) && player.getEffect(effect) == null) {
            player.setData(ownedAttachment, false);
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
