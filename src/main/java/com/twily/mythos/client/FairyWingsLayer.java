package com.twily.mythos.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.twily.mythos.Mythos;
import com.twily.mythos.myth.MythState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.equipment.ElytraModel;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public final class FairyWingsLayer extends RenderLayer<AvatarRenderState, PlayerModel> {

    private static final Identifier FAIRY = Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "fairy");
    private static final Identifier WINGS_TEXTURE = Identifier.withDefaultNamespace("textures/entity/elytra.png");
    private final ElytraModel wingsModel;

    public FairyWingsLayer(RenderLayerParent<AvatarRenderState, PlayerModel> renderer, EntityModelSet modelSet) {
        super(renderer);
        this.wingsModel = new ElytraModel(modelSet.bakeLayer(ModelLayers.ELYTRA));
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, AvatarRenderState state, float yRot, float xRot) {
        if (state.isInvisible || !state.chestEquipment.isEmpty()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        Entity entity = minecraft.level.getEntity(state.id);
        if (!(entity instanceof Player player) || !MythState.matches(player, FAIRY)) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(0.0F, 0.0F, 0.125F);
        poseStack.scale(0.6F, 0.6F, 0.6F);
        renderColoredCutoutModel(this.wingsModel, WINGS_TEXTURE, poseStack, submitNodeCollector, lightCoords, state, 0xFFFFFFFF, 1);
        poseStack.popPose();
    }
}
