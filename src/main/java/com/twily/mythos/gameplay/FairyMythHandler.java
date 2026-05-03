package com.twily.mythos.gameplay;

import com.twily.mythos.Mythos;
import com.twily.mythos.data.MythItemMarkerHelper;
import com.twily.mythos.myth.MythState;
import com.twily.mythos.registry.MythosAttachments;
import com.twily.mythos.registry.MythosItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Optional;

@EventBusSubscriber(modid = Mythos.MOD_ID)
public final class FairyMythHandler {

    private static final Identifier FAIRY = Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "fairy");
    private static final Identifier FAIRY_HEALTH = Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "fairy_health");
    private static final double FAIRY_HEALTH_REDUCTION = -0.5D;
    private static final float FAIRY_MELEE_DAMAGE_MULTIPLIER = 0.7F;
    private static final int FAIRY_FLIGHT_RANGE = 5;
    private static final int FAIRY_VISION_RADIUS = 5;
    private static final int FAIRY_VISION_DURATION_TICKS = 20 * 60 * 3;
    private static final int FAIRY_VISION_COOLDOWN_TICKS = 20 * 30;
    private static final String FAIRY_BOOTS_MARKER = "mythos_fairy_boots";
    private static final String FAIRY_WINGS_MARKER = "mythos_fairy_wings";

    private FairyMythHandler() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        boolean isFairy = MythState.is(player, FAIRY);
        boolean lowFlightAvailable = isFairy && hasSupportNearby(player, FAIRY_FLIGHT_RANGE);
        boolean fairyElytraMode = player.getData(MythosAttachments.FAIRY_ELYTRA_MODE);

        if (!player.level().isClientSide()) {
            tickFairyVisionCooldown(player);
            syncFairyBoots(player);
            syncFairyWings(player, isFairy);
            syncFairyHealth(player, isFairy);
            if (player.containerMenu instanceof SmithingMenu smithingMenu) {
                handleFairySmithing(player, smithingMenu, isFairy);
            }
            if (!isFairy) {
                if (fairyElytraMode) {
                    player.setData(MythosAttachments.FAIRY_ELYTRA_MODE, false);
                    fairyElytraMode = false;
                }
                syncFairyFlight(player, false, false);
                return;
            }

            if (fairyElytraMode) {
                if (player.isShiftKeyDown() && lowFlightAvailable && !player.onGround()) {
                    player.setData(MythosAttachments.FAIRY_ELYTRA_MODE, false);
                    fairyElytraMode = false;
                    switchToLowFlight(player);
                }
            } else {
                if (player.isFallFlying()) {
                    player.stopFallFlying();
                }
                applySlowFallingOutsideFlightZone(player, lowFlightAvailable);
            }

            syncFairyFlight(player, lowFlightAvailable, fairyElytraMode);
        }
    }

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Pre event) {
        if (event.getSource().is(DamageTypes.FLY_INTO_WALL) && event.getEntity() instanceof Player fairy
            && MythState.is(fairy, FAIRY) && fairy.getData(MythosAttachments.FAIRY_ELYTRA_MODE)) {
            event.setNewDamage(0.0F);
            return;
        }

        if (event.getSource().is(DamageTypeTags.IS_FALL) && event.getEntity() instanceof Player fairy && MythState.is(fairy, FAIRY)) {
            event.setNewDamage(0.0F);
            return;
        }

        if (!(event.getSource().getEntity() instanceof Player player) || !MythState.is(player, FAIRY)) {
            return;
        }

        if (event.getSource().getDirectEntity() == player) {
            event.setNewDamage(event.getNewDamage() * FAIRY_MELEE_DAMAGE_MULTIPLIER);
        }
    }

    public static void activateFairyVision(ServerPlayer player) {
        if (!MythState.is(player, FAIRY)) {
            return;
        }

        int cooldown = player.getData(MythosAttachments.FAIRY_VISION_COOLDOWN);
        if (cooldown > 0) {
            player.sendSystemMessage(Component.translatable("message.mythos.fairy_vision_cooldown", Math.max(1, cooldown / 20)));
            return;
        }

        player.level()
            .getEntitiesOfClass(Player.class, player.getBoundingBox().inflate(FAIRY_VISION_RADIUS), target -> target.isAlive())
            .forEach(target -> target.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, FAIRY_VISION_DURATION_TICKS, 0, false, true, true)));

        player.setData(MythosAttachments.FAIRY_VISION_COOLDOWN, FAIRY_VISION_COOLDOWN_TICKS);
    }

    public static void toggleFlightMode(ServerPlayer player) {
        if (!MythState.is(player, FAIRY)) {
            return;
        }

        syncFairyWings(player, true);
        boolean lowFlightAvailable = hasSupportNearby(player, FAIRY_FLIGHT_RANGE);
        boolean fairyElytraMode = player.getData(MythosAttachments.FAIRY_ELYTRA_MODE);

        if (fairyElytraMode) {
            player.setData(MythosAttachments.FAIRY_ELYTRA_MODE, false);
            if (player.isFallFlying()) {
                player.stopFallFlying();
            }
            if (!player.onGround() && !player.isInWater() && lowFlightAvailable) {
                switchToLowFlight(player);
            } else {
                syncFairyFlight(player, lowFlightAvailable, false);
            }
            applySlowFallingOutsideFlightZone(player, lowFlightAvailable);
            player.sendSystemMessage(Component.translatable("message.mythos.fairy_mode_low_flight"));
            return;
        }

        player.setData(MythosAttachments.FAIRY_ELYTRA_MODE, true);
        syncFairyFlight(player, false, true);
        if (canFallFly(player)) {
            switchToElytraFlight(player);
        }
        player.sendSystemMessage(Component.translatable("message.mythos.fairy_mode_elytra"));
    }

    private static void syncFairyHealth(Player player, boolean isFairy) {
        AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth == null) {
            return;
        }

        if (isFairy) {
            maxHealth.addOrUpdateTransientModifier(new AttributeModifier(FAIRY_HEALTH, FAIRY_HEALTH_REDUCTION, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        } else {
            maxHealth.removeModifier(FAIRY_HEALTH);
        }

        double currentMaxHealth = maxHealth.getValue();
        if (player.getHealth() > currentMaxHealth) {
            player.setHealth((float) currentMaxHealth);
        }
    }

    private static void syncFairyBoots(Player player) {
        ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
        if (MythItemMarkerHelper.hasMarker(boots, FAIRY_BOOTS_MARKER)) {
            player.addEffect(new MobEffectInstance(MobEffects.SPEED, 40, 0, false, false, true));
        }
    }

    private static void syncFairyWings(Player player, boolean isFairy) {
        if (!isFairy) {
            ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
            if (isFairyWings(chest)) {
                player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
                player.inventoryMenu.broadcastChanges();
            }
            return;
        }

        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!isFairyWings(chest)) {
            if (!chest.isEmpty()) {
                ItemStack displaced = chest.copy();
                player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
                if (!player.getInventory().add(displaced)) {
                    player.drop(displaced, false);
                }
            }
            player.setItemSlot(EquipmentSlot.CHEST, createFairyWings(player));
            player.inventoryMenu.broadcastChanges();
            return;
        }

        if (chest.isDamageableItem() && chest.getDamageValue() != 0) {
            chest.setDamageValue(0);
        }
    }

    private static ItemStack createFairyWings(Player player) {
        ItemStack result = new ItemStack(Items.ELYTRA);
        ItemEnchantments.Mutable enchantments = new ItemEnchantments.Mutable(result.getEnchantments());
        var registry = player.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        enchantments.set(registry.getOrThrow(Enchantments.BINDING_CURSE), 1);
        enchantments.set(registry.getOrThrow(Enchantments.VANISHING_CURSE), 1);
        result.set(DataComponents.ENCHANTMENTS, enchantments.toImmutable());
        result.set(DataComponents.CUSTOM_NAME, Component.translatable("item.mythos.fairy_wings").withStyle(ChatFormatting.LIGHT_PURPLE));
        MythItemMarkerHelper.setMarker(result, FAIRY_WINGS_MARKER);
        return result;
    }

    private static boolean isFairyWings(ItemStack stack) {
        return stack.is(Items.ELYTRA) && MythItemMarkerHelper.hasMarker(stack, FAIRY_WINGS_MARKER);
    }

    private static void syncFairyFlight(Player player, boolean allowLowFlight, boolean fairyElytraMode) {
        boolean shouldMayFly = (!fairyElytraMode && allowLowFlight) || player.isSpectator() || player.hasInfiniteMaterials();
        boolean changed = false;

        if (player.getAbilities().mayfly != shouldMayFly) {
            player.getAbilities().mayfly = shouldMayFly;
            changed = true;
        }

        if (!shouldMayFly && player.getAbilities().flying) {
            player.getAbilities().flying = false;
            changed = true;
        }

        if (changed) {
            player.onUpdateAbilities();
        }
    }

    private static void switchToLowFlight(Player player) {
        player.stopFallFlying();
        player.getAbilities().mayfly = true;
        player.getAbilities().flying = true;
        player.onUpdateAbilities();
        player.resetFallDistance();
    }

    private static void switchToElytraFlight(Player player) {
        if (player.getAbilities().flying) {
            player.getAbilities().flying = false;
        }
        if (player.getAbilities().mayfly) {
            player.getAbilities().mayfly = false;
            player.onUpdateAbilities();
        }
        if (!player.isFallFlying()) {
            player.startFallFlying();
        }
        player.resetFallDistance();
    }

    private static boolean canFallFly(Player player) {
        return !player.onGround() && !player.isInWater();
    }

    private static void applySlowFallingOutsideFlightZone(Player player, boolean lowFlightAvailable) {
        if (lowFlightAvailable || player.onGround() || player.isInWater() || player.isSpectator() || player.hasInfiniteMaterials()) {
            return;
        }

        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 40, 0, false, false, true));
    }

    private static void tickFairyVisionCooldown(Player player) {
        int cooldown = player.getData(MythosAttachments.FAIRY_VISION_COOLDOWN);
        if (cooldown > 0) {
            player.setData(MythosAttachments.FAIRY_VISION_COOLDOWN, cooldown - 1);
        }
    }

    private static void handleFairySmithing(Player player, SmithingMenu smithingMenu, boolean isFairy) {
        ItemStack template = smithingMenu.getSlot(SmithingMenu.TEMPLATE_SLOT).getItem();
        ItemStack base = smithingMenu.getSlot(SmithingMenu.BASE_SLOT).getItem();
        ItemStack addition = smithingMenu.getSlot(SmithingMenu.ADDITIONAL_SLOT).getItem();
        if (!template.isEmpty()) {
            return;
        }

        Optional<ItemStack> result = Optional.empty();
        if (isFairyBootsInputs(base, addition)) {
            result = createFairyBoots(base);
        } else if (isFairyMinecartInputs(base, addition)) {
            result = Optional.of(new ItemStack(MythosItems.FAIRY_MINECART.asItem()));
        } else {
            return;
        }

        if (!isFairy) {
            clearSmithingResult(smithingMenu);
            if (player.tickCount % 40 == 0) {
                player.sendSystemMessage(Component.translatable("message.mythos.fairy_only_artifacts"));
            }
            return;
        }

        if (result.isPresent()) {
            ItemStack current = smithingMenu.getSlot(SmithingMenu.RESULT_SLOT).getItem();
            if (!ItemStack.matches(current, result.get())) {
                smithingMenu.getSlot(SmithingMenu.RESULT_SLOT).set(result.get());
                smithingMenu.broadcastChanges();
            }
        } else {
            clearSmithingResult(smithingMenu);
        }
    }

    private static boolean isFairyBootsInputs(ItemStack base, ItemStack addition) {
        if (base.isEmpty() || !addition.is(Items.WIND_CHARGE)) {
            return false;
        }

        Equippable equippable = base.get(DataComponents.EQUIPPABLE);
        return equippable != null && equippable.slot() == EquipmentSlot.FEET;
    }

    private static boolean isFairyMinecartInputs(ItemStack base, ItemStack addition) {
        return base.is(Items.MINECART) && addition.is(Items.WIND_CHARGE);
    }

    private static Optional<ItemStack> createFairyBoots(ItemStack base) {
        if (MythItemMarkerHelper.hasMarker(base, FAIRY_BOOTS_MARKER)) {
            return Optional.empty();
        }

        ItemStack result = base.copyWithCount(1);
        result.set(DataComponents.CUSTOM_NAME, Component.translatable("item.mythos.fairy_boots").withStyle(ChatFormatting.LIGHT_PURPLE));
        MythItemMarkerHelper.setMarker(result, FAIRY_BOOTS_MARKER);
        return Optional.of(result);
    }

    private static void clearSmithingResult(SmithingMenu smithingMenu) {
        if (!smithingMenu.getSlot(SmithingMenu.RESULT_SLOT).getItem().isEmpty()) {
            smithingMenu.getSlot(SmithingMenu.RESULT_SLOT).set(ItemStack.EMPTY);
            smithingMenu.broadcastChanges();
        }
    }

    private static boolean hasSupportNearby(Player player, int maxDistance) {
        AABB playerBox = player.getBoundingBox();
        AABB searchBox = playerBox.inflate(maxDistance);
        double maxDistanceSquared = maxDistance * maxDistance;

        for (VoxelShape shape : player.level().getBlockCollisions(player, searchBox)) {
            if (shape.isEmpty()) {
                continue;
            }

            for (AABB collisionBox : shape.toAabbs()) {
                if (distanceSquared(playerBox, collisionBox) <= maxDistanceSquared) {
                    return true;
                }
            }
        }

        return false;
    }

    private static double distanceSquared(AABB source, AABB target) {
        double dx = axisGap(source.minX, source.maxX, target.minX, target.maxX);
        double dy = axisGap(source.minY, source.maxY, target.minY, target.maxY);
        double dz = axisGap(source.minZ, source.maxZ, target.minZ, target.maxZ);
        return dx * dx + dy * dy + dz * dz;
    }

    private static double axisGap(double minA, double maxA, double minB, double maxB) {
        if (maxA < minB) {
            return minB - maxA;
        }
        if (maxB < minA) {
            return minA - maxB;
        }
        return 0.0D;
    }
}
