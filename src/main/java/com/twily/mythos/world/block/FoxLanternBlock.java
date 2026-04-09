package com.twily.mythos.world.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class FoxLanternBlock extends LanternBlock {

    public static final MapCodec<FoxLanternBlock> CODEC = simpleCodec(FoxLanternBlock::new);
    private static final int ATTRACT_RADIUS = 18;
    private static final int TICK_INTERVAL = 20;
    private static final double MOVE_SPEED = 1.15D;

    public FoxLanternBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<FoxLanternBlock> codec() {
        return CODEC;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide()) {
            level.scheduleTick(pos, this, TICK_INTERVAL);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        lureHostiles(level, pos);
        level.scheduleTick(pos, this, TICK_INTERVAL);
    }

    private static void lureHostiles(ServerLevel level, BlockPos pos) {
        Vec3 target = Vec3.atCenterOf(pos);
        AABB area = new AABB(pos).inflate(ATTRACT_RADIUS);
        level.getEntitiesOfClass(Mob.class, area, mob -> mob instanceof Enemy && mob.isAlive()).forEach(mob -> {
            mob.getNavigation().moveTo(target.x, target.y, target.z, MOVE_SPEED);
            if (mob.getTarget() == null) {
                mob.setAggressive(true);
            }
        });
    }
}
