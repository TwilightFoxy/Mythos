package com.twily.mythos.world.block;

import com.mojang.serialization.MapCodec;
import com.twily.mythos.registry.MythosBlockEntities;
import com.twily.mythos.world.block.entity.ReinforcedShulkerBoxBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public final class ReinforcedShulkerBoxBlock extends ShulkerBoxBlock {

    public static final MapCodec<ShulkerBoxBlock> CODEC = simpleCodec(ReinforcedShulkerBoxBlock::new);

    public ReinforcedShulkerBoxBlock(BlockBehaviour.Properties properties) {
        this(null, properties);
    }

    public ReinforcedShulkerBoxBlock(DyeColor color, BlockBehaviour.Properties properties) {
        super(color, properties);
    }

    @Override
    public MapCodec<ShulkerBoxBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ReinforcedShulkerBoxBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return createTickerHelper(blockEntityType, MythosBlockEntities.REINFORCED_SHULKER_BOX.get(), ReinforcedShulkerBoxBlockEntity::tick);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        super.animateTick(state, level, pos, random);
        if (random.nextInt(7) != 0) {
            return;
        }

        double x = pos.getX() + 0.15D + random.nextDouble() * 0.7D;
        double y = pos.getY() + 0.2D + random.nextDouble() * 0.65D;
        double z = pos.getZ() + 0.15D + random.nextDouble() * 0.7D;
        double velocityX = (random.nextDouble() - 0.5D) * 0.03D;
        double velocityY = (random.nextDouble() - 0.5D) * 0.03D;
        double velocityZ = (random.nextDouble() - 0.5D) * 0.03D;
        level.addParticle(ParticleTypes.ENCHANT, x, y, z, velocityX, velocityY, velocityZ);
    }
}
