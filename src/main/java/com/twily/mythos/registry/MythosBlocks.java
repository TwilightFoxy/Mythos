package com.twily.mythos.registry;

import com.twily.mythos.Mythos;
import com.twily.mythos.world.block.FoxLanternBlock;
import com.twily.mythos.world.block.KitsuneFireBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class MythosBlocks {

    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Mythos.MOD_ID);
    private static final ResourceKey<Block> KITSUNE_FIRE_KEY =
        ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "kitsune_fire"));
    private static final ResourceKey<Block> FOX_LANTERN_KEY =
        ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "fox_lantern"));

    public static final DeferredBlock<Block> KITSUNE_FIRE = BLOCKS.register(
        "kitsune_fire",
        () -> new KitsuneFireBlock(
                BlockBehaviour.Properties.of()
                .setId(KITSUNE_FIRE_KEY)
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
                .setId(FOX_LANTERN_KEY)
                .lightLevel(state -> 12)
        )
    );

    private MythosBlocks() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
    }
}
