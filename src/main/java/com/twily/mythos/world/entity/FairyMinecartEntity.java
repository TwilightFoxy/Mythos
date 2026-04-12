package com.twily.mythos.world.entity;

import com.twily.mythos.registry.MythosItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.minecart.Minecart;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.Vec3;

public final class FairyMinecartEntity extends Minecart {

    private static final double MAX_FAIRY_SPEED_ON_LAND = 2.4D;
    private static final double MAX_FAIRY_SPEED_IN_WATER = 2.0D;
    private static final double POWERED_RAIL_EXTRA_BOOST = 0.20D;
    private static final double ASCENDING_RAIL_EXTRA_BOOST = 0.14D;
    private static final double CURVE_SAFE_SPEED = 0.48D;
    private static final double ASCENDING_SAFE_SPEED = 2.10D;
    private static final double ASCENDING_MIN_SPEED = 0.70D;
    private static final double MIN_MOVING_SPEED = 0.05D;

    public FairyMinecartEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide() || !(this.level() instanceof ServerLevel serverLevel) || !this.isOnRails()) {
            return;
        }

        BlockPos railPos = this.getCurrentBlockPosOrRailBelow();
        BlockState state = this.level().getBlockState(railPos);
        RailShape currentShape = getRailShape(railPos, state);
        if (currentShape == null) {
            return;
        }

        RailInfo upcomingRail = getUpcomingRailInfo(railPos, currentShape);
        RailShape effectiveShape = currentShape;
        if (upcomingRail != null && isCurve(upcomingRail.shape())) {
            effectiveShape = upcomingRail.shape();
        }

        // Predictive clamping is only used for curves. Slopes should preserve speed for launches.
        clampForRailShape(effectiveShape, serverLevel);
        maintainAscendingMomentum(currentShape);

        if (!(state.getBlock() instanceof PoweredRailBlock poweredRail) || poweredRail.isActivatorRail() || !state.getValue(PoweredRailBlock.POWERED)) {
            return;
        }

        if (isCurve(currentShape) || (upcomingRail != null && isCurve(upcomingRail.shape()))) {
            return;
        }

        Vec3 boostDirection = this.getBoostDirection(railPos, state);
        if (boostDirection.lengthSqr() <= 1.0E-5D) {
            return;
        }

        Vec3 horizontal = this.getDeltaMovement().multiply(1.0D, 0.0D, 1.0D);
        double currentSpeed = horizontal.length();
        double extraBoost = POWERED_RAIL_EXTRA_BOOST;
        if (currentShape.isSlope()) {
            extraBoost += ASCENDING_RAIL_EXTRA_BOOST;
        }

        double speedFloor = currentShape.isSlope() ? ASCENDING_MIN_SPEED : 0.0D;
        double shapeCap = maxAllowedSpeedForShape(currentShape, serverLevel);
        double cappedSpeed = Math.min(Math.max(currentSpeed + extraBoost, speedFloor), shapeCap);
        Vec3 boosted = boostDirection.scale(cappedSpeed);

        this.setDeltaMovement(boosted.x, this.getDeltaMovement().y, boosted.z);
    }

    @Override
    protected Item getDropItem() {
        return MythosItems.FAIRY_MINECART.asItem();
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(MythosItems.FAIRY_MINECART.asItem());
    }

    @Override
    protected double getMaxSpeed(ServerLevel level) {
        return this.isInWater() ? MAX_FAIRY_SPEED_IN_WATER : MAX_FAIRY_SPEED_ON_LAND;
    }

    @Override
    public Vec3 getKnownMovement() {
        return this.getDeltaMovement();
    }

    private Vec3 getBoostDirection(BlockPos railPos, BlockState state) {
        Vec3 movement = this.getDeltaMovement().multiply(1.0D, 0.0D, 1.0D);
        if (movement.lengthSqr() > 1.0E-5D) {
            return movement.normalize();
        }

        Vec3 redstoneDirection = this.getRedstoneDirection(railPos);
        if (redstoneDirection.lengthSqr() > 1.0E-5D) {
            return redstoneDirection.normalize();
        }

        if (state.getBlock() instanceof BaseRailBlock railBlock) {
            RailShape shape = railBlock.getRailDirection(state, this.level(), railPos, this);
            return switch (shape) {
                case NORTH_SOUTH, ASCENDING_NORTH, ASCENDING_SOUTH -> new Vec3(0.0D, 0.0D, 1.0D);
                default -> new Vec3(1.0D, 0.0D, 0.0D);
            };
        }

        return Vec3.ZERO;
    }

    private RailShape getRailShape(BlockPos railPos, BlockState state) {
        if (state.getBlock() instanceof BaseRailBlock railBlock) {
            return railBlock.getRailDirection(state, this.level(), railPos, this);
        }

        return null;
    }

    private void clampForRailShape(RailShape shape, ServerLevel level) {
        Vec3 horizontal = this.getDeltaMovement().multiply(1.0D, 0.0D, 1.0D);
        double speed = horizontal.length();
        if (speed <= 1.0E-5D) {
            return;
        }

        double maxSpeed = maxAllowedSpeedForShape(shape, level);
        if (speed > maxSpeed) {
            Vec3 clamped = horizontal.normalize().scale(maxSpeed);
            this.setDeltaMovement(clamped.x, this.getDeltaMovement().y, clamped.z);
        }
    }

    private void maintainAscendingMomentum(RailShape shape) {
        if (!shape.isSlope()) {
            return;
        }

        Vec3 horizontal = this.getDeltaMovement().multiply(1.0D, 0.0D, 1.0D);
        double speed = horizontal.length();
        if (speed < MIN_MOVING_SPEED || speed >= ASCENDING_MIN_SPEED) {
            return;
        }

        Vec3 adjusted = horizontal.normalize().scale(ASCENDING_MIN_SPEED);
        this.setDeltaMovement(adjusted.x, this.getDeltaMovement().y, adjusted.z);
    }

    private double maxAllowedSpeedForShape(RailShape shape, ServerLevel level) {
        double baseMax = level != null ? this.getMaxSpeed(level) : MAX_FAIRY_SPEED_ON_LAND;
        if (shape.isSlope()) {
            return Math.min(baseMax, ASCENDING_SAFE_SPEED);
        }

        return switch (shape) {
            case NORTH_EAST, NORTH_WEST, SOUTH_EAST, SOUTH_WEST -> Math.min(baseMax, CURVE_SAFE_SPEED);
            default -> baseMax;
        };
    }

    private RailInfo getUpcomingRailInfo(BlockPos currentRailPos, RailShape currentShape) {
        Vec3 direction = this.getDeltaMovement().multiply(1.0D, 0.0D, 1.0D);
        if (direction.lengthSqr() <= 1.0E-5D) {
            direction = switch (currentShape) {
                case NORTH_SOUTH, ASCENDING_NORTH, ASCENDING_SOUTH -> new Vec3(0.0D, 0.0D, 1.0D);
                default -> new Vec3(1.0D, 0.0D, 0.0D);
            };
        }

        int stepX = Mth.sign(direction.x);
        int stepZ = Mth.sign(direction.z);
        if (stepX == 0 && stepZ == 0) {
            return null;
        }

        BlockPos[] candidates = new BlockPos[] {
            currentRailPos.offset(stepX, 0, stepZ),
            currentRailPos.offset(stepX, 1, stepZ),
            currentRailPos.offset(stepX, -1, stepZ)
        };

        for (BlockPos candidate : candidates) {
            BlockState state = this.level().getBlockState(candidate);
            RailShape shape = getRailShape(candidate, state);
            if (shape != null) {
                return new RailInfo(candidate, shape);
            }
        }

        return null;
    }

    private boolean isCurve(RailShape shape) {
        return switch (shape) {
            case NORTH_EAST, NORTH_WEST, SOUTH_EAST, SOUTH_WEST -> true;
            default -> false;
        };
    }

    private record RailInfo(BlockPos pos, RailShape shape) {
    }
}
