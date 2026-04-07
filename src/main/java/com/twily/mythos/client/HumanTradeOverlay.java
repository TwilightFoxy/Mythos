package com.twily.mythos.client;

import com.twily.mythos.Mythos;
import com.twily.mythos.myth.MythState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

import java.lang.reflect.Field;

public final class HumanTradeOverlay {

    private static final Identifier HUMAN = Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "human");
    private static final Field SHOP_ITEM_FIELD = resolveField("shopItem");
    private static final int OVERLAY_TEXT = 0xFF404040;

    public HumanTradeOverlay() {
    }

    @SubscribeEvent
    public void onScreenRender(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof MerchantScreen screen)) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !(MythState.is(minecraft.player, HUMAN) || minecraft.player.hasEffect(MobEffects.HERO_OF_THE_VILLAGE))) {
            return;
        }

        MerchantOffers offers = screen.getMenu().getOffers();
        if (offers.isEmpty()) {
            return;
        }

        int selectedOfferIndex = getSelectedOfferIndex(screen, offers.size());
        MerchantOffer offer = offers.get(selectedOfferIndex);
        int remainingTrades = Math.max(0, offer.getMaxUses() - offer.getUses());
        Component label = Component.translatable("gui.mythos.human.trade_uses", remainingTrades, offer.getMaxUses());
        int x = screen.getGuiLeft() + 108;
        int y = screen.getGuiTop() + 62;
        event.getGuiGraphics().text(minecraft.font, label, x, y, OVERLAY_TEXT, false);
    }

    private static int getSelectedOfferIndex(MerchantScreen screen, int offerCount) {
        if (SHOP_ITEM_FIELD == null) {
            return 0;
        }

        try {
            return Math.clamp(SHOP_ITEM_FIELD.getInt(screen), 0, offerCount - 1);
        } catch (IllegalAccessException ignored) {
            return 0;
        }
    }

    private static Field resolveField(String name) {
        try {
            Field field = MerchantScreen.class.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }
}
