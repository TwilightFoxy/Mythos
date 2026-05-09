package com.twily.mythos.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.twily.mythos.Mythos;
import com.twily.mythos.myth.MythState;
import com.twily.mythos.registry.MythosAttachments;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public final class SlimeDisguiseLayer extends RenderLayer<AvatarRenderState, PlayerModel> {

    private static final Identifier SLIME = Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "slime");
    private static final Identifier SLIME_OVERLAY = Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "textures/entity/slime_player_overlay.png");
    private static final int[] INNER_AURA_TINT_BY_STAGE = {0xCC44FF88, 0xEC55FF88, 0xFF66FF88, 0xFF77FF88};
    private static final int[] OUTER_AURA_TINT_BY_STAGE = {0x7066FF99, 0x8855FF99, 0xA066FF99, 0xB877FF99};
    private static final float[] INNER_AURA_SCALE_BY_STAGE = {1.08F, 1.11F, 1.16F, 1.24F};
    private static final float[] OUTER_AURA_SCALE_BY_STAGE = {1.14F, 1.19F, 1.25F, 1.36F};

    public SlimeDisguiseLayer(RenderLayerParent<AvatarRenderState, PlayerModel> renderer, EntityModelSet modelSet) {
        super(renderer);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, AvatarRenderState state, float yRot, float xRot) {
        if (state.isInvisible) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        Entity entity = minecraft.level.getEntity(state.id);
        if (!(entity instanceof AbstractClientPlayer player) || !MythState.matches(player, SLIME)) {
            return;
        }

        int stage = Mth.clamp(player.getData(MythosAttachments.SLIME_STAGE), 0, INNER_AURA_TINT_BY_STAGE.length - 1);
        poseStack.pushPose();
        poseStack.scale(INNER_AURA_SCALE_BY_STAGE[stage], INNER_AURA_SCALE_BY_STAGE[stage], INNER_AURA_SCALE_BY_STAGE[stage]);
        renderTranslucentAura(poseStack, submitNodeCollector, lightCoords, state, INNER_AURA_TINT_BY_STAGE[stage], 1);
        float outerScale = OUTER_AURA_SCALE_BY_STAGE[stage] / INNER_AURA_SCALE_BY_STAGE[stage];
        poseStack.scale(outerScale, outerScale, outerScale);
        renderTranslucentAura(poseStack, submitNodeCollector, lightCoords, state, OUTER_AURA_TINT_BY_STAGE[stage], 2);
        poseStack.popPose();
    }

    private void renderTranslucentAura(
        PoseStack poseStack,
        SubmitNodeCollector submitNodeCollector,
        int lightCoords,
        AvatarRenderState state,
        int tint,
        int order
    ) {
        submitNodeCollector.order(order)
            .submitModel(
                this.getParentModel(),
                state,
                poseStack,
                this.getParentModel().renderType(SLIME_OVERLAY),
                lightCoords,
                LivingEntityRenderer.getOverlayCoords(state, 0.0F),
                tint,
                null,
                state.outlineColor,
                null
            );
    }
}
