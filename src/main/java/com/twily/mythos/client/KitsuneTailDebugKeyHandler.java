package com.twily.mythos.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.twily.mythos.registry.MythosItems;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

public final class KitsuneTailDebugKeyHandler {

    private static final KeyMapping MOVE_X_NEG = new KeyMapping("key.mythos.kitsune_tail_debug_x_neg", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_J, MythosKeyCategory.MYTHOS);
    private static final KeyMapping MOVE_X_POS = new KeyMapping("key.mythos.kitsune_tail_debug_x_pos", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_L, MythosKeyCategory.MYTHOS);
    private static final KeyMapping MOVE_Y_POS = new KeyMapping("key.mythos.kitsune_tail_debug_y_pos", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_I, MythosKeyCategory.MYTHOS);
    private static final KeyMapping MOVE_Y_NEG = new KeyMapping("key.mythos.kitsune_tail_debug_y_neg", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, MythosKeyCategory.MYTHOS);
    private static final KeyMapping MOVE_Z_NEG = new KeyMapping("key.mythos.kitsune_tail_debug_z_neg", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_U, MythosKeyCategory.MYTHOS);
    private static final KeyMapping MOVE_Z_POS = new KeyMapping("key.mythos.kitsune_tail_debug_z_pos", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_O, MythosKeyCategory.MYTHOS);
    private static final KeyMapping ROT_X_NEG = new KeyMapping("key.mythos.kitsune_tail_debug_rx_neg", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_7, MythosKeyCategory.MYTHOS);
    private static final KeyMapping ROT_X_POS = new KeyMapping("key.mythos.kitsune_tail_debug_rx_pos", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_9, MythosKeyCategory.MYTHOS);
    private static final KeyMapping ROT_Y_NEG = new KeyMapping("key.mythos.kitsune_tail_debug_ry_neg", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_4, MythosKeyCategory.MYTHOS);
    private static final KeyMapping ROT_Y_POS = new KeyMapping("key.mythos.kitsune_tail_debug_ry_pos", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_6, MythosKeyCategory.MYTHOS);
    private static final KeyMapping ROT_Z_NEG = new KeyMapping("key.mythos.kitsune_tail_debug_rz_neg", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_1, MythosKeyCategory.MYTHOS);
    private static final KeyMapping ROT_Z_POS = new KeyMapping("key.mythos.kitsune_tail_debug_rz_pos", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_3, MythosKeyCategory.MYTHOS);
    private static final KeyMapping STEP_DOWN = new KeyMapping("key.mythos.kitsune_tail_debug_step_down", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_BRACKET, MythosKeyCategory.MYTHOS);
    private static final KeyMapping STEP_UP = new KeyMapping("key.mythos.kitsune_tail_debug_step_up", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_BRACKET, MythosKeyCategory.MYTHOS);
    private static final KeyMapping RESET = new KeyMapping("key.mythos.kitsune_tail_debug_reset", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_P, MythosKeyCategory.MYTHOS);
    private static final KeyMapping SHOW = new KeyMapping("key.mythos.kitsune_tail_debug_show", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_H, MythosKeyCategory.MYTHOS);

    private KitsuneTailDebugKeyHandler() {
    }

    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(MOVE_X_NEG);
        event.register(MOVE_X_POS);
        event.register(MOVE_Y_POS);
        event.register(MOVE_Y_NEG);
        event.register(MOVE_Z_NEG);
        event.register(MOVE_Z_POS);
        event.register(ROT_X_NEG);
        event.register(ROT_X_POS);
        event.register(ROT_Y_NEG);
        event.register(ROT_Y_POS);
        event.register(ROT_Z_NEG);
        event.register(ROT_Z_POS);
        event.register(STEP_DOWN);
        event.register(STEP_UP);
        event.register(RESET);
        event.register(SHOW);
    }

    public static final class Handler {

        private Handler() {
        }

        @SubscribeEvent
        public static void onKeyInput(InputEvent.Key event) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == null || minecraft.screen != null || !isHoldingDebugTool(minecraft.player)) {
                return;
            }

            while (MOVE_X_NEG.consumeClick()) {
                KitsuneTailDebugState.nudgeX(-KitsuneTailDebugState.step());
                show(minecraft);
            }
            while (MOVE_X_POS.consumeClick()) {
                KitsuneTailDebugState.nudgeX(KitsuneTailDebugState.step());
                show(minecraft);
            }
            while (MOVE_Y_POS.consumeClick()) {
                KitsuneTailDebugState.nudgeY(KitsuneTailDebugState.step());
                show(minecraft);
            }
            while (MOVE_Y_NEG.consumeClick()) {
                KitsuneTailDebugState.nudgeY(-KitsuneTailDebugState.step());
                show(minecraft);
            }
            while (MOVE_Z_NEG.consumeClick()) {
                KitsuneTailDebugState.nudgeZ(-KitsuneTailDebugState.step());
                show(minecraft);
            }
            while (MOVE_Z_POS.consumeClick()) {
                KitsuneTailDebugState.nudgeZ(KitsuneTailDebugState.step());
                show(minecraft);
            }
            while (ROT_X_NEG.consumeClick()) {
                KitsuneTailDebugState.nudgeRotX(-5.0F);
                show(minecraft);
            }
            while (ROT_X_POS.consumeClick()) {
                KitsuneTailDebugState.nudgeRotX(5.0F);
                show(minecraft);
            }
            while (ROT_Y_NEG.consumeClick()) {
                KitsuneTailDebugState.nudgeRotY(-5.0F);
                show(minecraft);
            }
            while (ROT_Y_POS.consumeClick()) {
                KitsuneTailDebugState.nudgeRotY(5.0F);
                show(minecraft);
            }
            while (ROT_Z_NEG.consumeClick()) {
                KitsuneTailDebugState.nudgeRotZ(-5.0F);
                show(minecraft);
            }
            while (ROT_Z_POS.consumeClick()) {
                KitsuneTailDebugState.nudgeRotZ(5.0F);
                show(minecraft);
            }
            while (STEP_DOWN.consumeClick()) {
                KitsuneTailDebugState.decreaseStep();
                show(minecraft);
            }
            while (STEP_UP.consumeClick()) {
                KitsuneTailDebugState.increaseStep();
                show(minecraft);
            }
            while (RESET.consumeClick()) {
                KitsuneTailDebugState.reset();
                show(minecraft);
            }
            while (SHOW.consumeClick()) {
                show(minecraft);
            }
        }

        private static boolean isHoldingDebugTool(Player player) {
            return isTool(player.getMainHandItem()) || isTool(player.getOffhandItem());
        }

        private static boolean isTool(ItemStack stack) {
            return stack.is(MythosItems.KITSUNE_TAIL_TUNER.asItem());
        }

        private static void show(Minecraft minecraft) {
            minecraft.gui.setOverlayMessage(Component.literal("Tail debug: " + KitsuneTailDebugState.summary()), false);
        }
    }
}
