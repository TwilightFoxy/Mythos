package com.twily.mythos.gameplay;

import com.twily.mythos.Mythos;
import com.twily.mythos.myth.MythState;
import com.twily.mythos.registry.MythosEffects;
import com.twily.mythos.registry.MythosItems;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = Mythos.MOD_ID)
public final class ShulkerbornMythHandler {

    private static final Identifier SHULKERBORN = Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "shulkerborn");
    private static final Identifier SHULKERBORN_OVERLOAD_SPEED = Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "shulkerborn_overload_speed");
    private static final int OVERLOAD_CHECK_INTERVAL_TICKS = 20 * 5;
    private static final int OVERLOAD_EFFECT_DURATION_TICKS = 20 * 20;
    private static final int OVERLOAD_EFFECT_REFRESH_THRESHOLD_TICKS = 20 * 5;
    private static final double OVERLOAD_SPEED_AMOUNT = -0.15D;
    private static final int OVERLOADED_OCCUPIED_SLOT_THRESHOLD = 37;

    private ShulkerbornMythHandler() {
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof net.minecraft.world.entity.player.Player player)) {
            return;
        }

        syncOverloadNow(player);
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        net.minecraft.world.entity.player.Player player = event.getEntity();
        if (player.level().isClientSide()) {
            return;
        }

        if (!MythState.is(player, SHULKERBORN)) {
            clearOverload(player);
            return;
        }

        if (player.tickCount % OVERLOAD_CHECK_INTERVAL_TICKS == 0) {
            syncOverloadNow(player);
        }
    }

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        if (event.getEntity().level().isClientSide() || event.getEntity().getType() != EntityType.WARDEN) {
            return;
        }

        ItemStack stack = new ItemStack(MythosItems.RESONANCE_SHARD.asItem());
        ItemEntity drop = new ItemEntity(
            event.getEntity().level(),
            event.getEntity().getX(),
            event.getEntity().getY() + 0.5D,
            event.getEntity().getZ(),
            stack
        );
        event.getDrops().add(drop);
    }

    public static void syncOverloadNow(net.minecraft.world.entity.player.Player player) {
        if (!MythState.is(player, SHULKERBORN)) {
            clearOverload(player);
            return;
        }

        boolean overloaded = ShulkerbornInventoryHandler.occupiedSlotCount(player) >= OVERLOADED_OCCUPIED_SLOT_THRESHOLD;
        MythStatusHelper.syncModifier(
            player,
            Attributes.MOVEMENT_SPEED,
            SHULKERBORN_OVERLOAD_SPEED,
            OVERLOAD_SPEED_AMOUNT,
            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
            overloaded
        );

        if (!overloaded) {
            player.removeEffect(MythosEffects.SHULKERBORN_OVERLOADED);
            return;
        }

        ensureDisplayEffect(player, MythosEffects.SHULKERBORN_OVERLOADED, OVERLOAD_EFFECT_DURATION_TICKS);
    }

    private static void clearOverload(net.minecraft.world.entity.player.Player player) {
        MythStatusHelper.syncModifier(
            player,
            Attributes.MOVEMENT_SPEED,
            SHULKERBORN_OVERLOAD_SPEED,
            OVERLOAD_SPEED_AMOUNT,
            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
            false
        );
        player.removeEffect(MythosEffects.SHULKERBORN_OVERLOADED);
    }

    private static void ensureDisplayEffect(net.minecraft.world.entity.player.Player player, Holder<net.minecraft.world.effect.MobEffect> effect, int duration) {
        MobEffectInstance current = player.getEffect(effect);
        if (current == null || current.getDuration() <= OVERLOAD_EFFECT_REFRESH_THRESHOLD_TICKS) {
            player.addEffect(new MobEffectInstance(effect, duration, 0, false, true, true));
        }
    }
}
