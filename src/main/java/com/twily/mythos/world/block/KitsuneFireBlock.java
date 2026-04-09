package com.twily.mythos.world.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public final class KitsuneFireBlock extends BaseFireBlock {

    public static final MapCodec<KitsuneFireBlock> CODEC = simpleCodec(KitsuneFireBlock::new);

    public KitsuneFireBlock(BlockBehaviour.Properties properties) {
        super(properties, 2.0F);
    }

    @Override
    public MapCodec<KitsuneFireBlock> codec() {
        return CODEC;
    }

    @Override
    protected BlockState updateShape(
        BlockState state,
        LevelReader level,
        ScheduledTickAccess ticks,
        BlockPos pos,
        Direction directionToNeighbour,
        BlockPos neighbourPos,
        BlockState neighbourState,
        RandomSource random
    ) {
        return this.canSurvive(state, level, pos) ? this.defaultBlockState() : Blocks.AIR.defaultBlockState();
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        BlockState belowState = level.getBlockState(below);
        return belowState.isFaceSturdy(level, below, Direction.UP) || this.canBurn(belowState);
    }

    @Override
    protected boolean canBurn(BlockState state) {
        return true;
    }
}
