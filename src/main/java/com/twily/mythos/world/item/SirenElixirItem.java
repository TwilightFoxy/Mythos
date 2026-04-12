package com.twily.mythos.world.item;

import com.twily.mythos.myth.MythState;
import com.twily.mythos.registry.MythosAttachments;
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

public final class SirenElixirItem extends Item {

    private static final Identifier SIREN = Identifier.fromNamespaceAndPath("mythos", "siren");
    private static final int ELIXIR_DURATION_TICKS = 20 * 60 * 15;
    private static final int SIREN_DRY_MAX_TICKS = 20 * 90;

    public SirenElixirItem(Properties properties) {
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
            livingEntity.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, ELIXIR_DURATION_TICKS, 0, false, true, true));
            livingEntity.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, ELIXIR_DURATION_TICKS, 0, false, true, true));
            livingEntity.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, ELIXIR_DURATION_TICKS, 0, false, true, true));
            if (entity instanceof Player player) {
                player.setData(MythosAttachments.SIREN_DRY_TICKS, SIREN_DRY_MAX_TICKS);
                if (MythState.is(player, SIREN)) {
                    player.addEffect(new MobEffectInstance(MythosEffects.SIREN_ELIXIR_GRACE, ELIXIR_DURATION_TICKS, 0, false, true, true));
                }
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
        builder.accept(Component.translatable("tooltip.mythos.siren_elixir"));
    }
}
