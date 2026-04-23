package com.twily.mythos.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.twily.mythos.Mythos;
import com.twily.mythos.myth.MythState;
import com.twily.mythos.registry.MythosEffects;
import com.twily.mythos.registry.MythosItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public final class OniAdornmentLayer extends RenderLayer<AvatarRenderState, PlayerModel> {

    private static final net.minecraft.resources.Identifier ONI = net.minecraft.resources.Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "oni");
    private final ItemStackRenderState maskRenderState = new ItemStackRenderState();

    public OniAdornmentLayer(RenderLayerParent<AvatarRenderState, PlayerModel> renderer) {
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
        if (!(entity instanceof Player player) || !MythState.is(player, ONI) || !player.hasEffect(MythosEffects.ONI_BATTLE_FORM)) {
            return;
        }

        poseStack.pushPose();
        PlayerModel parentModel = this.getParentModel();
        parentModel.root().translateAndRotate(poseStack);
        parentModel.translateToHead(poseStack);
        CustomHeadLayer.translateToHead(poseStack, CustomHeadLayer.Transforms.DEFAULT);
        ItemStack maskStack = new ItemStack(MythosItems.ONI_MASK_VISUAL.asItem());
        minecraft.getItemModelResolver().updateForLiving(this.maskRenderState, maskStack, ItemDisplayContext.HEAD, player);
        this.maskRenderState.submit(poseStack, submitNodeCollector, lightCoords, 0, 0);
        poseStack.popPose();
    }
}
