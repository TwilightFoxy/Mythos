package com.twily.mythos.client;

import com.twily.mythos.Mythos;
import com.twily.mythos.data.ShulkerbornInventoryData;
import com.twily.mythos.gameplay.ShulkerbornInventoryHandler;
import com.twily.mythos.network.ClickShulkerbornSlotPayload;
import com.twily.mythos.registry.MythosAttachments;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Field;

public final class ShulkerbornInventoryOverlay {

    private static final Identifier SLOT_SPRITE = Identifier.fromNamespaceAndPath("minecraft", "container/slot");
    private static final Field LEFT_POS_FIELD = getField("leftPos");
    private static final Field TOP_POS_FIELD = getField("topPos");

    private static final int SLOT_SIZE = 18;
    private static final int SLOT_COUNT = ShulkerbornInventoryData.SLOT_COUNT;
    private static final int ROW_X = 8;
    private static final int ROW_Y = 166;
    private static final int BACKGROUND_PADDING = 2;
    private static final int BACKGROUND_COLOR = 0xCC1B1D24;
    private static final int BORDER_COLOR = 0xFF4A4F63;
    private static final int HOVER_COLOR = 0x55FFFFFF;
    private static boolean overlayInteractionActive;
    private static int overlayInteractionButton = -1;

    public ShulkerbornInventoryOverlay() {
    }

    @SubscribeEvent
    public void onRenderScreen(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof InventoryScreen screen)) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !ShulkerbornInventoryHandler.isShulkerborn(minecraft.player)) {
            return;
        }

        int leftPos = getFieldValue(LEFT_POS_FIELD, screen);
        int topPos = getFieldValue(TOP_POS_FIELD, screen);
        int startX = leftPos + ROW_X;
        int startY = topPos + ROW_Y;

        GuiGraphicsExtractor graphics = event.getGuiGraphics();
        int backgroundX = startX - BACKGROUND_PADDING;
        int backgroundY = startY - BACKGROUND_PADDING;
        int backgroundWidth = SLOT_COUNT * SLOT_SIZE + BACKGROUND_PADDING * 2;
        int backgroundHeight = SLOT_SIZE + BACKGROUND_PADDING * 2;
        graphics.fill(backgroundX, backgroundY, backgroundX + backgroundWidth, backgroundY + backgroundHeight, BACKGROUND_COLOR);
        graphics.outline(backgroundX, backgroundY, backgroundWidth, backgroundHeight, BORDER_COLOR);

        ShulkerbornInventoryData data = minecraft.player.getData(MythosAttachments.SHULKERBORN_EXTRA_SLOTS);
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            int x = startX + slot * SLOT_SIZE;
            int y = startY;
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_SPRITE, x, y, SLOT_SIZE, SLOT_SIZE);

            ItemStack stack = data.get(slot);
            if (!stack.isEmpty()) {
                graphics.item(stack, x + 1, y + 1);
                graphics.itemDecorations(minecraft.font, stack, x + 1, y + 1);
            }

            if (isMouseOverSlot(x, y, event.getMouseX(), event.getMouseY())) {
                graphics.fill(x, y, x + 16, y + 16, HOVER_COLOR);
                if (!stack.isEmpty()) {
                    graphics.setTooltipForNextFrame(minecraft.font, stack, event.getMouseX(), event.getMouseY());
                }
            }
        }

        ItemStack carried = minecraft.player.containerMenu.getCarried();
        boolean mouseOverOverlay = event.getMouseX() >= backgroundX
            && event.getMouseX() < backgroundX + backgroundWidth
            && event.getMouseY() >= backgroundY
            && event.getMouseY() < backgroundY + backgroundHeight;
        if (!carried.isEmpty() && mouseOverOverlay) {
            int carriedX = (int) event.getMouseX() - 8;
            int carriedY = (int) event.getMouseY() - 8;
            graphics.item(carried, carriedX, carriedY);
            graphics.itemDecorations(minecraft.font, carried, carriedX, carriedY);
        }
    }

    @SubscribeEvent
    public void onMousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!(event.getScreen() instanceof InventoryScreen screen)) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !ShulkerbornInventoryHandler.isShulkerborn(minecraft.player)) {
            return;
        }

        int slot = slotAt(screen, event.getMouseX(), event.getMouseY());
        if (slot < 0) {
            return;
        }

        int button = event.getButton();
        if (button != 0 && button != 1) {
            return;
        }

        overlayInteractionActive = true;
        overlayInteractionButton = button;
        ClientPacketDistributor.sendToServer(new ClickShulkerbornSlotPayload(slot, button == 1, isShiftDown()));
        event.setCanceled(true);
    }

    @SubscribeEvent
    public void onMouseReleased(ScreenEvent.MouseButtonReleased.Pre event) {
        if (!(event.getScreen() instanceof InventoryScreen)) {
            return;
        }

        if (!overlayInteractionActive || event.getButton() != overlayInteractionButton) {
            return;
        }

        overlayInteractionActive = false;
        overlayInteractionButton = -1;
        event.setCanceled(true);
    }

    @SubscribeEvent
    public void onMouseDragged(ScreenEvent.MouseDragged.Pre event) {
        if (!(event.getScreen() instanceof InventoryScreen)) {
            return;
        }

        if (!overlayInteractionActive || event.getMouseButton() != overlayInteractionButton) {
            return;
        }

        event.setCanceled(true);
    }

    private static int slotAt(InventoryScreen screen, double mouseX, double mouseY) {
        int leftPos = getFieldValue(LEFT_POS_FIELD, screen);
        int topPos = getFieldValue(TOP_POS_FIELD, screen);
        int startX = leftPos + ROW_X;
        int startY = topPos + ROW_Y;

        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            int x = startX + slot * SLOT_SIZE;
            if (isMouseOverSlot(x, startY, mouseX, mouseY)) {
                return slot;
            }
        }

        return -1;
    }

    private static boolean isMouseOverSlot(int x, int y, double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16;
    }

    private static boolean isShiftDown() {
        long window = Minecraft.getInstance().getWindow().handle();
        return GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS
            || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
    }

    private static Field getField(String name) {
        try {
            Field field = AbstractContainerScreen.class.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to access AbstractContainerScreen." + name, exception);
        }
    }

    private static int getFieldValue(Field field, AbstractContainerScreen<?> screen) {
        try {
            return field.getInt(screen);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to read screen field " + field.getName(), exception);
        }
    }
}
