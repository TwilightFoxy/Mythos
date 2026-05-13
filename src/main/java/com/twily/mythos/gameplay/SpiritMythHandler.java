package com.twily.mythos.gameplay;

import com.twily.mythos.Mythos;
import com.twily.mythos.myth.MythState;
import com.twily.mythos.registry.MythosAttachments;
import com.twily.mythos.registry.MythosBlocks;
import com.twily.mythos.registry.MythosEffects;
import com.twily.mythos.registry.MythosItems;
import com.twily.mythos.world.block.SpiritStepBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@EventBusSubscriber(modid = Mythos.MOD_ID)
public final class SpiritMythHandler {

    public static final Identifier SPIRIT_ID = Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "spirit");
    private static final Identifier SPIRIT_ARMOR = Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "spirit_armor");
    private static final Identifier SPIRIT_ARMOR_TOUGHNESS = Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "spirit_armor_toughness");

    private static final double DAY_DEFENSE_REDUCTION = -0.3D;
    private static final float NIGHT_DAMAGE_MULTIPLIER = 1.3F;
    private static final int PHASE_COOLDOWN_TICKS = 20 * 4;
    private static final int PHASE_DARKNESS_TICKS = 18;
    private static final int PHASE_FALL_IMMUNITY_TICKS = 40;
    private static final double PHASE_MAX_DISTANCE = 12.0D;
    private static final double PHASE_STEP = 0.5D;
    private static final double MIN_PHASE_DISTANCE = 1.5D;
    private static final double OPEN_CANDIDATE_EYE_OFFSET = 1.62D;
    private static final int SPIRIT_STEPS_DURATION_TICKS = 20 * 150;

    private SpiritMythHandler() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        boolean isSpirit = MythState.matches(player, SPIRIT_ID);

        if (!player.level().isClientSide()) {
            tickSpiritSteps(player);
            tickCooldown(player, MythosAttachments.SPIRIT_PHASE_COOLDOWN.get());
            tickCooldown(player, MythosAttachments.SPIRIT_PHASE_FALL_IMMUNITY.get());
            syncSpiritDefense(player, isSpirit);
            if (player.containerMenu instanceof SmithingMenu smithingMenu) {
                handleSpiritSmithing(player, smithingMenu, isSpirit);
            }
        }
    }

    @SubscribeEvent
    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        if (player.getData(MythosAttachments.SPIRIT_PHASE_FALL_IMMUNITY) > 0
            && (event.getSource().is(DamageTypeTags.IS_FALL)
            || event.getSource().is(net.minecraft.world.damagesource.DamageTypes.FLY_INTO_WALL)
            || event.getSource().is(net.minecraft.world.damagesource.DamageTypes.IN_WALL))) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Pre event) {
        if (!(event.getSource().getEntity() instanceof Player player) || !MythState.matches(player, SPIRIT_ID)) {
            return;
        }

        if (canUseNightStrength(player.level())) {
            event.setNewDamage(event.getNewDamage() * NIGHT_DAMAGE_MULTIPLIER);
        }
    }

    public static void performPhaseTransition(ServerPlayer player) {
        if (!MythState.matches(player, SPIRIT_ID) || player.isPassenger()) {
            return;
        }

        int cooldown = player.getData(MythosAttachments.SPIRIT_PHASE_COOLDOWN);
        if (cooldown > 0) {
            player.sendSystemMessage(Component.translatable("message.mythos.spirit_phase_cooldown", Math.max(1, cooldown / 20)));
            return;
        }

        Optional<Vec3> target = findPhaseTarget(player);
        if (target.isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.mythos.spirit_phase_blocked"));
            return;
        }

        Vec3 destination = target.get();
        if (destination.distanceTo(player.position()) < MIN_PHASE_DISTANCE) {
            player.sendSystemMessage(Component.translatable("message.mythos.spirit_phase_blocked"));
            return;
        }

        player.setData(MythosAttachments.SPIRIT_PHASE_COOLDOWN, PHASE_COOLDOWN_TICKS);
        player.setData(MythosAttachments.SPIRIT_PHASE_FALL_IMMUNITY, PHASE_FALL_IMMUNITY_TICKS);
        player.resetFallDistance();
        player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, PHASE_DARKNESS_TICKS, 0, false, false, false));
        player.setPos(destination.x, destination.y, destination.z);
        player.connection.teleport(destination.x, destination.y, destination.z, player.getYRot(), player.getXRot());
        player.level().playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, player.getSoundSource(), 0.75F, 1.1F);
    }

    public static void activateEtherealSteps(Player player) {
        player.setData(MythosAttachments.SPIRIT_STEPS_TICKS, SPIRIT_STEPS_DURATION_TICKS);
    }

    public static Optional<Vec3> previewPhaseTarget(Player player) {
        return findPhaseTarget(player);
    }

    public static boolean isDaytimeSpirit(Level level) {
        return !canUseNightStrength(level);
    }

    public static boolean canUseNightStrength(Level level) {
        if (level == null || !level.dimensionType().hasSkyLight()) {
            return true;
        }

        return level.getSkyDarken() >= 8;
    }

    private static Optional<Vec3> findPhaseTarget(Player player) {
        Vec3 eye = player.getEyePosition();
        Vec3 direction = player.getLookAngle().normalize();
        if (direction.lengthSqr() < 1.0E-6D) {
            return Optional.empty();
        }

        Vec3 origin = player.position();
        AABB box = player.getBoundingBox();
        List<PhaseCandidate> candidates = new ArrayList<>();

        for (double distance = PHASE_STEP; distance <= PHASE_MAX_DISTANCE; distance += PHASE_STEP) {
            Vec3 sample = eye.add(direction.scale(distance));
            collectStandingCandidates(player, origin, box, sample, distance, candidates);
            collectOpenCandidate(player, origin, box, sample, distance, candidates);
        }

        return candidates.stream()
            .max(Comparator.comparingDouble(PhaseCandidate::score))
            .map(PhaseCandidate::position);
    }

    private static void collectStandingCandidates(Player player, Vec3 origin, AABB box, Vec3 sample, double distance, List<PhaseCandidate> out) {
        BlockPos samplePos = BlockPos.containing(sample);
        int[] horizontal = {0, 1, -1, 2, -2};
        int[] vertical = {0, -1, 1, -2, 2};

        for (int dy : vertical) {
            for (int dx : horizontal) {
                for (int dz : horizontal) {
                    BlockPos floorPos = samplePos.offset(dx, dy - 1, dz);
                    BlockState floorState = player.level().getBlockState(floorPos);
                    if (!floorState.isSolidRender()) {
                        continue;
                    }

                    Vec3 feet = new Vec3(floorPos.getX() + 0.5D, floorPos.getY() + 1.0D, floorPos.getZ() + 0.5D);
                    if (!canFitAt(player, origin, box, feet)) {
                        continue;
                    }

                    double offsetPenalty = Math.abs(dx) * 2.5D + Math.abs(dz) * 2.5D + Math.abs(dy) * 3.0D;
                    double floorBonus = 25.0D;
                    out.add(new PhaseCandidate(feet, distance * 100.0D + floorBonus - offsetPenalty));
                }
            }
        }
    }

    private static void collectOpenCandidate(Player player, Vec3 origin, AABB box, Vec3 sample, double distance, List<PhaseCandidate> out) {
        Vec3 feet = new Vec3(sample.x, sample.y - OPEN_CANDIDATE_EYE_OFFSET, sample.z);
        if (!canFitAt(player, origin, box, feet)) {
            return;
        }

        out.add(new PhaseCandidate(feet, distance * 100.0D - 20.0D));
    }

    private static boolean canFitAt(Player player, Vec3 origin, AABB box, Vec3 feet) {
        AABB moved = box.move(feet.x - origin.x, feet.y - origin.y, feet.z - origin.z);
        return player.level().noCollision(player, moved);
    }

    private static void tickSpiritSteps(Player player) {
        int ticks = player.getData(MythosAttachments.SPIRIT_STEPS_TICKS);
        if (!(player.level() instanceof ServerLevel level) || player.isSpectator()) {
            player.removeEffect(MythosEffects.SPIRIT_ETHEREAL_STEPS);
            return;
        }

        if (ticks <= 0) {
            player.removeEffect(MythosEffects.SPIRIT_ETHEREAL_STEPS);
            return;
        }

        player.setData(MythosAttachments.SPIRIT_STEPS_TICKS, ticks - 1);
        syncDisplayEffectDuration(player, MythosEffects.SPIRIT_ETHEREAL_STEPS, ticks);

        if (player.isShiftKeyDown()) {
            removeSpiritStep(level, stepPosition(player));
            return;
        }

        BlockState stepState = MythosBlocks.SPIRIT_STEP.get().defaultBlockState();
        BlockPos pos = stepPosition(player);
        BlockState current = level.getBlockState(pos);
        if (current.is(MythosBlocks.SPIRIT_STEP.get())) {
            SpiritStepBlock.refreshLifetime(level, pos, MythosBlocks.SPIRIT_STEP.get());
        } else if ((current.isAir() || current.canBeReplaced()) && !level.getBlockState(pos.above()).isSolidRender()) {
            level.setBlock(pos, stepState, 3);
            SpiritStepBlock.refreshLifetime(level, pos, MythosBlocks.SPIRIT_STEP.get());
        }
    }

    private static BlockPos stepPosition(Player player) {
        AABB box = player.getBoundingBox().deflate(0.001D, 0.0D, 0.001D);
        double y = box.minY - 0.51D;
        return BlockPos.containing(player.getX(), y, player.getZ());
    }

    private static void removeSpiritStep(ServerLevel level, BlockPos pos) {
        if (level.getBlockState(pos).is(MythosBlocks.SPIRIT_STEP.get())) {
            level.removeBlock(pos, false);
        }
    }

    private static void syncSpiritDefense(Player player, boolean isSpirit) {
        boolean weakenedByDay = isSpirit && isDaytimeSpirit(player.level());

        AttributeInstance armor = player.getAttribute(Attributes.ARMOR);
        if (armor != null) {
            if (weakenedByDay) {
                armor.addOrUpdateTransientModifier(new AttributeModifier(SPIRIT_ARMOR, DAY_DEFENSE_REDUCTION, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            } else {
                armor.removeModifier(SPIRIT_ARMOR);
            }
        }

        AttributeInstance armorToughness = player.getAttribute(Attributes.ARMOR_TOUGHNESS);
        if (armorToughness != null) {
            if (weakenedByDay) {
                armorToughness.addOrUpdateTransientModifier(new AttributeModifier(SPIRIT_ARMOR_TOUGHNESS, DAY_DEFENSE_REDUCTION, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            } else {
                armorToughness.removeModifier(SPIRIT_ARMOR_TOUGHNESS);
            }
        }
    }

    private static void handleSpiritSmithing(Player player, SmithingMenu smithingMenu, boolean isSpirit) {
        ItemStack template = smithingMenu.getSlot(SmithingMenu.TEMPLATE_SLOT).getItem();
        ItemStack base = smithingMenu.getSlot(SmithingMenu.BASE_SLOT).getItem();
        ItemStack addition = smithingMenu.getSlot(SmithingMenu.ADDITIONAL_SLOT).getItem();
        if (!template.isEmpty()) {
            return;
        }

        Optional<ItemStack> result = Optional.empty();
        if (isEtherealCandleInputs(base, addition)) {
            result = Optional.of(new ItemStack(MythosItems.ETHEREAL_CANDLE.get()));
        } else {
            return;
        }

        if (!isSpirit) {
            clearSmithingResult(smithingMenu);
            if (player.tickCount % 40 == 0) {
                player.sendSystemMessage(Component.translatable("message.mythos.spirit_only_artifacts"));
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

    private static boolean isEtherealCandleInputs(ItemStack base, ItemStack addition) {
        return base.is(Items.SOUL_LANTERN) && addition.is(Items.PHANTOM_MEMBRANE);
    }

    private static void clearSmithingResult(SmithingMenu smithingMenu) {
        if (!smithingMenu.getSlot(SmithingMenu.RESULT_SLOT).getItem().isEmpty()) {
            smithingMenu.getSlot(SmithingMenu.RESULT_SLOT).set(ItemStack.EMPTY);
            smithingMenu.broadcastChanges();
        }
    }

    private static void tickCooldown(Player player, net.neoforged.neoforge.attachment.AttachmentType<Integer> type) {
        int cooldown = player.getData(type);
        if (cooldown > 0) {
            player.setData(type, cooldown - 1);
        }
    }

    private static void syncDisplayEffectDuration(Player player, Holder<net.minecraft.world.effect.MobEffect> effect, int duration) {
        MobEffectInstance current = player.getEffect(effect);
        if (current == null || Math.abs(current.getDuration() - duration) > 10) {
            player.addEffect(new MobEffectInstance(effect, duration, 0, false, true, true));
        }
    }

    private record PhaseCandidate(Vec3 position, double score) {
    }
}
