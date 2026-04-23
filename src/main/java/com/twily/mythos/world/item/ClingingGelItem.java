package com.twily.mythos.world.item;

import com.twily.mythos.registry.MythosEffects;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.Level;

public final class ClingingGelItem extends Item {

    private static final int CLINGING_DURATION_TICKS = 20 * 60 * 5;

    public ClingingGelItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand usedHand) {
        player.startUsingItem(usedHand);
        return InteractionResult.CONSUME;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide() && entity instanceof Player player) {
            player.addEffect(new MobEffectInstance(MythosEffects.SLIME_CLINGING, CLINGING_DURATION_TICKS, 0, false, true, true));
            return ItemUtils.createFilledResult(stack, player, ItemStack.EMPTY, false);
        }

        stack.consume(1, entity);
        return stack;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 32;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.DRINK;
    }

    @Override
    public void onUseTick(Level level, LivingEntity livingEntity, ItemStack stack, int ticksRemaining) {
        super.onUseTick(level, livingEntity, stack, ticksRemaining);
        if (level.isClientSide() && ticksRemaining % 4 == 0) {
            livingEntity.playSound(
                SoundEvents.HONEY_DRINK.value(),
                0.45F,
                0.95F + level.getRandom().nextFloat() * 0.1F
            );
        }
    }
}
