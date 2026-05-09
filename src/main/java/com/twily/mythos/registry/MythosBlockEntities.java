package com.twily.mythos.registry;

import com.twily.mythos.Mythos;
import com.twily.mythos.world.block.entity.ReinforcedShulkerBoxBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.event.BlockEntityTypeAddBlocksEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.lang.reflect.Constructor;
import java.util.Set;

public final class MythosBlockEntities {

    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
        DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Mythos.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ReinforcedShulkerBoxBlockEntity>> REINFORCED_SHULKER_BOX =
        BLOCK_ENTITY_TYPES.register(
            "reinforced_shulker_box",
            () -> createType(ReinforcedShulkerBoxBlockEntity::new, MythosBlocks.reinforcedShulkerBlocks())
        );

    private MythosBlockEntities() {
    }

    public static void register(IEventBus modBus) {
        BLOCK_ENTITY_TYPES.register(modBus);
    }

    public static void addValidBlocks(BlockEntityTypeAddBlocksEvent event) {
        event.modify(BlockEntityType.SHULKER_BOX, MythosBlocks.reinforcedShulkerBlocks());
    }

    @SuppressWarnings("unchecked")
    private static <T extends net.minecraft.world.level.block.entity.BlockEntity> BlockEntityType<T> createType(
        BlockEntityType.BlockEntitySupplier<? extends T> factory,
        net.minecraft.world.level.block.Block... validBlocks
    ) {
        try {
            Constructor<BlockEntityType> constructor = BlockEntityType.class.getDeclaredConstructor(BlockEntityType.BlockEntitySupplier.class, Set.class);
            constructor.setAccessible(true);
            return constructor.newInstance(factory, Set.of(validBlocks));
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to create block entity type for mythos:reinforced_shulker_box", exception);
        }
    }
}
