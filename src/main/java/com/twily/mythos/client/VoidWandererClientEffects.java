package com.twily.mythos.client;

import com.twily.mythos.Mythos;
import com.twily.mythos.gameplay.VoidWandererMythHandler;
import com.twily.mythos.myth.MythState;
import com.twily.mythos.registry.MythosAttachments;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = Mythos.MOD_ID, value = Dist.CLIENT)
public final class VoidWandererClientEffects {

    private VoidWandererClientEffects() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (minecraft.level == null || player == null || minecraft.screen != null || !MythState.matches(player, VoidWandererMythHandler.VOID_WANDERER_ID)) {
            return;
        }

        int energy = player.getData(MythosAttachments.VOID_WANDERER_ENERGY);
        float charge = Mth.clamp(energy / (float) VoidWandererMythHandler.MAX_VOID, 0.0F, 1.0F);
        long time = minecraft.level.getGameTime();

        if (time % 14L == 0L) {
            double angle = (time * 0.14D) % (Math.PI * 2.0D);
            double radius = 0.22D + charge * 0.06D;
            double y = player.getY() + 1.02D + Math.sin(time * 0.08D) * 0.05D;
            spawnWithVelocity(
                minecraft,
                player.getX() + Math.cos(angle) * radius,
                y,
                player.getZ() + Math.sin(angle) * radius,
                ParticleTypes.PORTAL,
                0.0D,
                -0.01D,
                0.0D
            );
        }

        Vec3 movement = player.getDeltaMovement();
        if (movement.horizontalDistanceSqr() > 0.0025D && time % 7L == 0L) {
            Vec3 trailPos = player.position().subtract(movement.normalize().scale(0.3D)).add(0.0D, 0.1D, 0.0D);
            spawnWithVelocity(minecraft, trailPos.x, trailPos.y, trailPos.z, ParticleTypes.SMOKE, 0.0D, 0.01D, 0.0D);
            if (charge >= 0.5F && time % 14L == 0L) {
                spawnWithVelocity(minecraft, trailPos.x, trailPos.y + 0.06D, trailPos.z, ParticleTypes.PORTAL, 0.0D, 0.0D, 0.0D);
            }
        }
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
