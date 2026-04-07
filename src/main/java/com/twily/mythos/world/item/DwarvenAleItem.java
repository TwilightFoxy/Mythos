package com.twily.mythos.world.item;

import com.twily.mythos.Mythos;
import com.twily.mythos.myth.MythState;
import com.twily.mythos.registry.MythosEffects;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

public final class DwarvenAleItem extends Item {

    private static final int ALE_DURATION_TICKS = 20 * 60 * 60;
    private static final int ELF_NAUSEA_TICKS = 20 * 15;
    private static final Identifier ELF = Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "elf");

    public DwarvenAleItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (level instanceof ServerLevel && entity instanceof LivingEntity livingEntity) {
            livingEntity.addEffect(new MobEffectInstance(MythosEffects.DWARVEN_ALE, ALE_DURATION_TICKS, 0, false, true, true));
            livingEntity.removeEffect(MobEffects.SLOWNESS);
            livingEntity.removeEffect(MobEffects.BLINDNESS);
            if (entity instanceof Player player && MythState.is(player, ELF)) {
                livingEntity.addEffect(new MobEffectInstance(MobEffects.NAUSEA, ELF_NAUSEA_TICKS, 0, false, true, true));
            }
        }

        if (entity instanceof Player player) {
            return ItemUtils.createFilledResult(stack, player, new ItemStack(Items.GLASS_BOTTLE));
        }

        stack.consume(1, entity);
        return new ItemStack(Items.GLASS_BOTTLE);
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.DRINK;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return 32;
    }

    @Override
    public void appendHoverText(ItemStack itemStack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag tooltipFlag) {
        builder.accept(Component.translatable("tooltip.mythos.dwarven_ale"));
    }
}
