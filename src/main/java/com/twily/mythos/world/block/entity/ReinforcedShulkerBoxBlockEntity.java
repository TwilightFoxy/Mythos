package com.twily.mythos.world.block.entity;

import com.twily.mythos.registry.MythosBlockEntities;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.core.BlockPos;

import java.lang.reflect.Field;
import java.util.stream.IntStream;

public final class ReinforcedShulkerBoxBlockEntity extends ShulkerBoxBlockEntity {

    public static final int CONTAINER_SIZE = 54;
    private static final int[] SLOTS = IntStream.range(0, CONTAINER_SIZE).toArray();
    private static final Field TYPE_FIELD = resolveTypeField();

    private NonNullList<ItemStack> itemStacks = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);

    public ReinforcedShulkerBoxBlockEntity(BlockPos pos, BlockState state) {
        super(pos, state);
        overrideType(MythosBlockEntities.REINFORCED_SHULKER_BOX.get());
    }

    @Override
    public int getContainerSize() {
        return this.itemStacks.size();
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.overrideType(MythosBlockEntities.REINFORCED_SHULKER_BOX.get());
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (!this.trySaveLootTable(output)) {
            ContainerHelper.saveAllItems(output, this.itemStacks, false);
        }
    }

    @Override
    public void loadFromTag(ValueInput input) {
        this.itemStacks = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        if (!this.tryLoadLootTable(input)) {
            ContainerHelper.loadAllItems(input, this.itemStacks);
        }
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return this.itemStacks;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.itemStacks = items;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return SLOTS;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.mythos.reinforced_shulker_box");
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return ChestMenu.sixRows(containerId, inventory, this);
    }

    private void overrideType(BlockEntityType<?> type) {
        try {
            TYPE_FIELD.set(this, type);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Failed to override reinforced shulker block entity type", exception);
        }
    }

    private static Field resolveTypeField() {
        try {
            Field field = BlockEntity.class.getDeclaredField("type");
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to access BlockEntity.type", exception);
        }
    }
}
