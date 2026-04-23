package com.twily.mythos.gameplay;

import com.twily.mythos.Mythos;
import com.twily.mythos.myth.MythState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

@EventBusSubscriber(modid = Mythos.MOD_ID)
public final class MythScaleHandler {

    private static final Identifier DWARF = Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "dwarf");
    private static final Identifier FAIRY = Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "fairy");
    private static final Identifier PLAYER = Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "player");
    private static final Identifier LEGACY_DWARF_SCALE = Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "dwarf_scale");

    /*
     * FROZEN SECTION: myth-driven player scale
     * This class owns persistent player size for standard, dwarf, fairy, and player myths.
     * Dynamic size changes such as oni battle form must stay outside this handler.
     * It is considered stable and should not be changed without explicit request.
     */
    private static final double NORMAL_SCALE = 1.0D;
    private static final double DWARF_SCALE = 0.75D;
    private static final double FAIRY_SCALE = 0.5D;

    private MythScaleHandler() {
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Player player)) {
            return;
        }

        syncPersistentScaleOnLoad(player);
    }

    public static void syncPersistentScaleOnMythChange(Player player, net.minecraft.resources.Identifier mythId) {
        applyTargetScale(player, targetScale(mythId));
    }

    public static void syncPersistentScaleOnLoad(Player player) {
        if (MythState.is(player, PLAYER)) {
            return;
        }

        applyTargetScale(player, targetScale(MythState.get(player)));
    }

    private static void applyTargetScale(Player player, double targetScale) {
        AttributeInstance scale = player.getAttribute(Attributes.SCALE);
        if (scale == null) {
            return;
        }

        scale.removeModifier(LEGACY_DWARF_SCALE);

        if (scale.getBaseValue() != targetScale) {
            scale.setBaseValue(targetScale);
        }
    }

    private static double targetScale(Identifier mythId) {
        if (DWARF.equals(mythId)) {
            return DWARF_SCALE;
        }

        if (FAIRY.equals(mythId)) {
            return FAIRY_SCALE;
        }

        return NORMAL_SCALE;
    }
    // END FROZEN SECTION: myth-driven player scale
}
