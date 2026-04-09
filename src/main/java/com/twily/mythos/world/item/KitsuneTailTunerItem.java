package com.twily.mythos.world.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

public final class KitsuneTailTunerItem extends Item {

    public KitsuneTailTunerItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack itemStack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag tooltipFlag) {
        builder.accept(Component.translatable("tooltip.mythos.kitsune_tail_tuner.line1"));
        builder.accept(Component.translatable("tooltip.mythos.kitsune_tail_tuner.line2"));
        builder.accept(Component.translatable("tooltip.mythos.kitsune_tail_tuner.line3"));
    }
}
