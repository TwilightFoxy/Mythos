package com.twily.mythos.world.item;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.FlintAndSteelItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public final class IfritLighterItem extends FlintAndSteelItem {

    public IfritLighterItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        ItemStack lighter = context.getItemInHand();

        if (level instanceof ServerLevel serverLevel && player != null) {
            Optional<ItemEntity> target = serverLevel.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(1.25D))
                .stream()
                .filter(entity -> entity.isAlive() && !entity.getItem().isEmpty())
                .min(Comparator.comparingDouble(entity -> entity.position().distanceToSqr(pos.getCenter())));

            if (target.isPresent()) {
                ItemEntity entity = target.get();
                ItemStack stack = entity.getItem();
                Optional<RecipeHolder<SmeltingRecipe>> recipe = serverLevel.recipeAccess().getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(stack.copyWithCount(1)), serverLevel);
                if (recipe.isPresent()) {
                    ItemStack sample = recipe.get().value().assemble(new SingleRecipeInput(stack.copyWithCount(1)));
                    if (!sample.isEmpty()) {
                        List<ItemStack> outputs = smeltWholeStack(sample, stack.getCount());
                        entity.discard();
                        for (ItemStack output : outputs) {
                            ItemEntity result = new ItemEntity(serverLevel, entity.getX(), entity.getY(), entity.getZ(), output);
                            result.setDefaultPickUpDelay();
                            serverLevel.addFreshEntity(result);
                        }
                        damageLighter(lighter, player);
                        serverLevel.playSound(null, pos, SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.7F, 1.2F);
                        return InteractionResult.SUCCESS_SERVER;
                    }
                }
            }
        }

        return super.useOn(context);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag tooltipFlag) {
        builder.accept(Component.translatable("tooltip.mythos.ifrit_lighter"));
    }

    private static List<ItemStack> smeltWholeStack(ItemStack sample, int count) {
        int total = sample.getCount() * count;
        int max = sample.getMaxStackSize();
        ArrayList<ItemStack> outputs = new ArrayList<>();
        while (total > 0) {
            ItemStack next = sample.copy();
            next.setCount(Math.min(max, total));
            outputs.add(next);
            total -= next.getCount();
        }
        return outputs;
    }

    private static void damageLighter(ItemStack lighter, Player player) {
        lighter.hurtAndBreak(1, player, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
    }
}
