package com.twily.mythos.registry;

import com.twily.mythos.Mythos;
import com.twily.mythos.world.item.DwarvenAleItem;
import com.twily.mythos.world.item.KitsuneTailTunerItem;
import com.twily.mythos.world.item.MythSphereItem;
import com.twily.mythos.world.item.MythosGuideItem;
import com.twily.mythos.world.item.SirenElixirItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.MinecartItem;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class MythosItems {

    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Mythos.MOD_ID);
    private static final ResourceKey<Item> DWARVEN_ALE_KEY = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "dwarven_ale"));
    private static final ResourceKey<Item> MYTHOS_GUIDE_KEY = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "mythos_guide"));
    private static final ResourceKey<Item> MYTH_SPHERE_KEY = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "myth_sphere"));
    private static final ResourceKey<Item> FAIRY_MINECART_KEY = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "fairy_minecart"));
    private static final ResourceKey<Item> KITSUNE_MASK_VISUAL_KEY = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "kitsune_mask_visual"));
    private static final ResourceKey<Item> KITSUNE_TAIL_VISUAL_KEY = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "kitsune_tail_visual"));
    private static final ResourceKey<Item> KITSUNE_FOXFIRE_VISUAL_KEY = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "kitsune_foxfire_visual"));
    private static final ResourceKey<Item> KITSUNE_TAIL_TUNER_KEY = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "kitsune_tail_tuner"));
    private static final ResourceKey<Item> FOX_LANTERN_KEY = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "fox_lantern"));
    private static final ResourceKey<Item> SIREN_ELIXIR_KEY = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "siren_elixir"));

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
    public static final DeferredItem<Item> MYTH_SPHERE = ITEMS.register(
        "myth_sphere",
        () -> new MythSphereItem(
            new Item.Properties()
                .setId(MYTH_SPHERE_KEY)
                .stacksTo(16)
                .rarity(Rarity.RARE)
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
    public static final DeferredItem<Item> KITSUNE_MASK_VISUAL = ITEMS.register(
        "kitsune_mask_visual",
        () -> new Item(
            new Item.Properties()
                .setId(KITSUNE_MASK_VISUAL_KEY)
                .stacksTo(1)
        )
    );
    public static final DeferredItem<Item> KITSUNE_TAIL_VISUAL = ITEMS.register(
        "kitsune_tail_visual",
        () -> new Item(
            new Item.Properties()
                .setId(KITSUNE_TAIL_VISUAL_KEY)
                .stacksTo(1)
        )
    );
    public static final DeferredItem<Item> KITSUNE_FOXFIRE_VISUAL = ITEMS.register(
        "kitsune_foxfire_visual",
        () -> new Item(
            new Item.Properties()
                .setId(KITSUNE_FOXFIRE_VISUAL_KEY)
                .stacksTo(1)
        )
    );
    public static final DeferredItem<Item> KITSUNE_TAIL_TUNER = ITEMS.register(
        "kitsune_tail_tuner",
        () -> new KitsuneTailTunerItem(
            new Item.Properties()
                .setId(KITSUNE_TAIL_TUNER_KEY)
                .stacksTo(1)
                .rarity(Rarity.UNCOMMON)
        )
    );
    public static final DeferredItem<Item> FOX_LANTERN = ITEMS.register(
        "fox_lantern",
        () -> new BlockItem(
            MythosBlocks.FOX_LANTERN.get(),
            new Item.Properties()
                .setId(FOX_LANTERN_KEY)
                .stacksTo(16)
                .rarity(Rarity.UNCOMMON)
        )
    );
    public static final DeferredItem<Item> SIREN_ELIXIR = ITEMS.register(
        "siren_elixir",
        () -> new SirenElixirItem(
            new Item.Properties()
                .setId(SIREN_ELIXIR_KEY)
                .stacksTo(16)
                .rarity(Rarity.UNCOMMON)
        )
    );
    private MythosItems() {
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }
}
