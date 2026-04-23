package com.twily.mythos.world.item;

import com.twily.mythos.registry.MythosAttachments;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

public final class RageTalismanItem extends Item {

    private static final int STRENGTH_DURATION_TICKS = 20 * 60 * 3;
    private static final int ITEM_COOLDOWN_TICKS = 20 * 60 * 4;

    public RageTalismanItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(stack)) {
            return InteractionResult.FAIL;
        }
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public void onUseTick(Level level, LivingEntity livingEntity, ItemStack stack, int ticksRemaining) {
        super.onUseTick(level, livingEntity, stack, ticksRemaining);
        if (level.isClientSide() && ticksRemaining % 4 == 0) {
            livingEntity.playSound(
                SoundEvents.GENERIC_EAT.value(),
                0.35F,
                0.92F + level.getRandom().nextFloat() * 0.14F
            );
        }
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (level instanceof ServerLevel) {
            entity.addEffect(new MobEffectInstance(MobEffects.STRENGTH, STRENGTH_DURATION_TICKS, 2, false, true, true));
            if (entity instanceof Player player) {
                player.setData(MythosAttachments.RAGE_TALISMAN_AFTERMATH_TICKS, STRENGTH_DURATION_TICKS);
                player.getCooldowns().addCooldown(stack, ITEM_COOLDOWN_TICKS);
            }
        }

        if (entity instanceof Player player && player.getAbilities().instabuild) {
            return stack;
        }

        stack.consume(1, entity);
        return stack;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.EAT;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return 32;
    }

    @Override
    public void appendHoverText(ItemStack itemStack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag tooltipFlag) {
        builder.accept(Component.translatable("tooltip.mythos.rage_talisman"));
    }
}
