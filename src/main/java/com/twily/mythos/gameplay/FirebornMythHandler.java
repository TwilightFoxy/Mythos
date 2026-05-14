package com.twily.mythos.gameplay;

import com.twily.mythos.Mythos;
import com.twily.mythos.client.FirebornActionKeyHandler;
import com.twily.mythos.data.MythItemMarkerHelper;
import com.twily.mythos.myth.MythState;
import com.twily.mythos.registry.MythosAttachments;
import com.twily.mythos.registry.MythosItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Optional;

@EventBusSubscriber(modid = Mythos.MOD_ID)
public final class FirebornMythHandler {

    public static final Identifier FIREBORN_ID = Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "fireborn");

    private static final Identifier FIREBORN_COLD_SPEED = Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "fireborn_cold_speed");
    private static final Identifier FIREBORN_COLD_DAMAGE = Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "fireborn_cold_damage");
    private static final Identifier FIREBORN_HOT_SPEED = Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "fireborn_hot_speed");

    private static final int MAX_HEAT = 100;
    private static final int STAGE_EXTINGUISHING = 0;
    private static final int STAGE_SMOLDERING = 1;
    private static final int STAGE_STABLE = 2;
    private static final int STAGE_HEATED = 3;
    private static final int STAGE_OVERHEATED = 4;

    private static final int OVERWORLD_DRAIN_INTERVAL = 200;
    private static final int NETHER_DRAIN_INTERVAL = 800;
    private static final int RAIN_DRAIN_INTERVAL = 100;
    private static final int WATER_DRAIN_INTERVAL = 50;
    private static final int FIRE_RECHARGE_INTERVAL = 40;
    private static final int CAMPFIRE_RECHARGE_INTERVAL = 40;
    private static final int LAVA_RECHARGE_INTERVAL = 10;
    private static final int COAL_HEAT = 25;
    private static final int CHARCOAL_HEAT = 20;
    private static final int LAVA_BUCKET_HEAT = MAX_HEAT;
    private static final int FIREBALL_HEAT_COST = 10;
    private static final int FIRE_RING_HEAT_COST = 20;
    private static final double EXTINGUISHING_SPEED = -0.60D;
    private static final double EXTINGUISHING_DAMAGE = -0.35D;
    private static final double SMOLDERING_SPEED = -0.15D;
    private static final double SMOLDERING_DAMAGE = -0.15D;
    private static final double HEATED_SPEED = 0.10D;
    private static final double OVERHEATED_SPEED = 0.30D;
    private static final String FIREBORN_FUEL_MARKER = "mythos_fireborn_fuel";
    private static final FoodProperties FIREBORN_FUEL_PROPERTIES = new FoodProperties(0, 0.0F, true);
    private static final Consumable FIREBORN_FUEL_CONSUMABLE = Consumables.DEFAULT_FOOD;

    private FirebornMythHandler() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        boolean isFireborn = MythState.matches(player, FIREBORN_ID);
        syncFuelEdibility(player, isFireborn);

        if (player.level().isClientSide()) {
            return;
        }

        if (player.containerMenu instanceof SmithingMenu smithingMenu) {
            handleFirebornSmithing(player, smithingMenu, isFireborn);
        }

        if (!isFireborn) {
            clearFirebornState(player);
            return;
        }

        keepFoodFull(player);
        removeSuppressedEffects(player);
        syncEnvironmentalHeat((ServerLevel) player.level(), player);
        syncStageModifiers(player);
        syncStageParticles((ServerLevel) player.level(), player);
        syncLavaWalking(player);
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        if (!MythState.matches(player, FIREBORN_ID)) {
            return;
        }

        ItemStack stack = player.getItemInHand(event.getHand());
        if (!stack.is(Items.LAVA_BUCKET)) {
            return;
        }

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (player.level().isClientSide()) {
            return;
        }

        setHeat(player, LAVA_BUCKET_HEAT);
        player.setItemInHand(event.getHand(), new ItemStack(Items.BUCKET));
        player.level().playSound(null, player.blockPosition(), SoundEvents.GENERIC_DRINK.value(), SoundSource.PLAYERS, 0.75F, 0.85F);
    }

    @SubscribeEvent
    public static void onItemFinished(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof Player player) || !MythState.matches(player, FIREBORN_ID)) {
            return;
        }

        ItemStack stack = event.getItem();
        if (stack.is(Items.COAL)) {
            addHeat(player, COAL_HEAT);
        } else if (isCharcoal(stack)) {
            addHeat(player, CHARCOAL_HEAT);
        }
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player) || !MythState.matches(player, FIREBORN_ID)) {
            return;
        }

        if (event.getSource().is(DamageTypeTags.IS_FIRE)) {
            event.setCanceled(true);
        }
    }

    public static void castFireball(ServerPlayer player) {
        if (!MythState.matches(player, FIREBORN_ID)) {
            return;
        }

        if (!canUseHeatAbilities(player)) {
            player.sendSystemMessage(Component.translatable("message.mythos.fireborn_max_heat_required"));
            return;
        }

        Vec3 look = player.getLookAngle().normalize();
        SmallFireball fireball = new SmallFireball(player.level(), player, look);
        fireball.setPos(player.getX() + look.x * 1.2D, player.getEyeY() - 0.15D, player.getZ() + look.z * 1.2D);
        player.level().addFreshEntity(fireball);
        player.level().playSound(null, player.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.8F, 1.0F);
        spendHeatForAbility(player, FIREBALL_HEAT_COST);
    }

    public static void castFireRing(ServerPlayer player) {
        if (!MythState.matches(player, FIREBORN_ID)) {
            return;
        }

        if (!canUseHeatAbilities(player)) {
            player.sendSystemMessage(Component.translatable("message.mythos.fireborn_max_heat_required"));
            return;
        }

        ServerLevel level = (ServerLevel) player.level();
        level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(4.0D), entity -> entity.isAlive() && entity != player)
            .forEach(entity -> entity.hurtServer(level, player.damageSources().magic(), 6.0F));
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME, player.getX(), player.getY() + 1.0D, player.getZ(), 26, 1.4D, 0.35D, 1.4D, 0.02D);
        level.playSound(null, player.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.9F, 0.8F);
        spendHeatForAbility(player, FIRE_RING_HEAT_COST);
    }

    public static int heatStage(int heat) {
        if (heat < 10) {
            return STAGE_EXTINGUISHING;
        }
        if (heat < 30) {
            return STAGE_SMOLDERING;
        }
        if (heat < 60) {
            return STAGE_STABLE;
        }
        if (heat < 85) {
            return STAGE_HEATED;
        }
        return STAGE_OVERHEATED;
    }

    public static String stageTranslationKey(int stage) {
        return switch (stage) {
            case STAGE_EXTINGUISHING -> "gui.mythos.fireborn_heat.extinguishing";
            case STAGE_SMOLDERING -> "gui.mythos.fireborn_heat.smoldering";
            case STAGE_HEATED -> "gui.mythos.fireborn_heat.heated";
            case STAGE_OVERHEATED -> "gui.mythos.fireborn_heat.overheated";
            default -> "gui.mythos.fireborn_heat.stable";
        };
    }

    private static void keepFoodFull(Player player) {
        player.getFoodData().setFoodLevel(20);
        player.getFoodData().setSaturation(5.0F);
    }

    private static void removeSuppressedEffects(Player player) {
        player.removeEffect(MobEffects.POISON);
        player.removeEffect(MobEffects.HUNGER);
    }

    private static void syncEnvironmentalHeat(ServerLevel level, Player player) {
        int heat = player.getData(MythosAttachments.FIREBORN_HEAT);

        if (player.tickCount % activeDrainInterval(player) == 0) {
            heat += player.level().dimension() == Level.NETHER ? 1 : -1;
        }

        if (isRainCooling(player) && player.tickCount % RAIN_DRAIN_INTERVAL == 0) {
            heat--;
        }

        if (isWaterCooling(player) && player.tickCount % WATER_DRAIN_INTERVAL == 0) {
            heat -= 2;
        }

        if (isWarmingInFire(player) && player.tickCount % FIRE_RECHARGE_INTERVAL == 0) {
            heat++;
        }

        if (isWarmingOnCampfire(player) && player.tickCount % CAMPFIRE_RECHARGE_INTERVAL == 0) {
            heat++;
        }

        if (isWarmingInLava(player) && player.tickCount % LAVA_RECHARGE_INTERVAL == 0) {
            heat++;
        }

        setHeat(player, heat);
    }

    private static int activeDrainInterval(Player player) {
        return player.level().dimension() == Level.NETHER ? NETHER_DRAIN_INTERVAL : OVERWORLD_DRAIN_INTERVAL;
    }

    private static boolean isRainCooling(Player player) {
        return player.level().isRainingAt(player.blockPosition().above()) && !player.level().dimension().equals(Level.NETHER);
    }

    private static boolean isWaterCooling(Player player) {
        return player.isInWaterOrRain() || player.isEyeInFluid(FluidTags.WATER);
    }

    private static boolean isWarmingInLava(Player player) {
        return player.isInLava() || player.isEyeInFluid(FluidTags.LAVA);
    }

    private static boolean isWarmingInFire(Player player) {
        return player.isOnFire();
    }

    private static boolean isWarmingOnCampfire(Player player) {
        BlockPos onPos = player.getOnPosLegacy();
        var state = player.level().getBlockState(onPos);
        return (state.is(Blocks.CAMPFIRE) || state.is(Blocks.SOUL_CAMPFIRE))
            && state.hasProperty(CampfireBlock.LIT)
            && state.getValue(CampfireBlock.LIT);
    }

    private static void syncStageModifiers(Player player) {
        int heat = player.getData(MythosAttachments.FIREBORN_HEAT);
        int stage = heatStage(heat);
        int previousStage = player.getData(MythosAttachments.FIREBORN_STAGE);
        if (previousStage != stage) {
            player.setData(MythosAttachments.FIREBORN_STAGE, stage);
            player.sendSystemMessage(Component.translatable(stageTranslationKey(stage)));
        }

        double speed = switch (stage) {
            case STAGE_EXTINGUISHING -> EXTINGUISHING_SPEED;
            case STAGE_SMOLDERING -> SMOLDERING_SPEED;
            case STAGE_HEATED -> HEATED_SPEED;
            case STAGE_OVERHEATED -> OVERHEATED_SPEED;
            default -> 0.0D;
        };
        double damage = switch (stage) {
            case STAGE_EXTINGUISHING -> EXTINGUISHING_DAMAGE;
            case STAGE_SMOLDERING -> SMOLDERING_DAMAGE;
            default -> 0.0D;
        };

        MythStatusHelper.syncModifier(player, Attributes.MOVEMENT_SPEED, FIREBORN_COLD_SPEED, speed < 0.0D ? speed : 0.0D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, speed < 0.0D);
        MythStatusHelper.syncModifier(player, Attributes.ATTACK_DAMAGE, FIREBORN_COLD_DAMAGE, damage, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, damage < 0.0D);
        MythStatusHelper.syncModifier(player, Attributes.MOVEMENT_SPEED, FIREBORN_HOT_SPEED, speed > 0.0D ? speed : 0.0D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, speed > 0.0D);

        applyStageEffects(player, stage);
    }

    private static void syncStageParticles(ServerLevel level, Player player) {
        int stage = heatStage(player.getData(MythosAttachments.FIREBORN_HEAT));
        if (player.tickCount % 8 != 0) {
            return;
        }

        switch (stage) {
            case STAGE_EXTINGUISHING -> level.sendParticles(net.minecraft.core.particles.ParticleTypes.ASH, player.getX(), player.getY() + 1.0D, player.getZ(), 3, 0.25D, 0.35D, 0.25D, 0.01D);
            case STAGE_SMOLDERING -> level.sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE, player.getX(), player.getY() + 1.0D, player.getZ(), 4, 0.3D, 0.35D, 0.3D, 0.01D);
            case STAGE_HEATED -> level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME, player.getX(), player.getY() + 1.0D, player.getZ(), 6, 0.35D, 0.45D, 0.35D, 0.01D);
            case STAGE_OVERHEATED -> level.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME, player.getX(), player.getY() + 1.0D, player.getZ(), 7, 0.35D, 0.45D, 0.35D, 0.01D);
            default -> {
            }
        }
    }

    private static void syncLavaWalking(Player player) {
        if (!player.isInLava() || player.isShiftKeyDown()) {
            return;
        }

        FluidState fluid = player.level().getFluidState(BlockPos.containing(player.getX(), player.getY() - 0.2D, player.getZ()));
        if (!fluid.is(FluidTags.LAVA)) {
            return;
        }

        double surfaceY = Math.floor(player.getY()) + 0.92D;
        if (player.getY() < surfaceY) {
            player.setPos(player.getX(), surfaceY, player.getZ());
        }

        Vec3 movement = player.getDeltaMovement();
        player.setDeltaMovement(movement.x, Math.max(0.05D, movement.y), movement.z);
        player.resetFallDistance();
    }

    private static void handleFirebornSmithing(Player player, SmithingMenu smithingMenu, boolean isFireborn) {
        ItemStack template = smithingMenu.getSlot(SmithingMenu.TEMPLATE_SLOT).getItem();
        ItemStack base = smithingMenu.getSlot(SmithingMenu.BASE_SLOT).getItem();
        ItemStack addition = smithingMenu.getSlot(SmithingMenu.ADDITIONAL_SLOT).getItem();
        if (!template.isEmpty()) {
            return;
        }

        Optional<ItemStack> result = Optional.empty();
        if (isIfritLighterInputs(base, addition)) {
            result = Optional.of(new ItemStack(MythosItems.IFRIT_LIGHTER.get()));
        } else {
            return;
        }

        if (!isFireborn) {
            clearSmithingResult(smithingMenu);
            if (player.tickCount % 40 == 0) {
                player.sendSystemMessage(Component.translatable("message.mythos.fireborn_only_artifacts"));
            }
            return;
        }

        ItemStack current = smithingMenu.getSlot(SmithingMenu.RESULT_SLOT).getItem();
        if (!ItemStack.matches(current, result.get())) {
            smithingMenu.getSlot(SmithingMenu.RESULT_SLOT).set(result.get());
            smithingMenu.broadcastChanges();
        }
    }

    private static boolean isIfritLighterInputs(ItemStack base, ItemStack addition) {
        return base.is(Items.FLINT_AND_STEEL) && addition.is(Items.BLAZE_POWDER);
    }

    private static void clearSmithingResult(SmithingMenu smithingMenu) {
        if (!smithingMenu.getSlot(SmithingMenu.RESULT_SLOT).getItem().isEmpty()) {
            smithingMenu.getSlot(SmithingMenu.RESULT_SLOT).set(ItemStack.EMPTY);
            smithingMenu.broadcastChanges();
        }
    }

    private static void syncFuelEdibility(Player player, boolean isFireborn) {
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();
        int inventorySize = player.getInventory().getContainerSize();

        for (int slot = 0; slot < inventorySize; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            boolean shouldBeEdible = isFireborn && isFuel(stack) && (stack == mainHand || stack == offHand);
            if (shouldBeEdible) {
                applyFuelComponents(stack);
            } else {
                stripFuelComponents(stack);
            }
        }
    }

    private static void applyFuelComponents(ItemStack stack) {
        if (!isFuel(stack)) {
            return;
        }

        stack.set(DataComponents.FOOD, FIREBORN_FUEL_PROPERTIES);
        stack.set(DataComponents.CONSUMABLE, FIREBORN_FUEL_CONSUMABLE);
        MythItemMarkerHelper.setMarker(stack, FIREBORN_FUEL_MARKER);
    }

    private static void stripFuelComponents(ItemStack stack) {
        if (!MythItemMarkerHelper.hasMarker(stack, FIREBORN_FUEL_MARKER)) {
            return;
        }

        stack.remove(DataComponents.FOOD);
        stack.remove(DataComponents.CONSUMABLE);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.remove(FIREBORN_FUEL_MARKER));
    }

    private static boolean isFuel(ItemStack stack) {
        return stack.is(Items.COAL) || isCharcoal(stack);
    }

    private static boolean isCharcoal(ItemStack stack) {
        return stack.is(Items.CHARCOAL);
    }

    private static void setHeat(Player player, int heat) {
        player.setData(MythosAttachments.FIREBORN_HEAT, Math.clamp(heat, 0, MAX_HEAT));
    }

    private static void addHeat(Player player, int amount) {
        setHeat(player, player.getData(MythosAttachments.FIREBORN_HEAT) + amount);
    }

    private static boolean canUseHeatAbilities(Player player) {
        return heatStage(player.getData(MythosAttachments.FIREBORN_HEAT)) >= STAGE_HEATED;
    }

    private static void spendHeatForAbility(Player player, int cost) {
        int heat = player.getData(MythosAttachments.FIREBORN_HEAT);
        if (heat >= MAX_HEAT) {
            setHeat(player, MAX_HEAT - 1);
            return;
        }

        addHeat(player, -cost);
    }

    private static void clearFirebornState(Player player) {
        MythStatusHelper.syncModifier(player, Attributes.MOVEMENT_SPEED, FIREBORN_COLD_SPEED, EXTINGUISHING_SPEED, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, false);
        MythStatusHelper.syncModifier(player, Attributes.ATTACK_DAMAGE, FIREBORN_COLD_DAMAGE, EXTINGUISHING_DAMAGE, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, false);
        MythStatusHelper.syncModifier(player, Attributes.MOVEMENT_SPEED, FIREBORN_HOT_SPEED, OVERHEATED_SPEED, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, false);
        player.removeEffect(MobEffects.SLOWNESS);
        player.removeEffect(MobEffects.BLINDNESS);
        player.removeEffect(MobEffects.WEAKNESS);
        player.removeEffect(MobEffects.MINING_FATIGUE);
        player.removeEffect(MobEffects.SPEED);
        player.removeEffect(MobEffects.STRENGTH);
        player.setData(MythosAttachments.FIREBORN_STAGE, STAGE_STABLE);
    }

    private static void applyStageEffects(Player player, int stage) {
        switch (stage) {
            case STAGE_EXTINGUISHING -> {
                ensureEffect(player, MobEffects.SLOWNESS, 40, 1);
                ensureEffect(player, MobEffects.BLINDNESS, 40, 0);
                ensureEffect(player, MobEffects.WEAKNESS, 40, 0);
                ensureEffect(player, MobEffects.MINING_FATIGUE, 40, 0);
                player.removeEffect(MobEffects.SPEED);
                player.removeEffect(MobEffects.STRENGTH);
            }
            case STAGE_SMOLDERING -> {
                ensureEffect(player, MobEffects.SLOWNESS, 40, 0);
                ensureEffect(player, MobEffects.BLINDNESS, 40, 0);
                player.removeEffect(MobEffects.WEAKNESS);
                player.removeEffect(MobEffects.MINING_FATIGUE);
                player.removeEffect(MobEffects.SPEED);
                player.removeEffect(MobEffects.STRENGTH);
            }
            case STAGE_HEATED -> {
                player.removeEffect(MobEffects.SLOWNESS);
                player.removeEffect(MobEffects.BLINDNESS);
                player.removeEffect(MobEffects.WEAKNESS);
                player.removeEffect(MobEffects.MINING_FATIGUE);
                ensureEffect(player, MobEffects.SPEED, 40, 0);
                ensureEffect(player, MobEffects.STRENGTH, 40, 0);
            }
            case STAGE_OVERHEATED -> {
                player.removeEffect(MobEffects.SLOWNESS);
                player.removeEffect(MobEffects.BLINDNESS);
                player.removeEffect(MobEffects.WEAKNESS);
                player.removeEffect(MobEffects.MINING_FATIGUE);
                ensureEffect(player, MobEffects.SPEED, 40, 1);
                ensureEffect(player, MobEffects.STRENGTH, 40, 0);
            }
            default -> {
                player.removeEffect(MobEffects.SLOWNESS);
                player.removeEffect(MobEffects.BLINDNESS);
                player.removeEffect(MobEffects.WEAKNESS);
                player.removeEffect(MobEffects.MINING_FATIGUE);
                player.removeEffect(MobEffects.SPEED);
                player.removeEffect(MobEffects.STRENGTH);
            }
        }
    }

    private static void ensureEffect(Player player, net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect, int duration, int amplifier) {
        if (!player.hasEffect(effect)) {
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(effect, duration, amplifier, false, true, true));
            return;
        }

        var current = player.getEffect(effect);
        if (current != null && (current.getAmplifier() != amplifier || current.getDuration() < duration / 2)) {
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(effect, duration, amplifier, false, true, true));
        }
    }
}
