package com.twily.mythos.world.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public final class SpiritStepBlock extends Block {

    public static final MapCodec<SpiritStepBlock> CODEC = simpleCodec(SpiritStepBlock::new);
    public static final int LIFETIME_TICKS = 20 * 30;

    public SpiritStepBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<SpiritStepBlock> codec() {
        return CODEC;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide()) {
            level.scheduleTick(pos, this, LIFETIME_TICKS);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.getBlockState(pos).is(this)) {
            level.removeBlock(pos, false);
        }
    }

    public static void refreshLifetime(ServerLevel level, BlockPos pos, Block block) {
        level.scheduleTick(pos, block, LIFETIME_TICKS);
    }
}
