package com.twily.mythos.world.block;

import com.mojang.serialization.MapCodec;
import com.twily.mythos.gameplay.StarWandererMythHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

public final class AstralLanternBlock extends LanternBlock {

    public static final MapCodec<AstralLanternBlock> CODEC = simpleCodec(AstralLanternBlock::new);
    public static final IntegerProperty RANGE = IntegerProperty.create("range", 0, 3);
    private static final int[] RADII = {8, 16, 24, 32};

    public AstralLanternBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(HANGING, false)
            .setValue(WATERLOGGED, false)
            .setValue(RANGE, 1));
    }

    @Override
    public MapCodec<AstralLanternBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(HANGING, WATERLOGGED, RANGE);
    }

    public static int radius(BlockState state) {
        return RADII[state.getValue(RANGE)];
    }

    public static BlockState cycleRange(BlockState state) {
        int next = (state.getValue(RANGE) + 1) % RADII.length;
        return state.setValue(RANGE, next);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        return interact(state, level, pos, player, InteractionHand.MAIN_HAND);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        return interact(state, level, pos, player, hand);
    }

    private InteractionResult interact(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }

        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }

        int radius;
        if (player.isShiftKeyDown()) {
            radius = radius(state);
        } else {
            state = cycleRange(state);
            level.setBlock(pos, state, 3);
            radius = radius(state);
        }

        StarWandererMythHandler.showAstralLanternOutline(serverLevel, pos, radius);
        player.sendSystemMessage(Component.translatable("message.mythos.astral_lantern_radius", radius));
        return InteractionResult.SUCCESS;
    }
}
