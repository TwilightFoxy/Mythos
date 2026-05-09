package com.twily.mythos.gameplay;

import com.twily.mythos.Mythos;
import com.twily.mythos.data.MythItemMarkerHelper;
import com.twily.mythos.myth.MythState;
import com.twily.mythos.registry.MythosAttachments;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.lang.reflect.Field;

@EventBusSubscriber(modid = Mythos.MOD_ID)
public final class FemboyKitsuneMythHandler {

    private static final Identifier FEMBOY_KITSUNE = Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "femboy_kitsune");
    private static final String FEMBOY_MILK_MARKER = "mythos_femboy_milk";
    private static final int BUCKET_REUSE_DELAY_TICKS = 10;
    private static final float LOGIN_THUNDER_VOLUME = 10000.0F;
    private static final float LOGIN_THUNDER_PITCH = 1.0F;
    private static final Field LIGHTNING_LIFE_FIELD = resolveLightningLifeField();

    private FemboyKitsuneMythHandler() {
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (!MythState.is(player, FEMBOY_KITSUNE) || !isLoginLightningEnabled(player)) {
            return;
        }

        spawnLoginLightning(player);
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() || !MythState.is(player, FEMBOY_KITSUNE)) {
            return;
        }

        InteractionHand hand = event.getHand();
        ItemStack stack = player.getItemInHand(hand);
        if (!stack.is(Items.BUCKET)) {
            return;
        }

        player.setItemInHand(hand, createFemboyMilk());
        player.containerMenu.broadcastChanges();
        player.level().playSound(null, player.blockPosition(), SoundEvents.COW_MILK, SoundSource.PLAYERS, 1.0F, 1.0F);
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    @SubscribeEvent
    public static void onDrinkFinished(LivingEntityUseItemEvent.Finish event) {
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof Player player)) {
            return;
        }

        if (!MythItemMarkerHelper.hasMarker(event.getItem(), FEMBOY_MILK_MARKER)) {
            return;
        }

        player.getCooldowns().addCooldown(new ItemStack(Items.BUCKET), BUCKET_REUSE_DELAY_TICKS);
    }

    public static boolean isLoginLightningEnabled(ServerPlayer player) {
        return player.getData(MythosAttachments.FEMBOY_LOGIN_LIGHTNING_ENABLED);
    }

    public static boolean setLoginLightningEnabled(ServerPlayer player, boolean enabled) {
        boolean previous = isLoginLightningEnabled(player);
        player.setData(MythosAttachments.FEMBOY_LOGIN_LIGHTNING_ENABLED, enabled);
        return previous != enabled;
    }

    public static boolean toggleLoginLightningEnabled(ServerPlayer player) {
        boolean enabled = !isLoginLightningEnabled(player);
        player.setData(MythosAttachments.FEMBOY_LOGIN_LIGHTNING_ENABLED, enabled);
        return enabled;
    }

    private static ItemStack createFemboyMilk() {
        ItemStack result = new ItemStack(Items.MILK_BUCKET);
        result.set(DataComponents.CUSTOM_NAME, Component.translatable("item.mythos.femboy_milk").withStyle(ChatFormatting.LIGHT_PURPLE));
        MythItemMarkerHelper.setMarker(result, FEMBOY_MILK_MARKER);
        return result;
    }

    private static void spawnLoginLightning(ServerPlayer player) {
        ServerLevel level = (ServerLevel)player.level();
        LightningBolt lightning = net.minecraft.world.entity.EntityType.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
        if (lightning == null) {
            return;
        }

        lightning.setPos(player.getX(), player.getY(), player.getZ());
        lightning.setVisualOnly(true);
        suppressLightningServerImpact(lightning);
        level.addFreshEntity(lightning);

        long seed = player.getRandom().nextLong();
        ClientboundSoundPacket thunder = new ClientboundSoundPacket(
            BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.LIGHTNING_BOLT_THUNDER),
            SoundSource.WEATHER,
            player.getX(),
            player.getY(),
            player.getZ(),
            LOGIN_THUNDER_VOLUME,
            LOGIN_THUNDER_PITCH,
            seed
        );

        for (ServerPlayer online : level.getServer().getPlayerList().getPlayers()) {
            online.connection.send(thunder);
        }
    }

    private static void suppressLightningServerImpact(LightningBolt lightning) {
        if (LIGHTNING_LIFE_FIELD == null) {
            return;
        }

        try {
            LIGHTNING_LIFE_FIELD.setInt(lightning, 1);
        } catch (IllegalAccessException ignored) {
        }
    }

    private static Field resolveLightningLifeField() {
        try {
            Field field = LightningBolt.class.getDeclaredField("life");
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }
}
