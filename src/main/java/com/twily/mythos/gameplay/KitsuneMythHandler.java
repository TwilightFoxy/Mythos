package com.twily.mythos.gameplay;

import com.twily.mythos.Mythos;
import com.twily.mythos.data.MythItemMarkerHelper;
import com.twily.mythos.myth.MythState;
import com.twily.mythos.registry.MythosAttachments;
import com.twily.mythos.registry.MythosEffects;
import com.twily.mythos.registry.MythosItems;
import com.twily.mythos.world.entity.KitsuneFoxfireEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Optional;

@EventBusSubscriber(modid = Mythos.MOD_ID)
public final class KitsuneMythHandler {

    private static final Identifier KITSUNE = Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "kitsune");
    private static final Identifier KITSUNE_ARMOR = Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "kitsune_armor");
    private static final Identifier KITSUNE_ARMOR_TOUGHNESS = Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "kitsune_armor_toughness");
    private static final float KITSUNE_MELEE_DAMAGE_MULTIPLIER = 0.7F;
    private static final double KITSUNE_DEFENSE_REDUCTION = -0.3D;
    private static final int MASK_DURATION_TICKS = 220;
    private static final int INVISIBILITY_DURATION_TICKS = 40;
    private static final int DASH_COOLDOWN_TICKS = 20 * 4;
    private static final int DASH_IMMUNITY_TICKS = 20;
    private static final int FOXFIRE_COOLDOWN_TICKS = 20 * 4;
    private static final int WRATH_COOLDOWN_TICKS = 20 * 12;
    private KitsuneMythHandler() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        boolean isKitsune = MythState.matches(player, KITSUNE);

        if (!player.level().isClientSide()) {
            tickCooldown(player, MythosAttachments.KITSUNE_DASH_COOLDOWN.get());
            tickCooldown(player, MythosAttachments.KITSUNE_DASH_IMMUNITY.get());
            tickCooldown(player, MythosAttachments.KITSUNE_FOXFIRE_COOLDOWN.get());
            tickCooldown(player, MythosAttachments.KITSUNE_WRATH_COOLDOWN.get());
            syncCooldownEffects(player, isKitsune);
            syncKitsuneDefense(player, isKitsune);
            if (player.containerMenu instanceof SmithingMenu smithingMenu) {
                handleKitsuneSmithing(player, smithingMenu, isKitsune);
            }
        }

        if (!isKitsune) {
            if (player.getData(MythosAttachments.KITSUNE_MASKED)) {
                player.setData(MythosAttachments.KITSUNE_MASKED, false);
            }
            return;
        }

        if (player.getData(MythosAttachments.KITSUNE_MASKED) && canUseMaskPowers(player.level(), player)) {
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, MASK_DURATION_TICKS, 0, false, false, true));
            if (player.isCrouching()) {
                player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, INVISIBILITY_DURATION_TICKS, 0, false, true, true));
            }
        }
    }

    private static void syncCooldownEffects(Player player, boolean isKitsune) {
        syncCooldownEffect(player, MythosEffects.KITSUNE_DASH_COOLDOWN, MythosAttachments.KITSUNE_DASH_COOLDOWN.get(), isKitsune);
        syncCooldownEffect(player, MythosEffects.KITSUNE_FOXFIRE_COOLDOWN, MythosAttachments.KITSUNE_FOXFIRE_COOLDOWN.get(), isKitsune);
        syncCooldownEffect(player, MythosEffects.KITSUNE_WRATH_COOLDOWN, MythosAttachments.KITSUNE_WRATH_COOLDOWN.get(), isKitsune);
    }

    private static void syncCooldownEffect(Player player, net.neoforged.neoforge.registries.DeferredHolder<net.minecraft.world.effect.MobEffect, ? extends net.minecraft.world.effect.MobEffect> effect, net.neoforged.neoforge.attachment.AttachmentType<Integer> attachment, boolean enabled) {
        if (!enabled) {
            player.removeEffect(effect);
            return;
        }

        int cooldown = player.getData(attachment);
        if (cooldown > 0) {
            player.addEffect(new MobEffectInstance(effect, cooldown, 0, false, false, true));
        } else {
            player.removeEffect(effect);
        }
    }

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Pre event) {
        if (event.getEntity() instanceof Player playerWithImmunity
            && playerWithImmunity.getData(MythosAttachments.KITSUNE_DASH_IMMUNITY) > 0
            && (event.getSource().is(net.minecraft.tags.DamageTypeTags.IS_FALL) || event.getSource().is(DamageTypes.FLY_INTO_WALL))) {
            event.setNewDamage(0.0F);
            return;
        }

        if (!(event.getSource().getEntity() instanceof Player player) || !MythState.matches(player, KITSUNE)) {
            return;
        }

        if (event.getSource().getDirectEntity() == player) {
            event.setNewDamage(event.getNewDamage() * KITSUNE_MELEE_DAMAGE_MULTIPLIER);
            if (player instanceof ServerPlayer serverPlayer
                && player.getData(MythosAttachments.KITSUNE_MASKED)
                && canUseMaskPowers(player.level(), player)
                && event.getEntity() instanceof LivingEntity target) {
                triggerHeavenlyWrath(serverPlayer, target);
            }
        }
    }

    public static void toggleMask(ServerPlayer player) {
        if (!MythState.matches(player, KITSUNE)) {
            return;
        }

        boolean newState = !player.getData(MythosAttachments.KITSUNE_MASKED);
        player.setData(MythosAttachments.KITSUNE_MASKED, newState);
        if (newState && !canUseMaskPowers(player.level(), player)) {
            player.sendSystemMessage(Component.translatable("message.mythos.kitsune_mask_day_visual"));
        }
    }

    public static void performDash(ServerPlayer player) {
        if (!MythState.matches(player, KITSUNE)) {
            return;
        }

        int cooldown = player.getData(MythosAttachments.KITSUNE_DASH_COOLDOWN);
        if (cooldown > 0) {
            player.sendSystemMessage(Component.translatable("message.mythos.kitsune_dash_cooldown", Math.max(1, cooldown / 20)));
            return;
        }

        Vec3 look = player.getLookAngle().normalize().scale(1.1D);
        player.push(look.x, look.y, look.z);
        player.hurtMarked = true;
        player.resetFallDistance();
        player.setData(MythosAttachments.KITSUNE_DASH_COOLDOWN, DASH_COOLDOWN_TICKS);
        player.setData(MythosAttachments.KITSUNE_DASH_IMMUNITY, DASH_IMMUNITY_TICKS);
    }

    public static void castFoxfire(ServerPlayer player) {
        if (!MythState.matches(player, KITSUNE)
            || !player.getData(MythosAttachments.KITSUNE_MASKED)
            || !canUseMaskPowers(player.level(), player)) {
            return;
        }

        int cooldown = player.getData(MythosAttachments.KITSUNE_FOXFIRE_COOLDOWN);
        if (cooldown > 0) {
            player.sendSystemMessage(Component.translatable("message.mythos.kitsune_foxfire_cooldown", Math.max(1, cooldown / 20)));
            return;
        }

        Vec3 look = player.getLookAngle();
        KitsuneFoxfireEntity fireball = new KitsuneFoxfireEntity(
            player.level(),
            player.getX() + look.x * 0.8D,
            player.getEyeY() - 0.1D,
            player.getZ() + look.z * 0.8D,
            look
        );
        fireball.setOwner(player);
        player.level().addFreshEntity(fireball);
        player.setData(MythosAttachments.KITSUNE_FOXFIRE_COOLDOWN, FOXFIRE_COOLDOWN_TICKS);
    }

    private static void triggerHeavenlyWrath(ServerPlayer player, LivingEntity target) {
        int cooldown = player.getData(MythosAttachments.KITSUNE_WRATH_COOLDOWN);
        if (cooldown > 0 || !(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        LightningBolt lightning = net.minecraft.world.entity.EntityType.LIGHTNING_BOLT.create(serverLevel, EntitySpawnReason.TRIGGERED);
        if (lightning == null) {
            return;
        }

        lightning.setPos(target.getX(), target.getY(), target.getZ());
        lightning.setCause(player);
        serverLevel.addFreshEntity(lightning);
        player.setData(MythosAttachments.KITSUNE_WRATH_COOLDOWN, WRATH_COOLDOWN_TICKS);
    }

    private static void syncKitsuneDefense(Player player, boolean isKitsune) {
        AttributeInstance armor = player.getAttribute(Attributes.ARMOR);
        if (armor != null) {
            if (isKitsune) {
                armor.addOrUpdateTransientModifier(new AttributeModifier(KITSUNE_ARMOR, KITSUNE_DEFENSE_REDUCTION, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            } else {
                armor.removeModifier(KITSUNE_ARMOR);
            }
        }

        AttributeInstance armorToughness = player.getAttribute(Attributes.ARMOR_TOUGHNESS);
        if (armorToughness != null) {
            if (isKitsune) {
                armorToughness.addOrUpdateTransientModifier(new AttributeModifier(KITSUNE_ARMOR_TOUGHNESS, KITSUNE_DEFENSE_REDUCTION, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            } else {
                armorToughness.removeModifier(KITSUNE_ARMOR_TOUGHNESS);
            }
        }
    }

    private static void handleKitsuneSmithing(Player player, SmithingMenu smithingMenu, boolean isKitsune) {
        ItemStack template = smithingMenu.getSlot(SmithingMenu.TEMPLATE_SLOT).getItem();
        ItemStack base = smithingMenu.getSlot(SmithingMenu.BASE_SLOT).getItem();
        ItemStack addition = smithingMenu.getSlot(SmithingMenu.ADDITIONAL_SLOT).getItem();
        if (!template.isEmpty()) {
            return;
        }

        Optional<ItemStack> result = Optional.empty();
        if (isFoxLanternInputs(base, addition)) {
            result = createFoxLantern(base);
        } else {
            return;
        }

        if (!isKitsune) {
            clearSmithingResult(smithingMenu);
            if (result.isPresent() && player.tickCount % 40 == 0) {
                player.sendSystemMessage(Component.translatable("message.mythos.kitsune_only_artifacts"));
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

    private static boolean isFoxLanternInputs(ItemStack base, ItemStack addition) {
        return base.is(Items.LANTERN) && addition.is(Items.GLOW_INK_SAC);
    }

    private static Optional<ItemStack> createFoxLantern(ItemStack base) {
        if (base.is(MythosItems.FOX_LANTERN.asItem())) {
            return Optional.empty();
        }
        return Optional.of(new ItemStack(MythosItems.FOX_LANTERN.asItem()));
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

    public static boolean canUseMaskPowers(Level level, Player player) {
        if (level == null || !level.dimensionType().hasSkyLight()) {
            return true;
        }

        return level.getSkyDarken() >= 8;
    }
}
