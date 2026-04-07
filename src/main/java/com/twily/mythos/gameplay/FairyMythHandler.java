package com.twily.mythos.gameplay;

import com.twily.mythos.Mythos;
import com.twily.mythos.myth.MythState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = Mythos.MOD_ID)
public final class FairyMythHandler {

    private static final Identifier FAIRY = Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "fairy");
    private static final Identifier FAIRY_HEALTH = Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "fairy_health");
    private static final double FAIRY_HEALTH_REDUCTION = -0.5D;
    private static final float FAIRY_MELEE_DAMAGE_MULTIPLIER = 0.6F;
    private static final int FAIRY_FLIGHT_RANGE = 5;

    private FairyMythHandler() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        boolean isFairy = MythState.is(player, FAIRY);
        boolean lowFlightAvailable = isFairy && hasSupportBelow(player, FAIRY_FLIGHT_RANGE);

        if (!player.level().isClientSide()) {
            syncFairyHealth(player, isFairy);
            syncFairyFlight(player, lowFlightAvailable);
        }

        if (isFairy && !lowFlightAvailable && !player.onGround() && !player.isInWater()) {
            applyFairyGlide(player);
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
        if (player.level().isClientSide() || !MythState.is(player, FAIRY)) {
            return;
        }

        ItemStack stack = player.getItemInHand(event.getHand());
        if (!stack.is(Items.FIREWORK_ROCKET) || hasSupportBelow(player, FAIRY_FLIGHT_RANGE) || player.onGround() || player.isInWater()) {
            return;
        }

        ServerLevel level = (ServerLevel) player.level();
        Vec3 lookAngle = player.getLookAngle();
        Vec3 movement = player.getDeltaMovement();
        player.setDeltaMovement(
            movement.add(
                lookAngle.x * 0.1 + (lookAngle.x * 1.5 - movement.x) * 0.5,
                lookAngle.y * 0.1 + (lookAngle.y * 1.5 - movement.y) * 0.5,
                lookAngle.z * 0.1 + (lookAngle.z * 1.5 - movement.z) * 0.5
            )
        );
        player.resetFallDistance();
        Projectile.spawnProjectile(new FireworkRocketEntity(level, stack.copyWithCount(1), player), level, stack);
        stack.consume(1, player);
        player.awardStat(Stats.ITEM_USED.get(Items.FIREWORK_ROCKET));
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
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

    private static void syncFairyFlight(Player player, boolean allowLowFlight) {
        boolean shouldMayFly = allowLowFlight || player.isSpectator() || player.hasInfiniteMaterials();
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

    private static void applyFairyGlide(Player player) {
        Vec3 movement = player.getDeltaMovement();
        Vec3 lookAngle = player.getLookAngle();
        double horizontalBoost = 0.025D;
        double verticalSpeed = movement.y < -0.08D ? movement.y * 0.6D : movement.y;
        Vec3 adjusted = new Vec3(
            movement.x + lookAngle.x * horizontalBoost,
            Math.max(verticalSpeed, -0.18D),
            movement.z + lookAngle.z * horizontalBoost
        );
        player.setDeltaMovement(adjusted);
        player.resetFallDistance();
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
