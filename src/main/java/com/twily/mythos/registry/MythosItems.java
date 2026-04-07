package com.twily.mythos.registry;

import com.twily.mythos.Mythos;
import com.twily.mythos.world.item.DwarvenAleItem;
import com.twily.mythos.world.item.MythosGuideItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.MinecartItem;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class MythosItems {

    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Mythos.MOD_ID);
    private static final ResourceKey<Item> DWARVEN_ALE_KEY = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "dwarven_ale"));
    private static final ResourceKey<Item> MYTHOS_GUIDE_KEY = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "mythos_guide"));
    private static final ResourceKey<Item> FAIRY_MINECART_KEY = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "fairy_minecart"));

    public static final DeferredItem<Item> DWARVEN_ALE = ITEMS.register(
        "dwarven_ale",
        () -> new DwarvenAleItem(
            new Item.Properties()
                .setId(DWARVEN_ALE_KEY)
                .stacksTo(16)
        )
    );
    public static final DeferredItem<Item> MYTHOS_GUIDE = ITEMS.register(
        "mythos_guide",
        () -> new MythosGuideItem(
            new Item.Properties()
                .setId(MYTHOS_GUIDE_KEY)
                .stacksTo(1)
                .rarity(Rarity.UNCOMMON)
        )
    );
    public static final DeferredItem<Item> FAIRY_MINECART = ITEMS.register(
        "fairy_minecart",
        () -> new MinecartItem(
            MythosEntities.FAIRY_MINECART.get(),
            new Item.Properties()
                .setId(FAIRY_MINECART_KEY)
                .stacksTo(1)
                .rarity(Rarity.UNCOMMON)
        )
    );

    private MythosItems() {
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }
}
