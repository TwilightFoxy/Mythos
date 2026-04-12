package com.twily.mythos.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.twily.mythos.Mythos;
import com.twily.mythos.myth.MythState;
import com.twily.mythos.registry.MythosAttachments;
import com.twily.mythos.registry.MythosItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public final class KitsuneAdornmentLayer extends RenderLayer<AvatarRenderState, PlayerModel> {

    private static final Identifier KITSUNE = Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "kitsune");
    private static final float TAIL_ANCHOR_X = 0.0F;
    private static final float TAIL_ANCHOR_Y = 0.58F;
    private static final float TAIL_ANCHOR_Z = 0.18F;
    private static final float TAIL_OFFSET_X = -1.15F;
    private static final float TAIL_OFFSET_Y = (-15.25F / 16.0F) - 0.15F;
    private static final float TAIL_OFFSET_Z = (-7.0F / 16.0F) + 1.6F;
    private static final float TAIL_ROT_X = 170.0F;
    private static final float TAIL_ROT_Y = 210.0F;
    private static final float TAIL_ROT_Z = 0.0F;
    private static final float TAIL_SCALE_X = 1.8F;
    private static final float TAIL_SCALE_Y = 2.0F;
    private static final float TAIL_SCALE_Z = 1.8F;
    private final ItemStackRenderState tailRenderState = new ItemStackRenderState();
    private final ItemStackRenderState maskRenderState = new ItemStackRenderState();

    public KitsuneAdornmentLayer(RenderLayerParent<AvatarRenderState, PlayerModel> renderer, EntityModelSet modelSet) {
        super(renderer);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, AvatarRenderState state, float yRot, float xRot) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        Entity entity = minecraft.level.getEntity(state.id);
        if (!(entity instanceof Player player) || !MythState.is(player, KITSUNE)) {
            return;
        }

        if (player.getData(MythosAttachments.KITSUNE_MASKED) && !state.isInvisible) {
            poseStack.pushPose();
            this.getParentModel().body.translateAndRotate(poseStack);
            poseStack.translate(TAIL_ANCHOR_X, TAIL_ANCHOR_Y, TAIL_ANCHOR_Z);
            poseStack.translate(TAIL_OFFSET_X, TAIL_OFFSET_Y, TAIL_OFFSET_Z);
            // Disabled tail-debug offsets. Keep these lines nearby if we need to revive the in-game tuner later.
            // poseStack.translate(KitsuneTailDebugState.offsetX(), KitsuneTailDebugState.offsetY(), KitsuneTailDebugState.offsetZ());
            poseStack.mulPose(Axis.XP.rotationDegrees(TAIL_ROT_X));
            poseStack.mulPose(Axis.YP.rotationDegrees(TAIL_ROT_Y));
            poseStack.mulPose(Axis.ZP.rotationDegrees(TAIL_ROT_Z));
            // Disabled tail-debug rotations. Keep for future reuse.
            // poseStack.mulPose(Axis.XP.rotationDegrees(KitsuneTailDebugState.rotX()));
            // poseStack.mulPose(Axis.YP.rotationDegrees(KitsuneTailDebugState.rotY()));
            // poseStack.mulPose(Axis.ZP.rotationDegrees(KitsuneTailDebugState.rotZ()));
            poseStack.scale(TAIL_SCALE_X, TAIL_SCALE_Y, TAIL_SCALE_Z);
            poseStack.translate(-0.5F, -0.5F, -0.5F);
            ItemStack tailStack = new ItemStack(MythosItems.KITSUNE_TAIL_VISUAL.asItem());
            Minecraft.getInstance().getItemModelResolver().updateForLiving(this.tailRenderState, tailStack, ItemDisplayContext.NONE, player);
            this.tailRenderState.submit(poseStack, submitNodeCollector, lightCoords, 0, 0);
            poseStack.popPose();
        }

        if (player.getData(MythosAttachments.KITSUNE_MASKED)) {
            if (!state.isInvisible) {
            poseStack.pushPose();
            PlayerModel parentModel = this.getParentModel();
            parentModel.root().translateAndRotate(poseStack);
            parentModel.translateToHead(poseStack);
            CustomHeadLayer.translateToHead(poseStack, CustomHeadLayer.Transforms.DEFAULT);
            ItemStack maskStack = new ItemStack(MythosItems.KITSUNE_MASK_VISUAL.asItem());
            Minecraft.getInstance().getItemModelResolver().updateForLiving(this.maskRenderState, maskStack, ItemDisplayContext.HEAD, player);
            this.maskRenderState.submit(poseStack, submitNodeCollector, lightCoords, 0, 0);
            poseStack.popPose();
            }
        }
    }
}
