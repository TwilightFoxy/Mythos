package com.twily.mythos.client;

import com.twily.mythos.gameplay.StarWandererMythHandler;
import com.twily.mythos.myth.MythState;
import com.twily.mythos.network.UseStarWandererActionPayload;
import com.twily.mythos.registry.MythosAttachments;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.Optional;

public final class StarWandererActionKeyHandler {

    private StarWandererActionKeyHandler() {
    }

    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
    }

    public static Component stepKeyName() {
        return MythosSkillKeys.skill1Name();
    }

    public static Component strikeKeyName() {
        return MythosSkillKeys.skill2Name();
    }

    public static Component waveKeyName() {
        return MythosSkillKeys.skill3Name();
    }

    public static final class Handler {
        private static boolean wasStepHeld;
        private static boolean wasBeamHeld;

        private Handler() {
        }

        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            Minecraft minecraft = Minecraft.getInstance();
            Player player = minecraft.player;
            boolean held = MythosSkillKeys.skill1().isDown();
            boolean beamHeld = MythosSkillKeys.skill2().isDown();

            if (player == null) {
                if (wasBeamHeld) {
                    ClientPacketDistributor.sendToServer(new UseStarWandererActionPayload("beam_stop"));
                }
                wasStepHeld = false;
                wasBeamHeld = false;
                return;
            }

            boolean canUse = minecraft.screen == null && MythState.matches(player, StarWandererMythHandler.STAR_WANDERER_ID);
            if (canUse) {
                spawnStarWandererAura(minecraft, player);
            }
            if (held && canUse) {
                spawnPreviewParticles(minecraft, StarWandererMythHandler.previewStarStepTarget(player));
            }

            if (wasStepHeld && !held && canUse) {
                ClientPacketDistributor.sendToServer(new UseStarWandererActionPayload("step"));
            }

            if (canUse) {
                if (!wasBeamHeld && beamHeld) {
                    ClientPacketDistributor.sendToServer(new UseStarWandererActionPayload("beam_start"));
                } else if (wasBeamHeld && !beamHeld) {
                    ClientPacketDistributor.sendToServer(new UseStarWandererActionPayload("beam_stop"));
                }
                while (MythosSkillKeys.skill3().consumeClick()) {
                    ClientPacketDistributor.sendToServer(new UseStarWandererActionPayload("wave"));
                }
            } else if (wasBeamHeld) {
                ClientPacketDistributor.sendToServer(new UseStarWandererActionPayload("beam_stop"));
            }

            wasStepHeld = held;
            wasBeamHeld = beamHeld && canUse;
        }

        private static void spawnPreviewParticles(Minecraft minecraft, Optional<Vec3> target) {
            if (minecraft.level == null || minecraft.player == null || target.isEmpty() || minecraft.level.getGameTime() % 2L != 0L) {
                return;
            }

            Vec3 feet = target.get();
            double width = minecraft.player.getBbWidth() * 0.5D;
            double height = minecraft.player.getBbHeight();

            spawn(minecraft, feet.x, feet.y + 0.02D, feet.z, ParticleTypes.GLOW);
            spawn(minecraft, feet.x - width, feet.y + 0.05D, feet.z - width, ParticleTypes.END_ROD);
            spawn(minecraft, feet.x + width, feet.y + 0.05D, feet.z - width, ParticleTypes.END_ROD);
            spawn(minecraft, feet.x - width, feet.y + 0.05D, feet.z + width, ParticleTypes.END_ROD);
            spawn(minecraft, feet.x + width, feet.y + 0.05D, feet.z + width, ParticleTypes.END_ROD);
            spawn(minecraft, feet.x, feet.y + height * 0.5D, feet.z, ParticleTypes.FIREWORK);
        }

        private static void spawnStarWandererAura(Minecraft minecraft, Player player) {
            if (minecraft.level == null) {
                return;
            }

            int energy = player.getData(MythosAttachments.STAR_WANDERER_ENERGY);
            float charge = Mth.clamp(energy / (float) StarWandererMythHandler.MAX_ENERGY, 0.0F, 1.0F);
            long time = minecraft.level.getGameTime();

            if (charge > 0.0F && time % 12L == 0L) {
                double angle = (time * 0.17D) % (Math.PI * 2.0D);
                double radius = 0.24D + charge * 0.08D;
                double y = player.getY() + 1.05D + Math.sin(time * 0.09D) * 0.08D;
                spawnWithVelocity(
                    minecraft,
                    player.getX() + Math.cos(angle) * radius,
                    y,
                    player.getZ() + Math.sin(angle) * radius,
                    ParticleTypes.GLOW,
                    0.0D,
                    0.002D,
                    0.0D
                );
                if (time % 24L == 0L) {
                    spawnWithVelocity(
                        minecraft,
                        player.getX() - Math.cos(angle * 0.8D) * (radius * 0.8D),
                        y + 0.22D,
                        player.getZ() - Math.sin(angle * 0.8D) * (radius * 0.8D),
                        ParticleTypes.END_ROD,
                        0.0D,
                        0.001D,
                        0.0D
                    );
                }
            }

            Vec3 movement = player.getDeltaMovement();
            double horizontalSpeed = movement.horizontalDistanceSqr();
            if (horizontalSpeed > 0.0035D && time % 4L == 0L) {
                Vec3 trailPos = player.position().subtract(movement.normalize().scale(0.35D)).add(0.0D, 0.12D, 0.0D);
                if (time % 8L == 0L) {
                    spawnWithVelocity(minecraft, trailPos.x, trailPos.y, trailPos.z, ParticleTypes.END_ROD, 0.0D, 0.0D, 0.0D);
                }
                if (charge >= 0.5F) {
                    spawnWithVelocity(minecraft, trailPos.x, trailPos.y + 0.08D, trailPos.z, ParticleTypes.GLOW, 0.0D, 0.0D, 0.0D);
                }
            }
        }

        private static void spawn(Minecraft minecraft, double x, double y, double z, net.minecraft.core.particles.ParticleOptions particle) {
            minecraft.level.addParticle(particle, x, y, z, 0.0D, 0.0D, 0.0D);
        }

        private static void spawnWithVelocity(
            Minecraft minecraft,
            double x,
            double y,
            double z,
            net.minecraft.core.particles.ParticleOptions particle,
            double velocityX,
            double velocityY,
            double velocityZ
        ) {
            minecraft.level.addParticle(particle, x, y, z, velocityX, velocityY, velocityZ);
        }
    }
}
