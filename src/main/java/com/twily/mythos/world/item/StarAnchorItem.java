package com.twily.mythos.world.item;

import com.twily.mythos.registry.MythosItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.function.Consumer;

public final class StarAnchorItem extends Item {

    private static final String DIMENSION_KEY = "AnchorDimension";
    private static final String X_KEY = "AnchorX";
    private static final String Y_KEY = "AnchorY";
    private static final String Z_KEY = "AnchorZ";

    private final boolean boundVariant;

    public StarAnchorItem(boolean boundVariant, Properties properties) {
        super(properties);
        this.boundVariant = boundVariant;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }

        if (!this.boundVariant) {
            BlockPos pos = serverPlayer.blockPosition();
            ItemStack bound = new ItemStack(MythosItems.BOUND_STAR_ANCHOR.get());
            CustomData.update(DataComponents.CUSTOM_DATA, bound, tag -> {
                tag.putString(DIMENSION_KEY, serverPlayer.level().dimension().identifier().toString());
                tag.putInt(X_KEY, pos.getX());
                tag.putInt(Y_KEY, pos.getY());
                tag.putInt(Z_KEY, pos.getZ());
            });
            bound.set(DataComponents.CUSTOM_NAME, Component.translatable("item.mythos.bound_star_anchor_named", pos.getX(), pos.getY(), pos.getZ()));
            player.setItemInHand(hand, bound);
            serverPlayer.sendSystemMessage(Component.translatable("message.mythos.star_anchor_bound", pos.getX(), pos.getY(), pos.getZ()));
            return InteractionResult.SUCCESS_SERVER;
        }

        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null || !data.copyTag().contains(DIMENSION_KEY)) {
            player.setItemInHand(hand, new ItemStack(MythosItems.STAR_ANCHOR.get()));
            return InteractionResult.SUCCESS_SERVER;
        }

        var tag = data.copyTag();
        String dimension = tag.getString(DIMENSION_KEY).orElse(Level.OVERWORLD.identifier().toString());
        if (!serverPlayer.level().dimension().identifier().toString().equals(dimension)) {
            serverPlayer.sendSystemMessage(Component.translatable("message.mythos.star_anchor_wrong_dimension"));
            return InteractionResult.SUCCESS_SERVER;
        }

        BlockPos anchorPos = new BlockPos(tag.getInt(X_KEY).orElse(0), tag.getInt(Y_KEY).orElse(0), tag.getInt(Z_KEY).orElse(0));
        Vec3 destination = findSafeDestination(serverPlayer, anchorPos);
        if (destination == null) {
            serverPlayer.sendSystemMessage(Component.translatable("message.mythos.star_anchor_blocked"));
            return InteractionResult.SUCCESS_SERVER;
        }

        serverPlayer.resetFallDistance();
        serverPlayer.teleportTo(destination.x, destination.y, destination.z);
        player.setItemInHand(hand, new ItemStack(MythosItems.STAR_ANCHOR.get()));
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return this.boundVariant || super.isFoil(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag tooltipFlag) {
        if (!this.boundVariant) {
            builder.accept(Component.translatable("tooltip.mythos.star_anchor"));
            return;
        }

        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data != null && data.copyTag().contains(DIMENSION_KEY)) {
            var tag = data.copyTag();
            builder.accept(Component.translatable("tooltip.mythos.bound_star_anchor", tag.getInt(X_KEY), tag.getInt(Y_KEY), tag.getInt(Z_KEY)));
        }
    }

    private static Vec3 findSafeDestination(ServerPlayer player, BlockPos anchorPos) {
        BlockPos[] tries = {
            anchorPos,
            anchorPos.above(),
            anchorPos.north(),
            anchorPos.south(),
            anchorPos.east(),
            anchorPos.west(),
            anchorPos.north().above(),
            anchorPos.south().above(),
            anchorPos.east().above(),
            anchorPos.west().above()
        };

        for (BlockPos pos : tries) {
            if (!player.level().getBlockState(pos).isAir() && !player.level().getBlockState(pos).canBeReplaced()) {
                continue;
            }
            if (!player.level().getBlockState(pos.above()).isAir() && !player.level().getBlockState(pos.above()).canBeReplaced()) {
                continue;
            }

            Vec3 feet = new Vec3(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
            AABB moved = player.getBoundingBox().move(feet.x - player.getX(), feet.y - player.getY(), feet.z - player.getZ());
            if (player.level().noCollision(player, moved)) {
                return feet;
            }
        }

        return null;
    }
}
