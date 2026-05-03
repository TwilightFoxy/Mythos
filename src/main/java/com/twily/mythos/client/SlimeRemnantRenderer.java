package com.twily.mythos.client;

import com.mojang.authlib.GameProfile;
import com.twily.mythos.world.entity.SlimeRemnantEntity;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.item.component.ResolvableProfile;
import com.mojang.blaze3d.vertex.PoseStack;

import java.util.UUID;

public final class SlimeRemnantRenderer extends LivingEntityRenderer<SlimeRemnantEntity, SlimeRemnantRenderState, PlayerModel> {

    private static final UUID DEFAULT_SKIN_UUID = UUID.fromString("72d0ed0f-8f46-4f44-aaf0-0ccdd26f22e0");

    private final PlayerModel wideModel;
    private final PlayerModel slimModel;
    private final PlayerSkinRenderCache playerSkinRenderCache;

    public SlimeRemnantRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
        this.wideModel = this.getModel();
        this.slimModel = new PlayerModel(context.bakeLayer(ModelLayers.PLAYER_SLIM), true);
        this.playerSkinRenderCache = context.getPlayerSkinRenderCache();
    }

    @Override
    public SlimeRemnantRenderState createRenderState() {
        return new SlimeRemnantRenderState();
    }

    @Override
    public void extractRenderState(SlimeRemnantEntity entity, SlimeRemnantRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        HumanoidMobRenderer.extractHumanoidRenderState(entity, state, partialTick, this.itemModelResolver);
        state.id = entity.getId();
        state.slimeStage = entity.getSlimeStage();
        state.skin = resolvePlayerSkin(entity.getAppearanceProfile());
        state.showHat = true;
        state.showJacket = true;
        state.showLeftPants = true;
        state.showRightPants = true;
        state.showLeftSleeve = true;
        state.showRightSleeve = true;
        state.showCape = false;
        state.isSpectator = false;
    }

    @Override
    public Identifier getTextureLocation(SlimeRemnantRenderState state) {
        return state.skin.body().texturePath();
    }

    @Override
    public void submit(SlimeRemnantRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, net.minecraft.client.renderer.state.level.CameraRenderState cameraState) {
        this.model = state.skin.model() == PlayerModelType.SLIM ? this.slimModel : this.wideModel;
        super.submit(state, poseStack, submitNodeCollector, cameraState);
    }

    private net.minecraft.world.entity.player.PlayerSkin resolvePlayerSkin(ResolvableProfile profile) {
        if (profile == null) {
            return DefaultPlayerSkin.get(DEFAULT_SKIN_UUID);
        }

        PlayerSkinRenderCache.RenderInfo info = this.playerSkinRenderCache.getOrDefault(profile);
        if (info != null) {
            return info.playerSkin();
        }

        GameProfile partial = profile.partialProfile();
        return partial != null ? DefaultPlayerSkin.get(partial) : DefaultPlayerSkin.get(DEFAULT_SKIN_UUID);
    }
}
