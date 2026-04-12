package com.twily.mythos.gameplay;

import com.twily.mythos.Mythos;
import com.twily.mythos.myth.MythState;
import com.twily.mythos.registry.MythosAttachments;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.util.Mth;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = Mythos.MOD_ID)
public final class HumanMythHandler {

    /*
     * FROZEN CONTRACT
     * Scope:
     * - human trading bonuses, near-minimum pricing, trade stock extension, and villager state cleanup
     * Guarantees:
     * - human keeps strong villager economy identity
     * - trade stock is boosted only during the active trading session and collapses afterward
     * Do not change without explicit request.
     */
    private static final Identifier HUMAN = Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "human");
    private static final int HERO_DURATION = 40;
    private static final int HERO_LEVEL = 4;
    private static final float HUMAN_PRICE_TARGET = 0.2F;

    private HumanMythHandler() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) {
            return;
        }

        if (MythState.is(player, HUMAN) && player.tickCount % 20 == 0) {
            player.addEffect(new MobEffectInstance(MobEffects.HERO_OF_THE_VILLAGE, HERO_DURATION, HERO_LEVEL, true, false, true));
        }

        int villagerId = player.getData(MythosAttachments.HUMAN_TRADE_VILLAGER);
        if (villagerId != -1 && !(player.containerMenu instanceof net.minecraft.world.inventory.MerchantMenu)) {
            collapseTrackedVillager(player, villagerId);
            player.setData(MythosAttachments.HUMAN_TRADE_VILLAGER, -1);
        }
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getEntity().level().isClientSide() || event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }

        Player player = event.getEntity();
        if (!MythState.is(player, HUMAN) || !(event.getTarget() instanceof AbstractVillager villager)) {
            return;
        }

        boostVillagerTrades(villager);
        player.setData(MythosAttachments.HUMAN_TRADE_VILLAGER, villager.getId());
    }

    private static void collapseTrackedVillager(Player player, int villagerId) {
        Entity entity = player.level().getEntity(villagerId);
        if (entity instanceof AbstractVillager villager) {
            collapseVillagerTrades(villager);
        }
    }

    private static void boostVillagerTrades(AbstractVillager villager) {
        if (villager.getData(MythosAttachments.HUMAN_TRADE_BOOSTED)) {
            return;
        }

        MerchantOffers offers = villager.getOffers();
        if (offers.isEmpty()) {
            return;
        }

        MerchantOffers boosted = new MerchantOffers();
        for (MerchantOffer offer : offers) {
            MerchantOffer adjusted = new MerchantOffer(
                offer.getItemCostA(),
                offer.getItemCostB(),
                offer.getResult().copy(),
                offer.getUses() * 2,
                Math.max(1, offer.getMaxUses() * 2),
                offer.getXp(),
                offer.getPriceMultiplier(),
                offer.getDemand()
            );
            adjusted.setSpecialPriceDiff(getHumanSpecialPriceDiff(offer));
            boosted.add(adjusted);
        }

        offers.clear();
        offers.addAll(boosted);
        villager.setData(MythosAttachments.HUMAN_TRADE_BOOSTED, true);
    }

    private static void collapseVillagerTrades(AbstractVillager villager) {
        if (!villager.getData(MythosAttachments.HUMAN_TRADE_BOOSTED)) {
            return;
        }

        MerchantOffers offers = villager.getOffers();
        MerchantOffers collapsed = new MerchantOffers();
        for (MerchantOffer offer : offers) {
            MerchantOffer adjusted = new MerchantOffer(
                offer.getItemCostA(),
                offer.getItemCostB(),
                offer.getResult().copy(),
                (offer.getUses() + 1) / 2,
                Math.max(1, offer.getMaxUses() / 2),
                offer.getXp(),
                offer.getPriceMultiplier(),
                offer.getDemand()
            );
            adjusted.setSpecialPriceDiff(0);
            collapsed.add(adjusted);
        }

        offers.clear();
        offers.addAll(collapsed);
        villager.setData(MythosAttachments.HUMAN_TRADE_BOOSTED, false);
    }

    private static int getHumanSpecialPriceDiff(MerchantOffer offer) {
        int basePrice = offer.getBaseCostA().getCount();
        int demandDiff = Math.max(0, Mth.floor(basePrice * offer.getDemand() * offer.getPriceMultiplier()));
        int heroReduction = Math.max(1, Mth.floor((0.3D + 0.0625D * HERO_LEVEL) * basePrice));
        int targetPrice = Math.max(1, Math.min(basePrice - 1, Mth.ceil(basePrice * HUMAN_PRICE_TARGET)));
        int targetSpecialPrice = targetPrice - basePrice - demandDiff + heroReduction;
        return Math.min(offer.getSpecialPriceDiff(), targetSpecialPrice);
    }
}
