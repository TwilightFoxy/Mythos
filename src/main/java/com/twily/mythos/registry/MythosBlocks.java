package com.twily.mythos.registry;

import com.twily.mythos.Mythos;
import com.twily.mythos.world.block.FoxLanternBlock;
import com.twily.mythos.world.block.KitsuneFireBlock;
import com.twily.mythos.world.block.ReinforcedShulkerBoxBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Map;

public final class MythosBlocks {

    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Mythos.MOD_ID);
    private static final Map<DyeColor, DeferredBlock<Block>> DYED_REINFORCED_SHULKER_BOXES = registerDyedReinforcedShulkerBoxes();

    public static final DeferredBlock<Block> KITSUNE_FIRE = BLOCKS.register(
        "kitsune_fire",
        () -> new KitsuneFireBlock(
                BlockBehaviour.Properties.of()
                .setId(blockKey("kitsune_fire"))
                .replaceable()
                .noCollision()
                .instabreak()
                .lightLevel(state -> 10)
                .noLootTable()
        )
    );

    public static final DeferredBlock<Block> FOX_LANTERN = BLOCKS.register(
        "fox_lantern",
        () -> new FoxLanternBlock(
            BlockBehaviour.Properties.ofFullCopy(Blocks.SOUL_LANTERN)
                .setId(blockKey("fox_lantern"))
                .lightLevel(state -> 12)
        )
    );

    public static final DeferredBlock<Block> REINFORCED_SHULKER_BOX = registerReinforcedShulkerBox("reinforced_shulker_box", null);

    private MythosBlocks() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
    }

    public static DeferredBlock<Block> reinforcedShulkerBox(DyeColor color) {
        return DYED_REINFORCED_SHULKER_BOXES.get(color);
    }

    public static Block[] reinforcedShulkerBlocks() {
        ArrayList<Block> blocks = new ArrayList<>(1 + DYED_REINFORCED_SHULKER_BOXES.size());
        blocks.add(REINFORCED_SHULKER_BOX.get());
        for (DeferredBlock<Block> block : DYED_REINFORCED_SHULKER_BOXES.values()) {
            blocks.add(block.get());
        }
        return blocks.toArray(Block[]::new);
    }

    private static Map<DyeColor, DeferredBlock<Block>> registerDyedReinforcedShulkerBoxes() {
        EnumMap<DyeColor, DeferredBlock<Block>> blocks = new EnumMap<>(DyeColor.class);
        for (DyeColor color : DyeColor.values()) {
            blocks.put(color, registerReinforcedShulkerBox(color.getName() + "_reinforced_shulker_box", color));
        }
        return Map.copyOf(blocks);
    }

    private static DeferredBlock<Block> registerReinforcedShulkerBox(String name, DyeColor color) {
        return BLOCKS.register(
            name,
            () -> new ReinforcedShulkerBoxBlock(
                color,
                BlockBehaviour.Properties.ofFullCopy(Blocks.SHULKER_BOX)
                    .setId(blockKey(name))
            )
        );
    }

    private static ResourceKey<Block> blockKey(String path) {
        return ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(Mythos.MOD_ID, path));
    }
}
