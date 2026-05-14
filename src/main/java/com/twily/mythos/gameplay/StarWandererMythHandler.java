package com.twily.mythos.gameplay;

import com.twily.mythos.Mythos;
import com.twily.mythos.myth.MythState;
import com.twily.mythos.registry.MythosAttachments;
import com.twily.mythos.registry.MythosBlocks;
import com.twily.mythos.registry.MythosEffects;
import com.twily.mythos.registry.MythosItems;
import com.twily.mythos.world.block.AstralLanternBlock;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@EventBusSubscriber(modid = Mythos.MOD_ID)
public final class StarWandererMythHandler {

    public static final Identifier STAR_WANDERER_ID = Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "star_wanderer");
    public static final int MAX_ENERGY = 200;
    private static final int MOBILITY_ENERGY_THRESHOLD = MAX_ENERGY / 4;
    private static final int REGEN_ENERGY_THRESHOLD = (MAX_ENERGY * 3) / 4;

    private static final Identifier NO_STARS_HEALTH = Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "star_wanderer_no_stars_health");
    private static final Identifier STAR_WEAKNESS_DAMAGE = Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "star_wanderer_no_stars_damage");
    private static final Identifier ADVANCEMENT_ID = Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "touch_the_stars");
    private static final int UNLOCK_Y = 10_000;
    private static final int CHARGE_INTERVAL = 10;
    private static final int PASSIVE_REFRESH_INTERVAL = 20;
    private static final int PASSIVE_DURATION = 100;
    private static final int STEP_COST = 20;
    private static final int BEAM_ENERGY_INTERVAL = 2;
    private static final int BEAM_DAMAGE_INTERVAL = 4;
    private static final int BEAM_COST = 1;
    private static final int WAVE_COST = 15;
    private static final double STEP_MAX_DISTANCE = 20.0D;
    private static final double STEP_SEARCH_RADIUS = 2.0D;
    private static final double STEP_OPEN_EYE_OFFSET = 1.62D;
    private static final float STAR_BEAM_DAMAGE = 5.0F;
    private static final float STAR_WAVE_DAMAGE = 8.0F;
    private static final int MAX_LANTERN_RADIUS = 32;
    private static final int LANTERN_VERTICAL_RANGE = 8;

    private StarWandererMythHandler() {
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

        boolean isStarWanderer = MythState.matches(player, STAR_WANDERER_ID);
        if (player.containerMenu instanceof SmithingMenu smithingMenu) {
            handleStarSmithing(player, smithingMenu, isStarWanderer);
        }

        if (!isStarWanderer) {
            clearStarState(player);
            return;
        }

        syncEnergy(player);
        syncPassives(player);
        tickStarBeam(level, player);
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        BlockPos pos = event.getPos();
        if (!MythState.matches(player, STAR_WANDERER_ID)) {
            return;
        }

        if (!player.level().getBlockState(pos).is(BlockTags.BEDS)) {
            return;
        }

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        player.setRespawnPosition(new ServerPlayer.RespawnConfig(LevelData.RespawnData.of(player.level().dimension(), pos, player.getYRot(), player.getXRot()), false), true);
        player.sendSystemMessage(Component.translatable("message.mythos.star_wanderer_no_sleep"));
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !MythState.matches(player, STAR_WANDERER_ID)) {
            return;
        }

        int energy = player.getData(MythosAttachments.STAR_WANDERER_ENERGY);
        if (energy <= 0 || !(player.level() instanceof ServerLevel level)) {
            return;
        }

        float radius = 2.0F + energy / 25.0F;
        float damage = 4.0F + energy / 8.0F;
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, player.getX(), player.getY() + 1.0D, player.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
        level.playSound(null, player.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.0F, 0.9F);
        level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(radius), entity -> entity.isAlive() && entity != player)
            .forEach(entity -> entity.hurtServer(level, player.damageSources().magic(), damage));
        player.setData(MythosAttachments.STAR_WANDERER_ENERGY, 0);
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player) || !MythState.matches(player, STAR_WANDERER_ID)) {
            return;
        }

        if (event.getSource().is(net.minecraft.tags.DamageTypeTags.IS_FALL)) {
            event.setCanceled(true);
            player.resetFallDistance();
        }
    }

    @SubscribeEvent
    public static void onMobSpawn(MobSpawnEvent.PositionCheck event) {
        if (!(event.getEntity() instanceof Enemy)) {
            return;
        }

        if (event.getSpawnType() == EntitySpawnReason.SPAWNER) {
            return;
        }

        BlockPos center = BlockPos.containing(event.getX(), event.getY(), event.getZ());
        if (hasAstralLanternNearby(event.getLevel(), center)) {
            event.setResult(MobSpawnEvent.PositionCheck.Result.FAIL);
        }
    }

    public static Optional<Vec3> previewStarStepTarget(Player player) {
        return findStarStepTarget(player);
    }

    public static void performStarStep(ServerPlayer player) {
        if (!MythState.matches(player, STAR_WANDERER_ID)) {
            return;
        }

        if (player.getData(MythosAttachments.STAR_WANDERER_ENERGY) < STEP_COST) {
            player.sendSystemMessage(Component.translatable("message.mythos.star_wanderer_no_energy"));
            return;
        }

        Optional<Vec3> target = findStarStepTarget(player);
        if (target.isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.mythos.star_wanderer_step_blocked"));
            return;
        }

        Vec3 destination = target.get();
        spendEnergy(player, STEP_COST);
        player.resetFallDistance();
        player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 12, 0, false, false, false));
        player.connection.teleport(destination.x, destination.y, destination.z, player.getYRot(), player.getXRot());
        player.level().playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, player.getSoundSource(), 0.75F, 1.2F);
    }

    public static void setBeamActive(ServerPlayer player, boolean active) {
        if (!MythState.matches(player, STAR_WANDERER_ID)) {
            player.setData(MythosAttachments.STAR_WANDERER_BEAM_ACTIVE, false);
            return;
        }

        if (active && player.getData(MythosAttachments.STAR_WANDERER_ENERGY) <= 0) {
            player.sendSystemMessage(Component.translatable("message.mythos.star_wanderer_no_energy"));
            player.setData(MythosAttachments.STAR_WANDERER_BEAM_ACTIVE, false);
            return;
        }

        player.setData(MythosAttachments.STAR_WANDERER_BEAM_ACTIVE, active);
    }

    public static void castStarWave(ServerPlayer player) {
        if (!MythState.matches(player, STAR_WANDERER_ID)) {
            return;
        }

        if (player.getData(MythosAttachments.STAR_WANDERER_ENERGY) < WAVE_COST) {
            player.sendSystemMessage(Component.translatable("message.mythos.star_wanderer_no_energy"));
            return;
        }

        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        Vec3 center = player.position().add(0.0D, player.getBbHeight() * 0.5D, 0.0D);
        boolean affected = false;
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(4.0D), entity -> entity.isAlive() && entity != player)) {
            Vec3 push = entity.position().subtract(center).multiply(1.0D, 0.0D, 1.0D);
            if (push.lengthSqr() < 0.0001D) {
                push = player.getLookAngle().multiply(1.0D, 0.0D, 1.0D);
            }
            if (push.lengthSqr() < 0.0001D) {
                push = new Vec3(1.0D, 0.0D, 0.0D);
            }

            Vec3 normalized = push.normalize().scale(1.2D);
            entity.push(normalized.x, 0.45D, normalized.z);
            entity.hurtMarked = true;
            entity.hurtServer(level, player.damageSources().magic(), STAR_WAVE_DAMAGE);
            affected = true;
        }

        level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.0D, player.getZ(), 18, 1.25D, 0.35D, 1.25D, 0.03D);
        level.sendParticles(ParticleTypes.FIREWORK, player.getX(), player.getY() + 1.0D, player.getZ(), 12, 1.0D, 0.3D, 1.0D, 0.02D);
        level.playSound(null, player.blockPosition(), SoundEvents.FIREWORK_ROCKET_TWINKLE, SoundSource.PLAYERS, 0.8F, affected ? 1.2F : 1.35F);
        spendEnergy(player, WAVE_COST);
    }

    private static void tryUnlock(ServerLevel level, ServerPlayer player) {
        if (player.getData(MythosAttachments.STAR_WANDERER_UNLOCKED)) {
            return;
        }

        BlockPos unlockPos = player.blockPosition().above();
        if (level.dimension() != Level.OVERWORLD || player.getY() < UNLOCK_Y || !level.canSeeSky(unlockPos) || level.isRainingAt(unlockPos)) {
            return;
        }

        player.setData(MythosAttachments.STAR_WANDERER_UNLOCKED, true);
        ItemStack sphere = new ItemStack(MythosItems.MYTH_SPHERE.get());
        if (!player.getInventory().add(sphere)) {
            player.drop(sphere, false);
        }

        AdvancementHolder advancement = player.level().getServer() != null ? player.level().getServer().getAdvancements().get(ADVANCEMENT_ID) : null;
        if (advancement != null) {
            player.getAdvancements().award(advancement, "unlocked");
        }

        player.sendSystemMessage(Component.translatable("message.mythos.star_wanderer_unlocked"));
    }

    private static void syncUnlockAdvancements(ServerPlayer player) {
        if (!player.getData(MythosAttachments.STAR_WANDERER_UNLOCKED) || player.level().getServer() == null) {
            return;
        }

        AdvancementHolder advancement = player.level().getServer().getAdvancements().get(ADVANCEMENT_ID);
        if (advancement != null) {
            player.getAdvancements().award(advancement, "unlocked");
        }
    }

    private static void syncEnergy(Player player) {
        int energy = player.getData(MythosAttachments.STAR_WANDERER_ENERGY);
        if (hasClearStarSky(player)) {
            if (player.tickCount % CHARGE_INTERVAL == 0) {
                energy++;
            }
        }

        player.setData(MythosAttachments.STAR_WANDERER_ENERGY, Math.clamp(energy, 0, MAX_ENERGY));
    }

    private static void syncPassives(Player player) {
        int energy = player.getData(MythosAttachments.STAR_WANDERER_ENERGY);
        boolean starless = isStarlessRealm(player.level());
        boolean mobilityReady = energy >= MOBILITY_ENERGY_THRESHOLD;
        boolean regenReady = energy >= REGEN_ENERGY_THRESHOLD;
        boolean refreshPassives = player.tickCount % PASSIVE_REFRESH_INTERVAL == 0;

        if (mobilityReady) {
            if (refreshPassives) {
                ensureEffect(player, MythosEffects.STAR_WANDERER_ASCENT, PASSIVE_DURATION, 0);
            }
        }

        if (regenReady) {
            if (refreshPassives) {
                ensureEffect(player, MobEffects.REGENERATION, PASSIVE_DURATION, 0);
            }
        }

        if (starless) {
            if (refreshPassives) {
                ensureEffect(player, MobEffects.SLOWNESS, PASSIVE_DURATION, 0);
                ensureEffect(player, MobEffects.WEAKNESS, PASSIVE_DURATION, 0);
            }
        } else {
            player.removeEffect(MobEffects.SLOWNESS);
            player.removeEffect(MobEffects.WEAKNESS);
        }

        syncStarlessHealth(player, starless);
    }

    private static void tickStarBeam(ServerLevel level, Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        if (!player.getData(MythosAttachments.STAR_WANDERER_BEAM_ACTIVE)) {
            return;
        }

        int energy = player.getData(MythosAttachments.STAR_WANDERER_ENERGY);
        if (energy <= 0) {
            player.setData(MythosAttachments.STAR_WANDERER_BEAM_ACTIVE, false);
            return;
        }

        if (player.tickCount % BEAM_ENERGY_INTERVAL == 0) {
            spendEnergy(player, BEAM_COST);
        }

        EntityHitResult hit = findTargetEntity(serverPlayer, 32.0D);
        Vec3 eye = player.getEyePosition();
        Vec3 end = hit != null ? hit.getLocation() : eye.add(player.getLookAngle().scale(32.0D));
        Vec3 mid = eye.add(end).scale(0.5D);
        Vec3 span = end.subtract(eye);

        level.sendParticles(ParticleTypes.END_ROD, mid.x, mid.y, mid.z, 12, Math.abs(span.x) * 0.15D, Math.abs(span.y) * 0.15D, Math.abs(span.z) * 0.15D, 0.01D);
        level.sendParticles(ParticleTypes.GLOW, end.x, end.y, end.z, 3, 0.05D, 0.05D, 0.05D, 0.0D);

        if (hit != null && hit.getEntity() instanceof LivingEntity target && player.tickCount % BEAM_DAMAGE_INTERVAL == 0) {
            target.hurtServer(level, player.damageSources().magic(), STAR_BEAM_DAMAGE);
            level.playSound(null, target.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.45F, 1.65F);
        }

        if (player.getData(MythosAttachments.STAR_WANDERER_ENERGY) <= 0) {
            player.setData(MythosAttachments.STAR_WANDERER_BEAM_ACTIVE, false);
        }
    }

    private static void syncStarlessHealth(Player player, boolean starless) {
        AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null) {
            if (starless) {
                maxHealth.addOrUpdateTransientModifier(new AttributeModifier(NO_STARS_HEALTH, -4.0D, AttributeModifier.Operation.ADD_VALUE));
                if (player.getHealth() > player.getMaxHealth()) {
                    player.setHealth(player.getMaxHealth());
                }
            } else {
                maxHealth.removeModifier(NO_STARS_HEALTH);
            }
        }

        AttributeInstance attackDamage = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackDamage != null) {
            if (starless) {
                attackDamage.addOrUpdateTransientModifier(new AttributeModifier(STAR_WEAKNESS_DAMAGE, -0.2D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            } else {
                attackDamage.removeModifier(STAR_WEAKNESS_DAMAGE);
            }
        }
    }

    private static Optional<Vec3> findStarStepTarget(Player player) {
        Vec3 eye = player.getEyePosition();
        Vec3 max = eye.add(player.getLookAngle().normalize().scale(STEP_MAX_DISTANCE));
        BlockHitResult hit = player.level().clip(new ClipContext(eye, max, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 sample = hit.getType() == HitResult.Type.MISS ? max : hit.getLocation().add(player.getLookAngle().scale(-0.2D));
        Vec3 origin = player.position();
        AABB box = player.getBoundingBox();
        List<StepCandidate> candidates = new ArrayList<>();

        BlockPos center = BlockPos.containing(sample);
        for (int dy = -2; dy <= 2; dy++) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    BlockPos floorPos = center.offset(dx, dy - 1, dz);
                    BlockState floorState = player.level().getBlockState(floorPos);
                    if (!floorState.isSolidRender()) {
                        continue;
                    }

                    Vec3 feet = new Vec3(floorPos.getX() + 0.5D, floorPos.getY() + 1.0D, floorPos.getZ() + 0.5D);
                    if (!canFitAt(player, origin, box, feet)) {
                        continue;
                    }

                    double closeness = -feet.distanceToSqr(sample);
                    double liftPenalty = -Math.abs(dy) * 2.0D - Math.abs(dx) * 0.5D - Math.abs(dz) * 0.5D;
                    candidates.add(new StepCandidate(feet, closeness + liftPenalty));
                }
            }
        }

        if (hit.getType() == HitResult.Type.MISS) {
            Vec3 feet = new Vec3(sample.x, sample.y - STEP_OPEN_EYE_OFFSET, sample.z);
            if (canFitAt(player, origin, box, feet)) {
                candidates.add(new StepCandidate(feet, 1.0D));
            }
        }

        return candidates.stream().max(Comparator.comparingDouble(StepCandidate::score)).map(StepCandidate::position);
    }

    private static EntityHitResult findTargetEntity(ServerPlayer player, double range) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(range));
        AABB search = player.getBoundingBox().expandTowards(player.getLookAngle().scale(range)).inflate(1.5D);
        return ProjectileUtil.getEntityHitResult(player, eye, end, search, entity -> entity instanceof LivingEntity && entity.isAlive() && entity != player, range * range);
    }

    private static boolean canFitAt(Player player, Vec3 origin, AABB box, Vec3 feet) {
        AABB moved = box.move(feet.x - origin.x, feet.y - origin.y, feet.z - origin.z);
        return player.level().noCollision(player, moved);
    }

    private static boolean hasClearStarSky(Player player) {
        Level level = player.level();
        if (level.dimension() != Level.OVERWORLD || !level.dimensionType().hasSkyLight()) {
            return false;
        }

        BlockPos pos = player.blockPosition().above();
        return level.getSkyDarken() >= 4 && level.canSeeSky(pos) && !level.isRainingAt(pos);
    }

    private static boolean isStarlessRealm(Level level) {
        return !level.dimensionType().hasSkyLight() || level.dimension() == Level.NETHER || level.dimension() == Level.END;
    }

    private static boolean hasAstralLanternNearby(net.minecraft.world.level.ServerLevelAccessor level, BlockPos center) {
        for (BlockPos pos : BlockPos.withinManhattan(center, MAX_LANTERN_RADIUS, LANTERN_VERTICAL_RANGE, MAX_LANTERN_RADIUS)) {
            BlockState state = level.getBlockState(pos);
            if (!state.is(MythosBlocks.ASTRAL_LANTERN.get())) {
                continue;
            }

            int radius = AstralLanternBlock.radius(state);
            if (Math.abs(center.getX() - pos.getX()) <= radius
                && Math.abs(center.getZ() - pos.getZ()) <= radius
                && Math.abs(center.getY() - pos.getY()) <= LANTERN_VERTICAL_RANGE) {
                return true;
            }
        }
        return false;
    }

    public static void showAstralLanternOutline(ServerLevel level, BlockPos center, int radius) {
        double y = center.getY() + 0.15D;
        double minX = center.getX() + 0.5D - radius;
        double maxX = center.getX() + 0.5D + radius;
        double minZ = center.getZ() + 0.5D - radius;
        double maxZ = center.getZ() + 0.5D + radius;

        for (int offset = -radius; offset <= radius; offset++) {
            double x = center.getX() + 0.5D + offset;
            double z = center.getZ() + 0.5D + offset;
            level.sendParticles(ParticleTypes.END_ROD, x, y, minZ, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            level.sendParticles(ParticleTypes.END_ROD, x, y, maxZ, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            level.sendParticles(ParticleTypes.END_ROD, minX, y, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            level.sendParticles(ParticleTypes.END_ROD, maxX, y, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    private static void spendEnergy(Player player, int amount) {
        player.setData(MythosAttachments.STAR_WANDERER_ENERGY, Math.max(0, player.getData(MythosAttachments.STAR_WANDERER_ENERGY) - amount));
    }

    private static void handleStarSmithing(Player player, SmithingMenu smithingMenu, boolean isStarWanderer) {
        ItemStack template = smithingMenu.getSlot(SmithingMenu.TEMPLATE_SLOT).getItem();
        ItemStack base = smithingMenu.getSlot(SmithingMenu.BASE_SLOT).getItem();
        ItemStack addition = smithingMenu.getSlot(SmithingMenu.ADDITIONAL_SLOT).getItem();
        if (!template.isEmpty()) {
            return;
        }

        Optional<ItemStack> result = Optional.empty();
        if (isStarAnchorInputs(base, addition)) {
            result = Optional.of(new ItemStack(MythosItems.STAR_ANCHOR.get()));
        } else if (isAstralLanternInputs(base, addition)) {
            result = Optional.of(new ItemStack(MythosItems.ASTRAL_LANTERN.get()));
        } else {
            return;
        }

        if (!isStarWanderer) {
            clearSmithingResult(smithingMenu);
            if (player.tickCount % 40 == 0) {
                player.sendSystemMessage(Component.translatable("message.mythos.star_wanderer_only_artifacts"));
            }
            return;
        }

        ItemStack current = smithingMenu.getSlot(SmithingMenu.RESULT_SLOT).getItem();
        if (!ItemStack.matches(current, result.get())) {
            smithingMenu.getSlot(SmithingMenu.RESULT_SLOT).set(result.get());
            smithingMenu.broadcastChanges();
        }
    }

    private static boolean isStarAnchorInputs(ItemStack base, ItemStack addition) {
        return base.is(Items.RECOVERY_COMPASS) && addition.is(Items.ENDER_PEARL);
    }

    private static boolean isAstralLanternInputs(ItemStack base, ItemStack addition) {
        return base.is(Items.LANTERN) && addition.is(Items.AMETHYST_SHARD);
    }

    private static void clearSmithingResult(SmithingMenu smithingMenu) {
        if (!smithingMenu.getSlot(SmithingMenu.RESULT_SLOT).getItem().isEmpty()) {
            smithingMenu.getSlot(SmithingMenu.RESULT_SLOT).set(ItemStack.EMPTY);
            smithingMenu.broadcastChanges();
        }
    }

    private static void clearStarState(Player player) {
        player.setData(MythosAttachments.STAR_WANDERER_BEAM_ACTIVE, false);
        player.removeEffect(MythosEffects.STAR_WANDERER_ASCENT);
        player.removeEffect(MobEffects.REGENERATION);
        player.removeEffect(MobEffects.SLOWNESS);
        player.removeEffect(MobEffects.WEAKNESS);
        syncStarlessHealth(player, false);
    }

    private static void ensureEffect(Player player, Holder<net.minecraft.world.effect.MobEffect> effect, int duration, int amplifier) {
        if (!player.hasEffect(effect)) {
            player.addEffect(new MobEffectInstance(effect, duration, amplifier, false, true, true));
            return;
        }

        var current = player.getEffect(effect);
        if (current != null && (current.getAmplifier() != amplifier || current.getDuration() < duration / 2)) {
            player.addEffect(new MobEffectInstance(effect, duration, amplifier, false, true, true));
        }
    }

    private record StepCandidate(Vec3 position, double score) {
    }
}
