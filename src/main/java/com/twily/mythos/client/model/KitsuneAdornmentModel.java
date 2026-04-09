package com.twily.mythos.client.model;

import com.twily.mythos.Mythos;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.Identifier;

public final class KitsuneAdornmentModel extends EntityModel<AvatarRenderState> {

    public static final ModelLayerLocation TAIL_LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "kitsune_tail"), "main");
    public static final ModelLayerLocation FOXFIRE_LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "kitsune_foxfire"), "main");

    public KitsuneAdornmentModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createTailLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild(
            "main",
            CubeListBuilder.create()
                .texOffs(30, 0)
                .addBox(-3.0F, -1.0F, 0.0F, 6.0F, 3.0F, 1.0F)
                .texOffs(24, 4)
                .addBox(-3.5F, -1.5F, 1.0F, 7.0F, 4.0F, 1.0F)
                .texOffs(18, 9)
                .addBox(-4.0F, -2.0F, 2.0F, 8.0F, 5.0F, 2.0F)
                .texOffs(12, 16)
                .addBox(-4.5F, -2.5F, 4.0F, 9.0F, 6.0F, 2.0F)
                .texOffs(4, 24)
                .addBox(-5.0F, -3.0F, 6.0F, 10.0F, 7.0F, 10.0F)
                .texOffs(12, 16)
                .addBox(-4.5F, -2.5F, 16.0F, 9.0F, 6.0F, 1.0F)
                .texOffs(18, 9)
                .addBox(-4.0F, -2.0F, 17.0F, 8.0F, 5.0F, 1.0F)
                .texOffs(24, 4)
                .addBox(-3.5F, -1.5F, 18.0F, 7.0F, 4.0F, 1.0F)
                .texOffs(30, 0)
                .addBox(-3.0F, -1.0F, 19.0F, 6.0F, 3.0F, 1.0F)
                .texOffs(32, 4)
                .addBox(-2.5F, -0.5F, 20.0F, 5.0F, 2.0F, 1.0F)
                .texOffs(34, 7)
                .addBox(-2.0F, 0.0F, 21.0F, 4.0F, 1.0F, 1.0F),
            PartPose.ZERO
        );
        return LayerDefinition.create(mesh, 48, 32);
    }

    public static LayerDefinition createFoxFireLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild(
            "main",
            CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(-2.0F, 0.0F, -2.0F, 4.0F, 3.0F, 4.0F)
                .texOffs(0, 0)
                .addBox(-1.0F, 3.0F, -2.0F, 3.0F, 1.0F, 1.0F)
                .texOffs(0, 0)
                .addBox(-2.0F, 3.0F, -1.0F, 4.0F, 1.0F, 3.0F)
                .texOffs(0, 0)
                .addBox(-1.0F, 4.0F, -1.0F, 2.0F, 3.0F, 2.0F)
                .texOffs(0, 0)
                .addBox(0.0F, 7.0F, -1.0F, 1.0F, 1.0F, 1.0F),
            PartPose.ZERO
        );
        return LayerDefinition.create(mesh, 8, 8);
    }

    @Override
    public void setupAnim(AvatarRenderState state) {
    }
}
