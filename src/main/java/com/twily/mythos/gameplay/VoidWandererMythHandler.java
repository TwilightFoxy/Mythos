package com.twily.mythos.gameplay;

import com.twily.mythos.Mythos;
import com.twily.mythos.myth.MythState;
import com.twily.mythos.registry.MythosAttachments;
import com.twily.mythos.registry.MythosItems;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.animal.equine.SkeletonHorse;
import net.minecraft.world.entity.animal.equine.ZombieHorse;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.PlayerRespawnPositionEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@EventBusSubscriber(modid = Mythos.MOD_ID)
public final class VoidWandererMythHandler {

    public static final Identifier VOID_WANDERER_ID = Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "void_wanderer");
    public static final int MAX_VOID = 100;
    private static final Identifier ADVANCEMENT_ID = Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "embrace_the_void");
    private static final int UNLOCK_Y = -1000;
    private static final int CHARGE_INTERVAL = 10;
    private static final int FLIGHT_DRAIN_INTERVAL = 10;
    private static final int PASSIVE_REFRESH_INTERVAL = 20;
    private static final int PASSIVE_DURATION = 100;
    private static final int REGEN_THRESHOLD = 50;
    private static final int BANISH_COST = 20;
    private static final float BANISH_HEAL = 6.0F;
    private static final int KILL_RESTORE = 6;
    private static final int VOID_FLIGHT_RANGE = 5;
    private static final int PATH_MEMORY = 100;
    private static final int RESPAWN_TARGET_DISTANCE = 50;
    private static final int RESPAWN_HORIZONTAL_SEARCH = 60;
    private static final int RESPAWN_VERTICAL_SEARCH = 24;
    private static final Map<UUID, Deque<BlockPos>> RECENT_PATHS = new HashMap<>();
    private static final Map<UUID, VoidRespawnMemory> PENDING_RESPAWNS = new HashMap<>();
    private static final Map<UUID, Boolean> BANISHED_MOBS = new HashMap<>();

    private VoidWandererMythHandler() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        if (player instanceof ServerPlayer serverPlayer) {
            syncUnlockAdvancements(serverPlayer);
            tryUnlock(level, serverPlayer);
        }

        boolean isVoidWanderer = MythState.matches(player, VOID_WANDERER_ID);
        if (!isVoidWanderer) {
            RECENT_PATHS.remove(player.getUUID());
            clearVoidState(player);
            return;
        }

        keepFoodFull(player);
        removeSuppressedEffects(player);
        trackRecentPath(player);
        syncVoidFlight(player);
        syncVoidEnergy(player);
        syncPassives(player);
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        if (!MythState.matches(player, VOID_WANDERER_ID)) {
            return;
        }

        ItemStack stack = player.getItemInHand(event.getHand());
        Consumable consumable = stack.get(DataComponents.CONSUMABLE);
        if (!stack.is(Items.MILK_BUCKET) && consumable == null) {
            return;
        }

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.FAIL);
        if (!player.level().isClientSide()) {
            player.sendSystemMessage(Component.translatable("message.mythos.void_wanderer_no_food"));
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !MythState.matches(player, VOID_WANDERER_ID)) {
            return;
        }

        BlockPos pos = event.getPos();
        BlockState state = player.level().getBlockState(pos);
        if (!state.is(BlockTags.BEDS) && !state.is(Blocks.RESPAWN_ANCHOR)) {
            return;
        }

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        player.sendSystemMessage(Component.translatable("message.mythos.void_wanderer_no_sleep"));
    }

    @SubscribeEvent
    public static void onDeath(net.neoforged.neoforge.event.entity.living.LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && MythState.matches(player, VOID_WANDERER_ID)) {
            Deque<BlockPos> path = RECENT_PATHS.getOrDefault(player.getUUID(), new ArrayDeque<>());
            PENDING_RESPAWNS.put(player.getUUID(), new VoidRespawnMemory(player.level().dimension(), player.blockPosition(), List.copyOf(path)));
        }

        if (!(event.getSource().getEntity() instanceof Player killer) || !(event.getEntity() instanceof LivingEntity target)) {
            return;
        }

        if (!MythState.matches(killer, VOID_WANDERER_ID) || killer.level().isClientSide() || target instanceof Player) {
            return;
        }

        UUID targetId = target.getUUID();
        if (BANISHED_MOBS.remove(targetId) != null) {
            return;
        }

        restoreVoid(killer, KILL_RESTORE);
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() || !MythState.matches(player, VOID_WANDERER_ID) || !(event.getTarget() instanceof LivingEntity target)) {
            return;
        }

        if (!isBanishableUndead(target)) {
            return;
        }

        event.setCanceled(true);
        if (!(player instanceof ServerPlayer serverPlayer) || !(player.level() instanceof ServerLevel level)) {
            event.setCancellationResult(InteractionResult.SUCCESS);
            return;
        }

        if (player.getData(MythosAttachments.VOID_WANDERER_ENERGY) < BANISH_COST) {
            event.setCancellationResult(InteractionResult.FAIL);
            player.sendSystemMessage(Component.translatable("message.mythos.void_wanderer_no_energy"));
            return;
        }

        spendVoid(player, BANISH_COST);
        BANISHED_MOBS.put(target.getUUID(), true);
        target.hurtServer(level, player.damageSources().magic(), 1000.0F);
        player.heal(BANISH_HEAL);
        level.sendParticles(ParticleTypes.PORTAL, target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(), 18, 0.25D, 0.4D, 0.25D, 0.08D);
        level.playSound(null, target.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8F, 0.65F);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    @SubscribeEvent
    public static void onLivingChangeTarget(LivingChangeTargetEvent event) {
        if (!(event.getEntity() instanceof Mob mob) || !(event.getNewAboutToBeSetTarget() instanceof Player player)) {
            return;
        }

        if (!MythState.matches(player, VOID_WANDERER_ID) || !isBanishableUndead(mob)) {
            return;
        }

        if (mob.getLastHurtByMob() == player) {
            return;
        }

        event.setNewAboutToBeSetTarget(null);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) {
            return;
        }

        Player original = event.getOriginal();
        Player clone = event.getEntity();
        if (MythState.matches(original, VOID_WANDERER_ID)) {
            clone.setData(MythosAttachments.VOID_WANDERER_ENERGY, original.getData(MythosAttachments.VOID_WANDERER_ENERGY));
        }
    }

    @SubscribeEvent
    public static void onRespawnPosition(PlayerRespawnPositionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (!MythState.matches(player, VOID_WANDERER_ID)) {
            return;
        }

        VoidRespawnMemory memory = PENDING_RESPAWNS.remove(player.getUUID());
        if (memory == null || player.level().getServer() == null) {
            return;
        }

        ServerLevel level = player.level().getServer().getLevel(memory.level());
        if (level == null) {
            return;
        }

        Optional<Vec3> target = findRespawnTarget(level, player, memory);
        if (target.isEmpty()) {
            return;
        }

        event.setTeleportTransition(new TeleportTransition(level, target.get(), Vec3.ZERO, player.getYRot(), player.getXRot(), TeleportTransition.PLAY_PORTAL_SOUND));
        event.setCopyOriginalSpawnPosition(false);
    }

    public static boolean isDarkEnough(Player player) {
        return player.level().getMaxLocalRawBrightness(player.blockPosition()) <= 4
            && player.level().getBrightness(LightLayer.BLOCK, player.blockPosition()) <= 4;
    }

    private static void tryUnlock(ServerLevel level, ServerPlayer player) {
        if (player.getData(MythosAttachments.VOID_WANDERER_UNLOCKED) || player.getY() > UNLOCK_Y) {
            return;
        }

        player.setData(MythosAttachments.VOID_WANDERER_UNLOCKED, true);
        ItemStack sphere = new ItemStack(MythosItems.MYTH_SPHERE.get());
        if (!player.getInventory().add(sphere)) {
            player.drop(sphere, false);
        }

        AdvancementHolder advancement = player.level().getServer() != null ? player.level().getServer().getAdvancements().get(ADVANCEMENT_ID) : null;
        if (advancement != null) {
            player.getAdvancements().award(advancement, "unlocked");
        }

        player.sendSystemMessage(Component.translatable("message.mythos.void_wanderer_unlocked"));
    }

    private static void syncUnlockAdvancements(ServerPlayer player) {
        if (!player.getData(MythosAttachments.VOID_WANDERER_UNLOCKED) || player.level().getServer() == null) {
            return;
        }

        AdvancementHolder advancement = player.level().getServer().getAdvancements().get(ADVANCEMENT_ID);
        if (advancement != null) {
            player.getAdvancements().award(advancement, "unlocked");
        }
    }

    private static void keepFoodFull(Player player) {
        FoodData foodData = player.getFoodData();
        foodData.setFoodLevel(20);
        foodData.setSaturation(5.0F);
    }

    private static void removeSuppressedEffects(Player player) {
        player.removeEffect(MobEffects.HUNGER);
    }

    private static void trackRecentPath(Player player) {
        Deque<BlockPos> path = RECENT_PATHS.computeIfAbsent(player.getUUID(), unused -> new ArrayDeque<>());
        BlockPos current = player.blockPosition();
        if (path.isEmpty() || !path.peekLast().equals(current)) {
            path.addLast(current);
            while (path.size() > PATH_MEMORY) {
                path.removeFirst();
            }
        }
    }

    private static void syncVoidEnergy(Player player) {
        int energy = player.getData(MythosAttachments.VOID_WANDERER_ENERGY);
        if (isConsumingFlight(player)) {
            if (!isFreeNightFlight(player) && player.tickCount % FLIGHT_DRAIN_INTERVAL == 0) {
                energy--;
            }
        } else if (isDarkEnough(player)) {
            if (player.tickCount % CHARGE_INTERVAL == 0) {
                energy++;
            }
        }

        player.setData(MythosAttachments.VOID_WANDERER_ENERGY, Math.clamp(energy, 0, MAX_VOID));
    }

    private static void syncPassives(Player player) {
        int energy = player.getData(MythosAttachments.VOID_WANDERER_ENERGY);
        if (energy >= REGEN_THRESHOLD && player.tickCount % PASSIVE_REFRESH_INTERVAL == 0) {
            ensureEffect(player, MobEffects.REGENERATION, PASSIVE_DURATION, 0);
        }
    }

    private static Optional<Vec3> findRespawnTarget(ServerLevel level, ServerPlayer player, VoidRespawnMemory memory) {
        Optional<Vec3> direct = findNearbySurface(level, player, memory.deathPos(), memory.deathPos());
        if (direct.isPresent()) {
            return direct;
        }

        return memory.trail().stream()
            .map(candidate -> safeFeetAt(level, player, candidate))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .min(Comparator.comparingDouble(vec -> Math.abs(horizontalDistance(vec, Vec3.atCenterOf(memory.deathPos())) - RESPAWN_TARGET_DISTANCE)));
    }

    private static Optional<Vec3> findNearbySurface(ServerLevel level, ServerPlayer player, BlockPos deathPos, BlockPos reference) {
        List<RespawnColumnCandidate> candidates = new ArrayList<>();
        for (int dx = -RESPAWN_HORIZONTAL_SEARCH; dx <= RESPAWN_HORIZONTAL_SEARCH; dx++) {
            for (int dz = -RESPAWN_HORIZONTAL_SEARCH; dz <= RESPAWN_HORIZONTAL_SEARCH; dz++) {
                double distance = Math.sqrt(dx * dx + dz * dz);
                if (distance < RESPAWN_TARGET_DISTANCE - 18 || distance > RESPAWN_TARGET_DISTANCE + 18) {
                    continue;
                }

                BlockPos column = new BlockPos(deathPos.getX() + dx, reference.getY(), deathPos.getZ() + dz);
                double score = Math.abs(distance - RESPAWN_TARGET_DISTANCE);
                candidates.add(new RespawnColumnCandidate(column, score));
            }
        }

        candidates.sort(Comparator.comparingDouble(RespawnColumnCandidate::score));
        for (RespawnColumnCandidate candidate : candidates) {
            Optional<Vec3> safe = safeFeetInColumn(level, player, candidate.column(), deathPos.getY());
            if (safe.isPresent()) {
                return safe;
            }
        }

        return Optional.empty();
    }

    private static Optional<Vec3> safeFeetInColumn(ServerLevel level, ServerPlayer player, BlockPos column, int referenceY) {
        for (int offset = 0; offset <= RESPAWN_VERTICAL_SEARCH; offset++) {
            if (offset == 0) {
                Optional<Vec3> exact = safeFeetAt(level, player, new BlockPos(column.getX(), referenceY, column.getZ()));
                if (exact.isPresent()) {
                    return exact;
                }
                continue;
            }

            Optional<Vec3> down = safeFeetAt(level, player, new BlockPos(column.getX(), referenceY - offset, column.getZ()));
            if (down.isPresent()) {
                return down;
            }

            Optional<Vec3> up = safeFeetAt(level, player, new BlockPos(column.getX(), referenceY + offset, column.getZ()));
            if (up.isPresent()) {
                return up;
            }
        }

        return Optional.empty();
    }

    private static Optional<Vec3> safeFeetAt(ServerLevel level, ServerPlayer player, BlockPos feetPos) {
        BlockPos floorPos = feetPos.below();
        BlockState floor = level.getBlockState(floorPos);
        if (!floor.isFaceSturdy(level, floorPos, Direction.UP)) {
            return Optional.empty();
        }

        if (!level.getFluidState(feetPos).isEmpty() || !level.getFluidState(feetPos.above()).isEmpty()) {
            return Optional.empty();
        }

        if (!level.getBlockState(feetPos).canBeReplaced() || !level.getBlockState(feetPos.above()).canBeReplaced()) {
            return Optional.empty();
        }

        Vec3 feet = new Vec3(feetPos.getX() + 0.5D, feetPos.getY(), feetPos.getZ() + 0.5D);
        EntityDimensions dimensions = player.getDimensions(player.getPose());
        AABB box = dimensions.makeBoundingBox(feet);
        if (!level.noCollision(player, box)) {
            return Optional.empty();
        }

        return Optional.of(feet);
    }

    private static boolean isBanishableUndead(LivingEntity target) {
        return !(target instanceof WitherBoss)
            && (target instanceof Zombie
            || target instanceof AbstractSkeleton
            || target instanceof ZombieHorse
            || target instanceof SkeletonHorse);
    }

    private static void syncVoidFlight(Player player) {
        boolean allowFlight = player.getData(MythosAttachments.VOID_WANDERER_ENERGY) > 0 && hasSupportNearby(player, VOID_FLIGHT_RANGE);
        boolean shouldMayFly = allowFlight || player.isSpectator() || player.hasInfiniteMaterials();
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

    private static boolean isConsumingFlight(Player player) {
        return player.getAbilities().flying && !player.isSpectator() && !player.hasInfiniteMaterials();
    }

    private static boolean isFreeNightFlight(Player player) {
        return player.level().dimension() == Level.OVERWORLD && player.level().getSkyDarken() >= 8;
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

    private static double horizontalDistance(Vec3 left, Vec3 right) {
        double dx = left.x - right.x;
        double dz = left.z - right.z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    private static void spendVoid(Player player, int amount) {
        player.setData(MythosAttachments.VOID_WANDERER_ENERGY, Math.max(0, player.getData(MythosAttachments.VOID_WANDERER_ENERGY) - amount));
    }

    private static void restoreVoid(Player player, int amount) {
        player.setData(MythosAttachments.VOID_WANDERER_ENERGY, Math.min(MAX_VOID, player.getData(MythosAttachments.VOID_WANDERER_ENERGY) + amount));
    }

    private static void clearVoidState(Player player) {
        player.removeEffect(MobEffects.REGENERATION);
        if (!player.isSpectator() && !player.hasInfiniteMaterials()) {
            if (player.getAbilities().flying) {
                player.getAbilities().flying = false;
            }
            if (player.getAbilities().mayfly) {
                player.getAbilities().mayfly = false;
            }
            player.onUpdateAbilities();
        }
    }

    private static void ensureEffect(Player player, net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect, int duration, int amplifier) {
        if (!player.hasEffect(effect)) {
            player.addEffect(new MobEffectInstance(effect, duration, amplifier, false, true, true));
            return;
        }

        var current = player.getEffect(effect);
        if (current != null && (current.getAmplifier() != amplifier || current.getDuration() < duration / 2)) {
            player.addEffect(new MobEffectInstance(effect, duration, amplifier, false, true, true));
        }
    }

    private record VoidRespawnMemory(ResourceKey<Level> level, BlockPos deathPos, List<BlockPos> trail) {
    }

    private record RespawnColumnCandidate(BlockPos column, double score) {
    }
}
