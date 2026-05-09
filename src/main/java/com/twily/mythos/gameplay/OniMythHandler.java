package com.twily.mythos.gameplay;

import com.twily.mythos.Mythos;
import com.twily.mythos.myth.MythState;
import com.twily.mythos.registry.MythosAttachments;
import com.twily.mythos.registry.MythosEffects;
import com.twily.mythos.registry.MythosItems;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Optional;

@EventBusSubscriber(modid = Mythos.MOD_ID)
public final class OniMythHandler {

    private static final Identifier ONI = Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "oni");
    private static final Identifier ONI_SCALE = Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "oni_battle_scale");
    private static final Identifier ONI_HEALTH = Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "oni_battle_health");
    private static final Identifier ONI_KNOCKBACK_RESISTANCE = Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "oni_battle_knockback_resistance");
    private static final double ONI_BATTLE_SCALE_AMOUNT = 0.5D;
    private static final double ONI_BATTLE_HEALTH_AMOUNT = 10.0D;
    private static final double ONI_BATTLE_KNOCKBACK_RESISTANCE_AMOUNT = 1.0D;
    private static final float ONI_MELEE_DAMAGE_MULTIPLIER = 1.3F;
    private static final float ONI_RANGED_DAMAGE_MULTIPLIER = 0.7F;
    private static final int BATTLE_FORM_DURATION_TICKS = 20 * 60 * 3;
    private static final int BATTLE_FORM_COOLDOWN_TICKS = 20 * 60 * 10;
    private static final int ENEMY_GLOW_DURATION_TICKS = 40;
    private static final int ENEMY_GLOW_RADIUS = 20;

    private OniMythHandler() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        boolean isOni = MythState.matches(player, ONI);

        if (player.level().isClientSide()) {
            return;
        }

        tickRageTalismanAftermath(player);
        tickCooldown(player, MythosAttachments.ONI_BATTLE_FORM_COOLDOWN.get());
        syncBattleFormEffects(player, isOni);
        if (player.containerMenu instanceof SmithingMenu smithingMenu) {
            handleOniSmithing(player, smithingMenu, isOni);
        }

        if (!isOni) {
            clearBattleFormState(player);
            return;
        }

        int activeTicks = player.getData(MythosAttachments.ONI_BATTLE_FORM_TICKS);
        if (activeTicks > 0) {
            syncBattleFormModifiers(player, true);
            if (activeTicks % 20 == 0) {
                glowNearbyEnemies(player);
            }
            player.setData(MythosAttachments.ONI_BATTLE_FORM_TICKS, activeTicks - 1);
            if (activeTicks == 1) {
                expireBattleForm(player);
            }
        } else {
            syncBattleFormModifiers(player, false);
        }
    }

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Pre event) {
        if (!(event.getSource().getEntity() instanceof Player player) || !MythState.matches(player, ONI)) {
            return;
        }

        ItemStack mainHand = player.getItemBySlot(EquipmentSlot.MAINHAND);
        if (event.getSource().getDirectEntity() == player && isOniMeleeWeapon(mainHand)) {
            event.setNewDamage(event.getNewDamage() * ONI_MELEE_DAMAGE_MULTIPLIER);
            return;
        }

        if (event.getSource().getDirectEntity() instanceof Projectile) {
            event.setNewDamage(event.getNewDamage() * ONI_RANGED_DAMAGE_MULTIPLIER);
        }
    }

    public static void activateBattleForm(net.minecraft.server.level.ServerPlayer player) {
        if (!MythState.matches(player, ONI)) {
            return;
        }

        if (player.getData(MythosAttachments.ONI_BATTLE_FORM_TICKS) > 0) {
            player.sendSystemMessage(Component.translatable("message.mythos.oni_battle_form_active"));
            return;
        }

        int cooldown = player.getData(MythosAttachments.ONI_BATTLE_FORM_COOLDOWN);
        if (cooldown > 0) {
            player.sendSystemMessage(Component.translatable("message.mythos.oni_battle_form_cooldown", Math.max(1, cooldown / 20)));
            return;
        }

        player.setData(MythosAttachments.ONI_BATTLE_FORM_TICKS, BATTLE_FORM_DURATION_TICKS);
        syncBattleFormModifiers(player, true);
    }

    private static void syncBattleFormEffects(Player player, boolean isOni) {
        if (!isOni) {
            player.removeEffect(MythosEffects.ONI_BATTLE_FORM);
            player.removeEffect(MythosEffects.ONI_BATTLE_FORM_COOLDOWN);
            return;
        }

        int activeTicks = player.getData(MythosAttachments.ONI_BATTLE_FORM_TICKS);
        if (activeTicks > 0) {
            player.addEffect(new MobEffectInstance(MythosEffects.ONI_BATTLE_FORM, activeTicks, 0, false, true, true));
        } else {
            player.removeEffect(MythosEffects.ONI_BATTLE_FORM);
        }

        int cooldown = player.getData(MythosAttachments.ONI_BATTLE_FORM_COOLDOWN);
        if (cooldown > 0) {
            player.addEffect(new MobEffectInstance(MythosEffects.ONI_BATTLE_FORM_COOLDOWN, cooldown, 0, false, true, true));
        } else {
            player.removeEffect(MythosEffects.ONI_BATTLE_FORM_COOLDOWN);
        }
    }

    private static void glowNearbyEnemies(Player player) {
        player.level()
            .getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(ENEMY_GLOW_RADIUS), entity -> entity.isAlive() && entity instanceof Enemy)
            .forEach(entity -> entity.addEffect(new MobEffectInstance(MobEffects.GLOWING, ENEMY_GLOW_DURATION_TICKS, 0, false, false, true)));
    }

    private static void syncBattleFormModifiers(Player player, boolean active) {
        MythStatusHelper.syncModifier(player, Attributes.SCALE, ONI_SCALE, ONI_BATTLE_SCALE_AMOUNT, AttributeModifier.Operation.ADD_VALUE, active);
        MythStatusHelper.syncModifier(player, Attributes.MAX_HEALTH, ONI_HEALTH, ONI_BATTLE_HEALTH_AMOUNT, AttributeModifier.Operation.ADD_VALUE, active);
        MythStatusHelper.syncModifier(player, Attributes.KNOCKBACK_RESISTANCE, ONI_KNOCKBACK_RESISTANCE, ONI_BATTLE_KNOCKBACK_RESISTANCE_AMOUNT, AttributeModifier.Operation.ADD_VALUE, active);

        if (!active) {
            double maxHealth = player.getMaxHealth();
            if (player.getHealth() > maxHealth) {
                player.setHealth((float) maxHealth);
            }
        }
    }

    private static void expireBattleForm(Player player) {
        player.setData(MythosAttachments.ONI_BATTLE_FORM_TICKS, 0);
        player.setData(MythosAttachments.ONI_BATTLE_FORM_COOLDOWN, BATTLE_FORM_COOLDOWN_TICKS);
        syncBattleFormModifiers(player, false);

        float currentHealth = player.getHealth();
        if (currentHealth <= 1.0F) {
            return;
        }

        float backlashDamage = Math.min(currentHealth * 0.5F, currentHealth - 1.0F);
        if (backlashDamage > 0.0F) {
            player.hurt(player.damageSources().magic(), backlashDamage);
        }
    }

    private static void clearBattleFormState(Player player) {
        player.setData(MythosAttachments.ONI_BATTLE_FORM_TICKS, 0);
        player.setData(MythosAttachments.ONI_BATTLE_FORM_COOLDOWN, 0);
        syncBattleFormModifiers(player, false);
        player.removeEffect(MythosEffects.ONI_BATTLE_FORM);
        player.removeEffect(MythosEffects.ONI_BATTLE_FORM_COOLDOWN);
    }

    private static void handleOniSmithing(Player player, SmithingMenu smithingMenu, boolean isOni) {
        ItemStack template = smithingMenu.getSlot(SmithingMenu.TEMPLATE_SLOT).getItem();
        ItemStack base = smithingMenu.getSlot(SmithingMenu.BASE_SLOT).getItem();
        ItemStack addition = smithingMenu.getSlot(SmithingMenu.ADDITIONAL_SLOT).getItem();
        if (!template.isEmpty()) {
            return;
        }

        Optional<ItemStack> result = Optional.empty();
        if (isRageTalismanInputs(base, addition)) {
            result = Optional.of(new ItemStack(MythosItems.RAGE_TALISMAN.asItem()));
        } else {
            return;
        }

        if (!isOni) {
            clearSmithingResult(smithingMenu);
            if (result.isPresent() && player.tickCount % 40 == 0) {
                player.sendSystemMessage(Component.translatable("message.mythos.oni_only_artifacts"));
            }
            return;
        }

        ItemStack current = smithingMenu.getSlot(SmithingMenu.RESULT_SLOT).getItem();
        if (!ItemStack.matches(current, result.get())) {
            smithingMenu.getSlot(SmithingMenu.RESULT_SLOT).set(result.get());
            smithingMenu.broadcastChanges();
        }
    }

    private static boolean isRageTalismanInputs(ItemStack base, ItemStack addition) {
        return base.is(Items.PAPER) && addition.is(Items.NETHER_WART);
    }

    private static boolean isOniMeleeWeapon(ItemStack stack) {
        return stack.is(ItemTags.SWORDS) || stack.is(ItemTags.AXES);
    }

    private static void clearSmithingResult(SmithingMenu smithingMenu) {
        if (!smithingMenu.getSlot(SmithingMenu.RESULT_SLOT).getItem().isEmpty()) {
            smithingMenu.getSlot(SmithingMenu.RESULT_SLOT).set(ItemStack.EMPTY);
            smithingMenu.broadcastChanges();
        }
    }

    private static void tickRageTalismanAftermath(Player player) {
        int ticks = player.getData(MythosAttachments.RAGE_TALISMAN_AFTERMATH_TICKS);
        if (ticks <= 0) {
            return;
        }

        int next = ticks - 1;
        player.setData(MythosAttachments.RAGE_TALISMAN_AFTERMATH_TICKS, next);
        if (next == 0) {
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 20 * 60, 0, false, true, true));
        }
    }

    private static void tickCooldown(Player player, net.neoforged.neoforge.attachment.AttachmentType<Integer> type) {
        int cooldown = player.getData(type);
        if (cooldown > 0) {
            player.setData(type, cooldown - 1);
        }
    }
}
