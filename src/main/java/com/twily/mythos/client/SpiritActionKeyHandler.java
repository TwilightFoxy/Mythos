package com.twily.mythos.client;

import com.twily.mythos.gameplay.SpiritMythHandler;
import com.twily.mythos.myth.MythState;
import com.twily.mythos.network.UseSpiritActionPayload;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.Optional;

public final class SpiritActionKeyHandler {

    private SpiritActionKeyHandler() {
    }

    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
    }

    public static Component phaseKeyName() {
        return MythosSkillKeys.skill1Name();
    }

    public static final class Handler {

        private static boolean wasHeld;

        private Handler() {
        }

        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            Minecraft minecraft = Minecraft.getInstance();
            Player player = minecraft.player;
            boolean held = MythosSkillKeys.skill1().isDown();

            if (player == null) {
                wasHeld = false;
                return;
            }

            boolean canUse = minecraft.screen == null && MythState.matches(player, SpiritMythHandler.SPIRIT_ID);
            if (held && canUse) {
                spawnPreviewParticles(minecraft, SpiritMythHandler.previewPhaseTarget(player));
            }

            if (wasHeld && !held && canUse) {
                ClientPacketDistributor.sendToServer(new UseSpiritActionPayload());
            }

            wasHeld = held;
        }

        private static void spawnPreviewParticles(Minecraft minecraft, Optional<Vec3> target) {
            if (minecraft.level == null || target.isEmpty() || minecraft.player == null || minecraft.level.getGameTime() % 2L != 0L) {
                return;
            }

            Vec3 feet = target.get();
            double width = minecraft.player.getBbWidth() * 0.5D;
            double height = minecraft.player.getBbHeight();

            spawn(minecraft, feet.x, feet.y + 0.02D, feet.z, ParticleTypes.GLOW);
            spawn(minecraft, feet.x - width, feet.y + 0.05D, feet.z - width, ParticleTypes.SOUL_FIRE_FLAME);
            spawn(minecraft, feet.x + width, feet.y + 0.05D, feet.z - width, ParticleTypes.SOUL_FIRE_FLAME);
            spawn(minecraft, feet.x - width, feet.y + 0.05D, feet.z + width, ParticleTypes.SOUL_FIRE_FLAME);
            spawn(minecraft, feet.x + width, feet.y + 0.05D, feet.z + width, ParticleTypes.SOUL_FIRE_FLAME);
            spawn(minecraft, feet.x, feet.y + height * 0.5D, feet.z, ParticleTypes.REVERSE_PORTAL);
        }

        private static void spawn(Minecraft minecraft, double x, double y, double z, net.minecraft.core.particles.ParticleOptions particle) {
            minecraft.level.addParticle(particle, x, y, z, 0.0D, 0.0D, 0.0D);
        }
    }
}
