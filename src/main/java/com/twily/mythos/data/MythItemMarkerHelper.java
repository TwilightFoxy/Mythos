package com.twily.mythos.data;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class MythItemMarkerHelper {

    private MythItemMarkerHelper() {
    }

    public static boolean hasMarker(ItemStack stack, String marker) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        return customData != null && customData.contains(marker);
    }

    public static void setMarker(ItemStack stack, String marker) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putBoolean(marker, true));
    }
}
