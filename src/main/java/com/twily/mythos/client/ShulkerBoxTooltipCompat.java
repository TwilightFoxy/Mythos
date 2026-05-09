package com.twily.mythos.client;

import com.twily.mythos.Mythos;
import com.twily.mythos.registry.MythosItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.neoforged.fml.ModList;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.List;

public final class ShulkerBoxTooltipCompat {

    private static final String MOD_ID = "shulkerboxtooltip";
    private static final int REINFORCED_SHULKER_BOX_SIZE = 54;

    private ShulkerBoxTooltipCompat() {
    }

    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(ShulkerBoxTooltipCompat::registerProviders);
    }

    private static void registerProviders() {
        if (!ModList.get().isLoaded(MOD_ID)) {
            return;
        }

        try {
            Class<?> registryClass = Class.forName("com.misterpemodder.shulkerboxtooltip.api.provider.PreviewProviderRegistry");
            Class<?> providerClass = Class.forName("com.misterpemodder.shulkerboxtooltip.api.provider.PreviewProvider");
            Object registry = registryClass.getMethod("getInstance").invoke(null);
            Object provider = Proxy.newProxyInstance(
                providerClass.getClassLoader(),
                new Class<?>[]{providerClass},
                new ReinforcedShulkerPreviewHandler()
            );

            Method register = registryClass.getMethod("register", Identifier.class, providerClass, Item[].class);
            register.invoke(
                registry,
                Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "reinforced_shulker_box"),
                provider,
                MythosItems.reinforcedShulkerBoxItems()
            );
        } catch (ReflectiveOperationException exception) {
            fallbackToVanillaProvider();
        }
    }

    private static void fallbackToVanillaProvider() {
        try {
            Class<?> registryClass = Class.forName("com.misterpemodder.shulkerboxtooltip.api.provider.PreviewProviderRegistry");
            Class<?> providerClass = Class.forName("com.misterpemodder.shulkerboxtooltip.api.provider.PreviewProvider");
            Object registry = registryClass.getMethod("getInstance").invoke(null);
            Object provider = registryClass.getMethod("get", Item.class).invoke(registry, net.minecraft.world.item.Items.SHULKER_BOX);
            if (provider == null) {
                return;
            }

            Method register = registryClass.getMethod("register", Identifier.class, providerClass, Item[].class);
            register.invoke(
                registry,
                Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "reinforced_shulker_box_fallback"),
                provider,
                MythosItems.reinforcedShulkerBoxItems()
            );
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private static final class ReinforcedShulkerPreviewHandler implements InvocationHandler {

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String name = method.getName();
            return switch (name) {
                case "shouldDisplay" -> shouldDisplay(args);
                case "getInventory" -> getInventory(args);
                case "getInventoryMaxSize" -> REINFORCED_SHULKER_BOX_SIZE;
                case "getMaxRowSize", "getCompactMaxRowSize" -> 9;
                case "isFullPreviewAvailable", "showTooltipHints" -> true;
                case "getTooltipHintLangKey" -> "text.shulkerboxtooltip.openHint";
                case "getFullTooltipHintLangKey" -> "text.shulkerboxtooltip.fullOpenHint";
                case "getLockKeyTooltipHintLangKey" -> "text.shulkerboxtooltip.lockHint";
                case "getWindowColorKey" -> resolveColorKey(args);
                case "addTooltip" -> Collections.emptyList();
                case "getRenderer", "getTextureOverride" -> null;
                case "getPriority" -> 100;
                case "onInventoryAccessStart" -> null;
                default -> defaultValue(method.getReturnType());
            };
        }

        private static boolean shouldDisplay(Object[] args) throws ReflectiveOperationException {
            ItemStack stack = stackFromContext(args);
            return stack.has(DataComponents.CONTAINER) || stack.has(DataComponents.CONTAINER_LOOT);
        }

        private static List<ItemStack> getInventory(Object[] args) throws ReflectiveOperationException {
            ItemStack stack = stackFromContext(args);
            ItemContainerContents contents = stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
            return contents.allItemsCopyStream().toList();
        }

        private static Object resolveColorKey(Object[] args) throws ReflectiveOperationException {
            ItemStack stack = stackFromContext(args);
            Block block = Block.byItem(stack.getItem());
            String fieldName = "GENERIC_CONTAINER";
            if (block instanceof ShulkerBoxBlock shulkerBox) {
                if (shulkerBox.getColor() == null) {
                    fieldName = "SHULKER_BOX";
                } else {
                    fieldName = switch (shulkerBox.getColor()) {
                    case WHITE -> "WHITE_SHULKER_BOX";
                    case ORANGE -> "ORANGE_SHULKER_BOX";
                    case MAGENTA -> "MAGENTA_SHULKER_BOX";
                    case LIGHT_BLUE -> "LIGHT_BLUE_SHULKER_BOX";
                    case YELLOW -> "YELLOW_SHULKER_BOX";
                    case LIME -> "LIME_SHULKER_BOX";
                    case PINK -> "PINK_SHULKER_BOX";
                    case GRAY -> "GRAY_SHULKER_BOX";
                    case LIGHT_GRAY -> "LIGHT_GRAY_SHULKER_BOX";
                    case CYAN -> "CYAN_SHULKER_BOX";
                    case PURPLE -> "PURPLE_SHULKER_BOX";
                    case BLUE -> "BLUE_SHULKER_BOX";
                    case BROWN -> "BROWN_SHULKER_BOX";
                    case GREEN -> "GREEN_SHULKER_BOX";
                    case RED -> "RED_SHULKER_BOX";
                    case BLACK -> "BLACK_SHULKER_BOX";
                    };
                }
            }

            Class<?> colorKeyClass = Class.forName("com.misterpemodder.shulkerboxtooltip.api.color.ColorKey");
            return colorKeyClass.getField(fieldName).get(null);
        }

        private static ItemStack stackFromContext(Object[] args) throws ReflectiveOperationException {
            Object context = args[0];
            return (ItemStack) context.getClass().getMethod("stack").invoke(context);
        }

        private static Object defaultValue(Class<?> returnType) {
            if (returnType == boolean.class) {
                return false;
            }
            if (returnType == int.class) {
                return 0;
            }
            return null;
        }
    }
}
