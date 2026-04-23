package com.twily.mythos.gameplay;

import com.twily.mythos.Mythos;
import com.twily.mythos.myth.MythState;
import com.twily.mythos.registry.MythosAttachments;
import com.twily.mythos.registry.MythosEffects;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public final class SlimeMythHandler {

    private static final Identifier SLIME = Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "slime");
    private static final Identifier SLIME_SCALE = Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "slime_stage_scale");
    private static final Identifier SLIME_HEALTH = Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "slime_stage_health");
    private static final Identifier SLIME_SPEED = Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "slime_stage_speed");
    private static final Identifier SLIME_ATTACK = Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "slime_stage_attack");
    private static final Identifier SLIME_ARMOR = Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "slime_stage_armor");
    private static final Identifier SLIME_KNOCKBACK = Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "slime_stage_knockback");
    private static final int STAGE_EFFECT_DURATION_TICKS = 20 * 30;
    private static final int STAGE_EFFECT_REFRESH_THRESHOLD_TICKS = 20 * 15;

    private static final int STAGE_SMALL = 0;
    private static final int STAGE_MEDIUM = 1;
    private static final int STAGE_LARGE = 2;
    private static final int STAGE_HUGE = 3;

    // Player base height is 1.8 blocks, so these modifiers target 1.0 / 1.5 / 2.0 / 3.0 block forms.
    private static final double[] SCALE_BY_STAGE = {
        -0.4444444444D,
        -0.1666666667D,
        0.1111111111D,
        0.6666666667D
    };
    private static final double[] HEALTH_BY_STAGE = {-4.0D, 0.0D, 4.0D, 8.0D};
    private static final double[] SPEED_BY_STAGE = {0.35D, 0.10D, -0.10D, -0.20D};
    private static final double[] ATTACK_BY_STAGE = {-0.25D, 0.0D, 0.30D, 0.45D};
    private static final double[] ARMOR_BY_STAGE = {-4.0D, 2.0D, 4.0D, 8.0D};
    private static final double[] KNOCKBACK_BY_STAGE = {0.0D, 0.0D, 0.0D, 0.80D};

    private SlimeMythHandler() {
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Player player)) {
            return;
        }

        if (!MythState.is(player, SLIME)) {
            clearSlimeState(player);
            return;
        }

        applySlimeStage(player, determineStage(player));
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();

        if (player.hasEffect(MythosEffects.SLIME_CLINGING)) {
            tickClinging(player);
        }

        if (player.level().isClientSide()) {
            return;
        }

        if (!MythState.is(player, SLIME)) {
            clearSlimeState(player);
            return;
        }

        int nextStage = determineStage(player);
        int currentStage = player.getData(MythosAttachments.SLIME_STAGE);
        if (nextStage != currentStage) {
            applySlimeStage(player, nextStage);
            if (currentStage >= 0 && player instanceof ServerPlayer serverPlayer) {
                serverPlayer.sendSystemMessage(Component.translatable(stageMessageKey(nextStage)), true);
            }
        } else {
            ensureStageDisplayEffect(player, nextStage);
        }
    }

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Pre event) {
        if (event.getEntity() instanceof Player player && MythState.is(player, SLIME) && event.getSource().is(DamageTypeTags.IS_FALL)) {
            event.setNewDamage(0.0F);
        }
    }

    private static void tickClinging(Player player) {
        if (!player.horizontalCollision) {
            return;
        }

        player.resetFallDistance();
        if (player.isShiftKeyDown()) {
            player.setDeltaMovement(player.getDeltaMovement().x * 0.85D, 0.0D, player.getDeltaMovement().z * 0.85D);
            return;
        }

        if (player.zza > 0.0F) {
            double upward = Math.max(player.getDeltaMovement().y, 0.13D);
            player.setDeltaMovement(player.getDeltaMovement().x, upward, player.getDeltaMovement().z);
        } else if (player.getDeltaMovement().y < -0.05D) {
            player.setDeltaMovement(player.getDeltaMovement().x, -0.05D, player.getDeltaMovement().z);
        }
    }

    private static int determineStage(Player player) {
        int occupiedSlots = 0;
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (!stack.isEmpty()) {
                occupiedSlots++;
            }
        }

        if (occupiedSlots <= 9) {
            return STAGE_SMALL;
        }
        if (occupiedSlots <= 18) {
            return STAGE_MEDIUM;
        }
        if (occupiedSlots <= 27) {
            return STAGE_LARGE;
        }
        return STAGE_HUGE;
    }

    private static void applySlimeStage(Player player, int stage) {
        player.setData(MythosAttachments.SLIME_STAGE, stage);
        syncStageModifier(player, Attributes.SCALE, SLIME_SCALE, SCALE_BY_STAGE[stage], AttributeModifier.Operation.ADD_VALUE);
        syncStageModifier(player, Attributes.MAX_HEALTH, SLIME_HEALTH, HEALTH_BY_STAGE[stage], AttributeModifier.Operation.ADD_VALUE);
        syncStageModifier(player, Attributes.MOVEMENT_SPEED, SLIME_SPEED, SPEED_BY_STAGE[stage], AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        syncStageModifier(player, Attributes.ATTACK_DAMAGE, SLIME_ATTACK, ATTACK_BY_STAGE[stage], AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        syncStageModifier(player, Attributes.ARMOR, SLIME_ARMOR, ARMOR_BY_STAGE[stage], AttributeModifier.Operation.ADD_VALUE);
        syncStageModifier(player, Attributes.KNOCKBACK_RESISTANCE, SLIME_KNOCKBACK, KNOCKBACK_BY_STAGE[stage], AttributeModifier.Operation.ADD_VALUE);
        ensureStageDisplayEffect(player, stage);

        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    private static void syncStageModifier(Player player, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute, Identifier id, double amount, AttributeModifier.Operation operation) {
        MythStatusHelper.syncModifier(player, attribute, id, amount, operation, amount != 0.0D);
    }

    private static void clearSlimeState(Player player) {
        if (player.getData(MythosAttachments.SLIME_STAGE) < 0) {
            return;
        }

        player.setData(MythosAttachments.SLIME_STAGE, -1);
        MythStatusHelper.syncModifier(player, Attributes.SCALE, SLIME_SCALE, 0.0D, AttributeModifier.Operation.ADD_VALUE, false);
        MythStatusHelper.syncModifier(player, Attributes.MAX_HEALTH, SLIME_HEALTH, 0.0D, AttributeModifier.Operation.ADD_VALUE, false);
        MythStatusHelper.syncModifier(player, Attributes.MOVEMENT_SPEED, SLIME_SPEED, 0.0D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, false);
        MythStatusHelper.syncModifier(player, Attributes.ATTACK_DAMAGE, SLIME_ATTACK, 0.0D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, false);
        MythStatusHelper.syncModifier(player, Attributes.ARMOR, SLIME_ARMOR, 0.0D, AttributeModifier.Operation.ADD_VALUE, false);
        MythStatusHelper.syncModifier(player, Attributes.KNOCKBACK_RESISTANCE, SLIME_KNOCKBACK, 0.0D, AttributeModifier.Operation.ADD_VALUE, false);
        clearStageDisplayEffects(player);

        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    private static void ensureStageDisplayEffect(Player player, int stage) {
        Holder<net.minecraft.world.effect.MobEffect> currentEffect = stageDisplayEffect(stage);
        MobEffectInstance currentInstance = player.getEffect(currentEffect);
        if (!hasOnlyCurrentStageEffect(player, stage) || currentInstance == null || currentInstance.getDuration() <= STAGE_EFFECT_REFRESH_THRESHOLD_TICKS) {
            clearStageDisplayEffects(player);
            player.addEffect(new MobEffectInstance(currentEffect, STAGE_EFFECT_DURATION_TICKS, 0, false, false, true));
        }
    }

    private static boolean hasOnlyCurrentStageEffect(Player player, int stage) {
        MobEffectInstance small = player.getEffect(MythosEffects.SLIME_MASS_SMALL);
        MobEffectInstance medium = player.getEffect(MythosEffects.SLIME_MASS_MEDIUM);
        MobEffectInstance large = player.getEffect(MythosEffects.SLIME_MASS_LARGE);
        MobEffectInstance huge = player.getEffect(MythosEffects.SLIME_MASS_HUGE);
        return switch (stage) {
            case STAGE_SMALL -> small != null && medium == null && large == null && huge == null;
            case STAGE_MEDIUM -> small == null && medium != null && large == null && huge == null;
            case STAGE_LARGE -> small == null && medium == null && large != null && huge == null;
            default -> small == null && medium == null && large == null && huge != null;
        };
    }

    private static void clearStageDisplayEffects(Player player) {
        player.removeEffect(MythosEffects.SLIME_MASS_SMALL);
        player.removeEffect(MythosEffects.SLIME_MASS_MEDIUM);
        player.removeEffect(MythosEffects.SLIME_MASS_LARGE);
        player.removeEffect(MythosEffects.SLIME_MASS_HUGE);
    }

    private static Holder<net.minecraft.world.effect.MobEffect> stageDisplayEffect(int stage) {
        return switch (stage) {
            case STAGE_SMALL -> MythosEffects.SLIME_MASS_SMALL;
            case STAGE_MEDIUM -> MythosEffects.SLIME_MASS_MEDIUM;
            case STAGE_LARGE -> MythosEffects.SLIME_MASS_LARGE;
            default -> MythosEffects.SLIME_MASS_HUGE;
        };
    }

    private static String stageMessageKey(int stage) {
        return switch (stage) {
            case STAGE_SMALL -> "message.mythos.slime_stage_small";
            case STAGE_MEDIUM -> "message.mythos.slime_stage_medium";
            case STAGE_LARGE -> "message.mythos.slime_stage_large";
            default -> "message.mythos.slime_stage_huge";
        };
    }
}
