package com.twily.mythos.world.entity;

import com.twily.mythos.registry.MythosItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
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

    private static final double MAX_FAIRY_SPEED_ON_LAND = 30.0D;
    private static final double MAX_FAIRY_SPEED_IN_WATER = 10.0D;
    private static final double MAX_FAIRY_AIRBORNE_SPEED = 34.0D;
    private static final double MAX_STORED_MOMENTUM = 34.0D;
    private static final double POWERED_RAIL_EXTRA_BOOST = 0.62D;
    private static final double ASCENDING_RAIL_EXTRA_BOOST = 0.38D;
    private static final double CURVE_SAFE_SPEED = 0.92D;
    private static final double UPCOMING_CURVE_SAFE_SPEED = 1.12D;
    private static final double ASCENDING_SAFE_SPEED = 22.0D;
    private static final double ASCENDING_MIN_SPEED = 1.45D;
    private static final double MIN_MOVING_SPEED = 0.05D;
    private static final double GROUNDED_OFFTRACK_FRICTION = 0.88D;
    private static final double AIRBORNE_OFFTRACK_FRICTION = 0.998D;
    private static final double STRAIGHT_MOMENTUM_DECAY = 0.9975D;
    private static final double CURVE_MOMENTUM_DECAY = 0.9925D;
    private static final double RIDER_START_PUSH = 0.16D;
    private static final double RIDER_DRIVE_PUSH = 0.07D;
    private static final double RIDER_REVERSE_BRAKE = 0.18D;
    private static final double MAX_SLOPE_LAUNCH_VERTICAL_SPEED = 4.25D;
    private static final double SLOPE_LAUNCH_VERTICAL_FACTOR = 0.14D;

    private double storedRailMomentum;
    private boolean offTrackLaunchConsumed = true;
    private RailShape lastTrackedRailShape;
    private Vec3 lastTrackedTravelDirection = Vec3.ZERO;
    private Vec3 lastTrackedLaunchDirection = Vec3.ZERO;

    public FairyMinecartEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Override
    public void tick() {
        ServerLevel serverLevel = this.level() instanceof ServerLevel level ? level : null;
        if (serverLevel != null && this.isOnRails()) {
            this.offTrackLaunchConsumed = false;
            prepareForRailStep();
        }

        super.tick();

        if (serverLevel == null || !this.isOnRails()) {
            return;
        }

        BlockPos railPos = this.getCurrentBlockPosOrRailBelow();
        BlockState state = this.level().getBlockState(railPos);
        RailShape currentShape = getRailShape(railPos, state);
        if (currentShape == null) {
            return;
        }

        RailInfo upcomingRail = getUpcomingRailInfo(railPos, currentShape);
        Vec3 travelDirection = getRailTravelDirection(currentShape, this.getDeltaMovement(), this.lastTrackedTravelDirection);
        double horizontalSpeed = this.getDeltaMovement().multiply(1.0D, 0.0D, 1.0D).length();
        updateStoredMomentum(currentShape, horizontalSpeed);
        applyRiderInput(travelDirection);

        if (!(state.getBlock() instanceof PoweredRailBlock poweredRail) || poweredRail.isActivatorRail() || !state.getValue(PoweredRailBlock.POWERED)) {
            applyRailTargetSpeed(currentShape, upcomingRail, travelDirection, serverLevel);
            trackRailState(currentShape, travelDirection);
            return;
        }

        if (!isCurve(currentShape) && (upcomingRail == null || !isCurve(upcomingRail.shape()))) {
            double extraBoost = POWERED_RAIL_EXTRA_BOOST;
            if (currentShape.isSlope()) {
                extraBoost += ASCENDING_RAIL_EXTRA_BOOST;
            }

            this.storedRailMomentum = Math.min(MAX_STORED_MOMENTUM, this.storedRailMomentum + extraBoost);
        }

        applyRailTargetSpeed(currentShape, upcomingRail, travelDirection, serverLevel);
        trackRailState(currentShape, travelDirection);
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
    protected void comeOffTrack(ServerLevel level) {
        Vec3 movement = this.getDeltaMovement();
        double launchSpeed = Math.max(movement.multiply(1.0D, 0.0D, 1.0D).length(), this.storedRailMomentum);
        Vec3 adjustedMovement = movement;
        if (!this.offTrackLaunchConsumed && this.lastTrackedLaunchDirection.lengthSqr() > 1.0E-5D && launchSpeed > MIN_MOVING_SPEED) {
            double clampedLaunchSpeed = Math.min(launchSpeed, MAX_FAIRY_AIRBORNE_SPEED);
            if (this.lastTrackedRailShape != null && this.lastTrackedRailShape.isSlope()) {
                Vec3 horizontalLaunch = this.lastTrackedLaunchDirection.multiply(1.0D, 0.0D, 1.0D);
                if (horizontalLaunch.lengthSqr() > 1.0E-5D) {
                    Vec3 normalizedHorizontal = horizontalLaunch.normalize().scale(clampedLaunchSpeed);
                    double verticalSpeed = Math.min(MAX_SLOPE_LAUNCH_VERTICAL_SPEED, clampedLaunchSpeed * SLOPE_LAUNCH_VERTICAL_FACTOR);
                    adjustedMovement = new Vec3(normalizedHorizontal.x, Math.max(movement.y, verticalSpeed), normalizedHorizontal.z);
                } else {
                    adjustedMovement = this.lastTrackedLaunchDirection.scale(clampedLaunchSpeed);
                }
            } else {
                adjustedMovement = this.lastTrackedLaunchDirection.scale(clampedLaunchSpeed);
            }
            this.offTrackLaunchConsumed = true;
        } else {
            double maxHorizontalSpeed = this.onGround() ? this.getMaxSpeed(level) : Math.max(this.getMaxSpeed(level), MAX_FAIRY_AIRBORNE_SPEED);
            adjustedMovement = new Vec3(
                Mth.clamp(movement.x, -maxHorizontalSpeed, maxHorizontalSpeed),
                movement.y,
                Mth.clamp(movement.z, -maxHorizontalSpeed, maxHorizontalSpeed)
            );
        }

        this.setDeltaMovement(adjustedMovement);

        if (this.onGround()) {
            this.setDeltaMovement(this.getDeltaMovement().scale(GROUNDED_OFFTRACK_FRICTION));
        }

        this.move(net.minecraft.world.entity.MoverType.SELF, this.getDeltaMovement());
        if (!this.onGround()) {
            this.setDeltaMovement(this.getDeltaMovement().scale(AIRBORNE_OFFTRACK_FRICTION));
        }

        this.storedRailMomentum = Math.min(MAX_FAIRY_AIRBORNE_SPEED, Math.max(this.storedRailMomentum, this.getDeltaMovement().length()));
    }

    @Override
    public Vec3 getKnownMovement() {
        return this.getDeltaMovement();
    }

    private Vec3 getLaunchDirectionForShape(RailShape shape, Vec3 travelDirection) {
        Vec3 horizontal = travelDirection.multiply(1.0D, 0.0D, 1.0D);
        if (shape == null) {
            return horizontal.lengthSqr() > 1.0E-5D ? horizontal.normalize() : Vec3.ZERO;
        }

        if (!shape.isSlope()) {
            return horizontal.lengthSqr() > 1.0E-5D ? horizontal.normalize() : switch (shape) {
                case NORTH_SOUTH -> new Vec3(0.0D, 0.0D, 1.0D);
                default -> new Vec3(1.0D, 0.0D, 0.0D);
            };
        }

        Vec3 uphill = getUphillDirection(shape);
        if (uphill.lengthSqr() <= 1.0E-5D) {
            return horizontal.lengthSqr() > 1.0E-5D ? horizontal.normalize() : Vec3.ZERO;
        }

        double sign = horizontal.lengthSqr() > 1.0E-5D && horizontal.dot(uphill) < 0.0D ? -1.0D : 1.0D;
        Vec3 launchHorizontal = sign < 0.0D ? uphill.scale(-1.0D) : uphill;
        return new Vec3(launchHorizontal.x, sign, launchHorizontal.z).normalize();
    }

    private RailShape getRailShape(BlockPos railPos, BlockState state) {
        if (state.getBlock() instanceof BaseRailBlock railBlock) {
            return railBlock.getRailDirection(state, this.level(), railPos, this);
        }

        return null;
    }

    private Vec3 getUphillDirection(RailShape shape) {
        return switch (shape) {
            case ASCENDING_EAST -> new Vec3(1.0D, 0.0D, 0.0D);
            case ASCENDING_WEST -> new Vec3(-1.0D, 0.0D, 0.0D);
            case ASCENDING_NORTH -> new Vec3(0.0D, 0.0D, -1.0D);
            case ASCENDING_SOUTH -> new Vec3(0.0D, 0.0D, 1.0D);
            default -> Vec3.ZERO;
        };
    }

    private void prepareForRailStep() {
        BlockPos railPos = this.getCurrentBlockPosOrRailBelow();
        BlockState state = this.level().getBlockState(railPos);
        RailShape currentShape = getRailShape(railPos, state);
        if (currentShape == null) {
            return;
        }

        RailInfo upcomingRail = getUpcomingRailInfo(railPos, currentShape);
        if (isCurve(currentShape)) {
            clampHorizontalTo(CURVE_SAFE_SPEED);
            return;
        }

        if (upcomingRail != null && isCurve(upcomingRail.shape())) {
            clampHorizontalTo(UPCOMING_CURVE_SAFE_SPEED);
        }
    }

    private void clampHorizontalTo(double maxSpeed) {
        Vec3 horizontal = this.getDeltaMovement().multiply(1.0D, 0.0D, 1.0D);
        double speed = horizontal.length();
        if (speed <= maxSpeed || speed <= 1.0E-5D) {
            return;
        }

        Vec3 clamped = horizontal.normalize().scale(maxSpeed);
        this.setDeltaMovement(clamped.x, this.getDeltaMovement().y, clamped.z);
    }

    private void updateStoredMomentum(RailShape shape, double currentSpeed) {
        double decay = isCurve(shape) ? CURVE_MOMENTUM_DECAY : STRAIGHT_MOMENTUM_DECAY;
        this.storedRailMomentum = Math.max(currentSpeed, this.storedRailMomentum * decay);
        if (shape.isSlope() && this.storedRailMomentum < ASCENDING_MIN_SPEED) {
            this.storedRailMomentum = ASCENDING_MIN_SPEED;
        }
        this.storedRailMomentum = Math.min(this.storedRailMomentum, MAX_STORED_MOMENTUM);
    }

    private void applyRailTargetSpeed(RailShape currentShape, RailInfo upcomingRail, Vec3 travelDirection, ServerLevel level) {
        Vec3 horizontalDirection = travelDirection.multiply(1.0D, 0.0D, 1.0D);
        if (horizontalDirection.lengthSqr() <= 1.0E-5D) {
            return;
        }

        double shapeCap = maxAllowedSpeedForShape(currentShape, level);
        double targetSpeed = Math.min(this.storedRailMomentum, shapeCap);
        if (!isCurve(currentShape) && upcomingRail != null && isCurve(upcomingRail.shape())) {
            targetSpeed = Math.min(targetSpeed, UPCOMING_CURVE_SAFE_SPEED);
        }
        if (currentShape.isSlope()) {
            targetSpeed = Math.max(targetSpeed, ASCENDING_MIN_SPEED);
        }

        Vec3 clamped = horizontalDirection.normalize().scale(targetSpeed);
        this.setDeltaMovement(clamped.x, this.getDeltaMovement().y, clamped.z);
    }

    private void applyRiderInput(Vec3 travelDirection) {
        Entity passenger = this.getFirstPassenger();
        if (!(passenger instanceof LivingEntity rider)) {
            return;
        }

        Vec3 look = rider.getLookAngle().multiply(1.0D, 0.0D, 1.0D);
        Vec3 railDirection = travelDirection.multiply(1.0D, 0.0D, 1.0D);
        if (look.lengthSqr() <= 1.0E-5D || railDirection.lengthSqr() <= 1.0E-5D) {
            return;
        }

        Vec3 normalizedLook = look.normalize();
        Vec3 normalizedRail = railDirection.normalize();
        double alignment = normalizedLook.dot(normalizedRail);
        float forwardInput = rider.zza;
        if (Math.abs(forwardInput) <= 0.01F) {
            return;
        }

        double currentSpeed = this.getDeltaMovement().multiply(1.0D, 0.0D, 1.0D).length();
        double signedDrive = alignment * forwardInput;
        if (signedDrive > 0.05D) {
            double push = currentSpeed <= 0.45D
                ? RIDER_START_PUSH * signedDrive
                : RIDER_DRIVE_PUSH * signedDrive;
            this.storedRailMomentum = Math.min(MAX_STORED_MOMENTUM, this.storedRailMomentum + push);
        } else if (signedDrive < -0.05D) {
            this.storedRailMomentum = Math.max(0.0D, this.storedRailMomentum - (RIDER_REVERSE_BRAKE * -signedDrive));
        }
    }

    private void trackRailState(RailShape shape, Vec3 travelDirection) {
        this.lastTrackedRailShape = shape;
        this.lastTrackedTravelDirection = travelDirection.lengthSqr() > 1.0E-5D ? travelDirection.normalize() : this.lastTrackedTravelDirection;
        this.lastTrackedLaunchDirection = getLaunchDirectionForShape(shape, this.lastTrackedTravelDirection);
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

    private Vec3 getRailTravelDirection(RailShape shape, Vec3 movement, Vec3 fallbackDirection) {
        Vec3 horizontal = movement.multiply(1.0D, 0.0D, 1.0D);
        if (shape == null) {
            return horizontal.lengthSqr() > 1.0E-5D ? horizontal.normalize() : fallbackDirection;
        }

        if (shape.isSlope()) {
            Vec3 uphill = getUphillDirection(shape);
            if (uphill.lengthSqr() <= 1.0E-5D) {
                return horizontal.lengthSqr() > 1.0E-5D ? horizontal.normalize() : fallbackDirection;
            }
            if (horizontal.lengthSqr() <= 1.0E-5D) {
                return fallbackDirection.lengthSqr() > 1.0E-5D ? fallbackDirection.normalize() : uphill;
            }
            return horizontal.dot(uphill) < 0.0D ? uphill.scale(-1.0D) : uphill;
        }

        return switch (shape) {
            case NORTH_SOUTH -> {
                double sign = horizontal.lengthSqr() > 1.0E-5D ? Math.signum(horizontal.z) : Math.signum(fallbackDirection.z);
                if (sign == 0.0D) {
                    sign = 1.0D;
                }
                yield new Vec3(0.0D, 0.0D, sign);
            }
            case EAST_WEST -> {
                double sign = horizontal.lengthSqr() > 1.0E-5D ? Math.signum(horizontal.x) : Math.signum(fallbackDirection.x);
                if (sign == 0.0D) {
                    sign = 1.0D;
                }
                yield new Vec3(sign, 0.0D, 0.0D);
            }
            default -> horizontal.lengthSqr() > 1.0E-5D ? horizontal.normalize() : fallbackDirection;
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
