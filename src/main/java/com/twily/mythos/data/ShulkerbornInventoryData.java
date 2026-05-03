package com.twily.mythos.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public record ShulkerbornInventoryData(List<ItemStack> slots) {

    public static final int SLOT_COUNT = 9;
    private static final ShulkerbornInventoryData EMPTY = new ShulkerbornInventoryData(Collections.nCopies(SLOT_COUNT, ItemStack.EMPTY));

    public static final MapCodec<ShulkerbornInventoryData> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        ItemStack.OPTIONAL_CODEC.listOf().fieldOf("slots").forGetter(ShulkerbornInventoryData::slots)
    ).apply(instance, ShulkerbornInventoryData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, ShulkerbornInventoryData> STREAM_CODEC = StreamCodec.composite(
        ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list()),
        ShulkerbornInventoryData::slots,
        ShulkerbornInventoryData::new
    );

    public ShulkerbornInventoryData {
        slots = normalize(slots);
    }

    public static ShulkerbornInventoryData empty() {
        return EMPTY;
    }

    public ItemStack get(int index) {
        return this.slots.get(index);
    }

    public ShulkerbornInventoryData withSlot(int index, ItemStack stack) {
        List<ItemStack> updated = new ArrayList<>(this.slots);
        updated.set(index, sanitize(stack));
        return new ShulkerbornInventoryData(updated);
    }

    public boolean isEmpty() {
        return this.slots.stream().allMatch(ItemStack::isEmpty);
    }

    private static List<ItemStack> normalize(List<ItemStack> raw) {
        List<ItemStack> normalized = new ArrayList<>(SLOT_COUNT);
        if (raw != null) {
            for (ItemStack stack : raw) {
                if (normalized.size() >= SLOT_COUNT) {
                    break;
                }
                normalized.add(sanitize(stack));
            }
        }

        while (normalized.size() < SLOT_COUNT) {
            normalized.add(ItemStack.EMPTY);
        }

        return List.copyOf(normalized);
    }

    private static ItemStack sanitize(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        return stack.copy();
    }
}
