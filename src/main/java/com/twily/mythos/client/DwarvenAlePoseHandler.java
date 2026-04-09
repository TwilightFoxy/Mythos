package com.twily.mythos.client;

import com.twily.mythos.Mythos;
import com.twily.mythos.registry.MythosItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;

@EventBusSubscriber(modid = Mythos.MOD_ID, value = Dist.CLIENT)
public final class DwarvenAlePoseHandler {

    private static final float RAISED_ARM_X_ROT = -1.57F;
    private static final float RAISED_ARM_Y_ROT = 0.08F;

    private DwarvenAlePoseHandler() {
    }

    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Pre<?> event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        if (!(minecraft.level.getEntity(event.getRenderState().id) instanceof Player player)) {
            return;
        }

        PlayerModel model = event.getRenderer().getModel();
        poseArm(model, HumanoidArm.RIGHT, player.getMainArm() == HumanoidArm.RIGHT ? player.getMainHandItem() : player.getOffhandItem());
        poseArm(model, HumanoidArm.LEFT, player.getMainArm() == HumanoidArm.LEFT ? player.getMainHandItem() : player.getOffhandItem());
    }

    private static void poseArm(PlayerModel model, HumanoidArm arm, ItemStack stack) {
        if (!stack.is(MythosItems.DWARVEN_ALE.asItem())) {
            return;
        }

        if (arm == HumanoidArm.RIGHT) {
            model.rightArm.xRot = Math.min(model.rightArm.xRot, RAISED_ARM_X_ROT);
            model.rightArm.yRot -= RAISED_ARM_Y_ROT;
            model.rightSleeve.xRot = model.rightArm.xRot;
            model.rightSleeve.yRot = model.rightArm.yRot;
            model.rightSleeve.zRot = model.rightArm.zRot;
        } else {
            model.leftArm.xRot = Math.min(model.leftArm.xRot, RAISED_ARM_X_ROT);
            model.leftArm.yRot += RAISED_ARM_Y_ROT;
            model.leftSleeve.xRot = model.leftArm.xRot;
            model.leftSleeve.yRot = model.leftArm.yRot;
            model.leftSleeve.zRot = model.leftArm.zRot;
        }
    }
}
