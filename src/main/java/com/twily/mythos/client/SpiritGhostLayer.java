package com.twily.mythos.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.twily.mythos.Mythos;
import com.twily.mythos.gameplay.SpiritMythHandler;
import com.twily.mythos.myth.MythState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;

public final class SpiritGhostLayer extends RenderLayer<AvatarRenderState, PlayerModel> {

    private static final Identifier SPIRIT = Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "spirit");
    private static final int DAY_GHOST_TINT = 0x96FFFFFF;

    public SpiritGhostLayer(RenderLayerParent<AvatarRenderState, PlayerModel> renderer) {
        super(renderer);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, AvatarRenderState state, float yRot, float xRot) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        Entity entity = minecraft.level.getEntity(state.id);
        if (!(entity instanceof AbstractClientPlayer player) || !MythState.matches(player, SPIRIT) || !SpiritMythHandler.isDaytimeSpirit(player.level())) {
            return;
        }

        submitNodeCollector.order(3)
            .submitModel(
                this.getParentModel(),
                state,
                poseStack,
                this.getParentModel().renderType(player.getSkin().body().texturePath()),
                lightCoords,
                LivingEntityRenderer.getOverlayCoords(state, 0.0F),
                DAY_GHOST_TINT,
                null,
                state.outlineColor,
                null
            );
    }
}
