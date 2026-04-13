package com.twily.mythos.gameplay;

import com.twily.mythos.Mythos;
import com.twily.mythos.data.MythItemMarkerHelper;
import com.twily.mythos.myth.MythState;
import com.twily.mythos.registry.MythosAttachments;
import com.twily.mythos.registry.MythosItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Optional;

@EventBusSubscriber(modid = Mythos.MOD_ID)
public final class FairyMythHandler {

    private static final Identifier FAIRY = Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "fairy");
    private static final Identifier FAIRY_HEALTH = Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "fairy_health");
    private static final Identifier FAIRY_ARMOR = Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "fairy_armor");
    private static final Identifier FAIRY_ARMOR_TOUGHNESS = Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "fairy_armor_toughness");
    private static final double FAIRY_HEALTH_REDUCTION = -0.5D;
    private static final double FAIRY_DEFENSE_REDUCTION = -0.3D;
    private static final float FAIRY_MELEE_DAMAGE_MULTIPLIER = 0.7F;
    private static final int FAIRY_FLIGHT_RANGE = 5;
    private static final int FAIRY_VISION_RADIUS = 5;
    private static final int FAIRY_VISION_DURATION_TICKS = 20 * 60 * 3;
    private static final int FAIRY_VISION_COOLDOWN_TICKS = 20 * 30;
    private static final String FAIRY_BOOTS_MARKER = "mythos_fairy_boots";

    private FairyMythHandler() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        boolean isFairy = MythState.is(player, FAIRY);
        boolean lowFlightAvailable = isFairy && hasSupportBelow(player, FAIRY_FLIGHT_RANGE);
        boolean fairyElytraMode = player.getData(MythosAttachments.FAIRY_ELYTRA_MODE);
        boolean wearingVanillaElytra = isWearingVanillaElytra(player);

        if (!player.level().isClientSide()) {
            tickFairyVisionCooldown(player);
            syncFairyBoots(player);
            syncFairyHealth(player, isFairy);
            syncFairyDefense(player, isFairy);
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

            if (wearingVanillaElytra) {
                if (fairyElytraMode) {
                    player.setData(MythosAttachments.FAIRY_ELYTRA_MODE, false);
                    fairyElytraMode = false;
                }
                syncFairyFlight(player, false, false);
                return;
            }

            if (fairyElytraMode) {
                if (player.onGround() || player.isInWater()) {
                    player.setData(MythosAttachments.FAIRY_ELYTRA_MODE, false);
                    fairyElytraMode = false;
                    if (player.isFallFlying()) {
                        player.stopFallFlying();
                    }
                } else if (lowFlightAvailable && player.isShiftKeyDown()) {
                    player.setData(MythosAttachments.FAIRY_ELYTRA_MODE, false);
                    fairyElytraMode = false;
                    switchToLowFlight(player);
                } else if (canFallFly(player) && !player.isFallFlying()) {
                    switchToElytraFlight(player);
                }
            } else if (!lowFlightAvailable && canFallFly(player)) {
                player.setData(MythosAttachments.FAIRY_ELYTRA_MODE, true);
                fairyElytraMode = true;
                switchToElytraFlight(player);
            }

            syncFairyFlight(player, lowFlightAvailable, fairyElytraMode);
        }
    }

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Pre event) {
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

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() || !MythState.is(player, FAIRY) || isWearingVanillaElytra(player)) {
            return;
        }

        ItemStack stack = player.getItemInHand(event.getHand());
        if (!stack.is(Items.FIREWORK_ROCKET) || !canFallFly(player)) {
            return;
        }

        if (!player.getData(MythosAttachments.FAIRY_ELYTRA_MODE)) {
            player.setData(MythosAttachments.FAIRY_ELYTRA_MODE, true);
        }
        if (!player.isFallFlying()) {
            switchToElytraFlight(player);
        }

        ServerLevel level = (ServerLevel) player.level();
        player.resetFallDistance();
        level.addFreshEntity(new FireworkRocketEntity(level, stack.copyWithCount(1), player));
        stack.consume(1, player);
        player.awardStat(Stats.ITEM_USED.get(Items.FIREWORK_ROCKET));
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
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

    private static void syncFairyDefense(Player player, boolean isFairy) {
        AttributeInstance armor = player.getAttribute(Attributes.ARMOR);
        if (armor != null) {
            if (isFairy) {
                armor.addOrUpdateTransientModifier(new AttributeModifier(FAIRY_ARMOR, FAIRY_DEFENSE_REDUCTION, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            } else {
                armor.removeModifier(FAIRY_ARMOR);
            }
        }

        AttributeInstance armorToughness = player.getAttribute(Attributes.ARMOR_TOUGHNESS);
        if (armorToughness != null) {
            if (isFairy) {
                armorToughness.addOrUpdateTransientModifier(new AttributeModifier(FAIRY_ARMOR_TOUGHNESS, FAIRY_DEFENSE_REDUCTION, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            } else {
                armorToughness.removeModifier(FAIRY_ARMOR_TOUGHNESS);
            }
        }
    }

    private static void syncFairyBoots(Player player) {
        ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
        if (MythItemMarkerHelper.hasMarker(boots, FAIRY_BOOTS_MARKER)) {
            player.addEffect(new MobEffectInstance(MobEffects.SPEED, 40, 0, false, false, true));
        }
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

    private static boolean isWearingVanillaElytra(Player player) {
        return player.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA);
    }

    private static boolean hasSupportBelow(Player player, int maxDistance) {
        AABB box = player.getBoundingBox();
        double minY = box.minY - 0.05D;
        double inset = Math.min(0.15D, box.getXsize() * 0.25D);
        double[] xSamples = new double[]{(box.minX + box.maxX) * 0.5D, box.minX + inset, box.maxX - inset};
        double[] zSamples = new double[]{(box.minZ + box.maxZ) * 0.5D, box.minZ + inset, box.maxZ - inset};

        for (double sampleX : xSamples) {
            for (double sampleZ : zSamples) {
                for (int distance = 0; distance <= maxDistance; distance++) {
                    BlockPos pos = BlockPos.containing(sampleX, minY - distance, sampleZ);
                    if (isSolidSupport(player.level().getBlockState(pos), player, pos)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private static boolean isSolidSupport(BlockState state, Player player, BlockPos pos) {
        return !state.isAir() && state.blocksMotion() && !state.getCollisionShape(player.level(), pos).isEmpty();
    }
}
