package com.twily.mythos.client;

import com.twily.mythos.Mythos;
import com.twily.mythos.registry.MythosItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;

@EventBusSubscriber(modid = Mythos.MOD_ID, value = Dist.CLIENT)
public final class DwarvenAlePoseHandler {

    private static final float DRINK_START_X_ROT = -0.95F;
    private static final float DRINK_END_X_ROT = -1.65F;
    private static final float DRINK_Y_ROT = 0.24F;
    private static final float DRINK_Z_ROT = 0.12F;

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

        if (!player.isUsingItem() || !player.getUseItem().is(MythosItems.DWARVEN_ALE.asItem())) {
            return;
        }

        PlayerModel model = event.getRenderer().getModel();
        HumanoidArm usedArm = armForUse(player);
        float progress = drinkProgress(player);
        poseArm(model, usedArm, progress);
    }

    private static void poseArm(PlayerModel model, HumanoidArm arm, float progress) {
        float xRot = lerp(progress, DRINK_START_X_ROT, DRINK_END_X_ROT);
        if (arm == HumanoidArm.RIGHT) {
            model.rightArm.xRot = Math.min(model.rightArm.xRot, xRot);
            model.rightArm.yRot -= DRINK_Y_ROT;
            model.rightArm.zRot += DRINK_Z_ROT;
            model.rightSleeve.xRot = model.rightArm.xRot;
            model.rightSleeve.yRot = model.rightArm.yRot;
            model.rightSleeve.zRot = model.rightArm.zRot;
        } else {
            model.leftArm.xRot = Math.min(model.leftArm.xRot, xRot);
            model.leftArm.yRot += DRINK_Y_ROT;
            model.leftArm.zRot -= DRINK_Z_ROT;
            model.leftSleeve.xRot = model.leftArm.xRot;
            model.leftSleeve.yRot = model.leftArm.yRot;
            model.leftSleeve.zRot = model.leftArm.zRot;
        }
    }

    private static HumanoidArm armForUse(Player player) {
        return switch (player.getUsedItemHand()) {
            case MAIN_HAND -> player.getMainArm();
            case OFF_HAND -> player.getMainArm() == HumanoidArm.RIGHT ? HumanoidArm.LEFT : HumanoidArm.RIGHT;
        };
    }

    private static float drinkProgress(Player player) {
        int total = player.getUseItem().getUseDuration(player);
        if (total <= 0) {
            return 1.0F;
        }

        int elapsed = total - player.getUseItemRemainingTicks();
        float raw = Math.min(1.0F, elapsed / (float) total);
        // Ease in so the arm lifts naturally instead of snapping to the mouth.
        return raw * raw * (3.0F - 2.0F * raw);
    }

    private static float lerp(float delta, float start, float end) {
        return start + (end - start) * delta;
    }
}
