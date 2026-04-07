package com.twily.mythos.gameplay;

import com.twily.mythos.Mythos;
import com.twily.mythos.data.MythItemMarkerHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.ArrowLooseEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = Mythos.MOD_ID)
public final class ElvenBowHandler {

    private static final String ELVEN_BOW_MARKER = "mythos_elven_bow";
    private static final int ELVEN_BOW_LOOTING_LEVEL = 3;
    private static final Map<UUID, PendingShot> PENDING_SHOTS = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> MARKED_PROJECTILES = new ConcurrentHashMap<>();

    private ElvenBowHandler() {
    }

    @SubscribeEvent
    public static void onArrowLoose(ArrowLooseEvent event) {
        if (event.getLevel().isClientSide() || event.getCharge() <= 0) {
            return;
        }

        if (MythItemMarkerHelper.hasMarker(event.getBow(), ELVEN_BOW_MARKER)) {
            PENDING_SHOTS.put(event.getEntity().getUUID(), new PendingShot(event.getLevel().getGameTime() + 2L, ELVEN_BOW_LOOTING_LEVEL));
        }
    }

    @SubscribeEvent
    public static void onProjectileJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || event.loadedFromDisk()) {
            return;
        }

        if (!(event.getEntity() instanceof AbstractArrow arrow) || !(arrow.getOwner() instanceof Player player)) {
            return;
        }

        PendingShot pendingShot = PENDING_SHOTS.get(player.getUUID());
        if (pendingShot == null) {
            return;
        }

        if (event.getLevel().getGameTime() <= pendingShot.expiresAt()) {
            MARKED_PROJECTILES.put(arrow.getUUID(), pendingShot.lootingLevel());
        }

        PENDING_SHOTS.remove(player.getUUID());
    }

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        if (event.getEntity() instanceof Player) {
            return;
        }

        if (!(event.getSource().getDirectEntity() instanceof AbstractArrow arrow)) {
            return;
        }

        Integer lootingLevel = MARKED_PROJECTILES.get(arrow.getUUID());
        if (lootingLevel == null || lootingLevel <= 0) {
            return;
        }

        for (ItemEntity drop : event.getDrops()) {
            ItemStack stack = drop.getItem();
            if (stack.isEmpty() || stack.getMaxStackSize() <= 1) {
                continue;
            }

            int extra = event.getEntity().getRandom().nextInt(lootingLevel + 1);
            if (extra <= 0) {
                continue;
            }

            int targetCount = Math.min(stack.getCount() + extra, stack.getMaxStackSize());
            stack.setCount(targetCount);
        }
    }

    @SubscribeEvent
    public static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
        if (event.getEntity() instanceof AbstractArrow arrow) {
            MARKED_PROJECTILES.remove(arrow.getUUID());
        } else if (event.getEntity() instanceof Player player) {
            PENDING_SHOTS.remove(player.getUUID());
        }
    }

    private record PendingShot(long expiresAt, int lootingLevel) {
    }
}
