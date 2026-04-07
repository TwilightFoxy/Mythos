package com.twily.mythos.world.entity;

import com.twily.mythos.registry.MythosItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
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

    private static final double MAX_FAIRY_SPEED_ON_LAND = 8.0D;
    private static final double MAX_FAIRY_SPEED_IN_WATER = 2.0D;
    private static final double POWERED_RAIL_EXTRA_BOOST = 0.12D;

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
        if (!(state.getBlock() instanceof PoweredRailBlock poweredRail) || poweredRail.isActivatorRail() || !state.getValue(PoweredRailBlock.POWERED)) {
            return;
        }

        Vec3 boostDirection = this.getBoostDirection(railPos, state);
        if (boostDirection.lengthSqr() <= 1.0E-5D) {
            return;
        }

        Vec3 horizontal = this.getDeltaMovement().multiply(1.0D, 0.0D, 1.0D);
        double currentSpeed = horizontal.length();
        double cappedSpeed = Math.min(currentSpeed + POWERED_RAIL_EXTRA_BOOST, this.getMaxSpeed(serverLevel));
        Vec3 boosted = boostDirection.scale(cappedSpeed);

        this.setDeltaMovement(boosted.x, this.getDeltaMovement().y, boosted.z);
        this.move(MoverType.SELF, new Vec3(boosted.x, 0.0D, boosted.z));
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
}
