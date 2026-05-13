package com.twily.mythos.registry;

import com.twily.mythos.Mythos;
import com.twily.mythos.world.item.DwarvenAleItem;
import com.twily.mythos.world.item.ClingingGelItem;
import com.twily.mythos.world.item.EtherealCandleItem;
import com.twily.mythos.world.item.FoilBlockItem;
import com.twily.mythos.world.item.KitsuneTailTunerItem;
import com.twily.mythos.world.item.MythSphereItem;
import com.twily.mythos.world.item.MythosGuideItem;
import com.twily.mythos.world.item.RageTalismanItem;
import com.twily.mythos.world.item.SirenElixirItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.MinecartItem;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Map;

public final class MythosItems {

    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Mythos.MOD_ID);
    private static final Map<DyeColor, DeferredItem<Item>> DYED_REINFORCED_SHULKER_BOXES = registerDyedReinforcedShulkerBoxes();
    private static final ResourceKey<Item> DWARVEN_ALE_KEY = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "dwarven_ale"));
    private static final ResourceKey<Item> MYTHOS_GUIDE_KEY = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "mythos_guide"));
    private static final ResourceKey<Item> MYTH_SPHERE_KEY = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "myth_sphere"));
    private static final ResourceKey<Item> FAIRY_MINECART_KEY = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "fairy_minecart"));
    private static final ResourceKey<Item> KITSUNE_MASK_VISUAL_KEY = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "kitsune_mask_visual"));
    private static final ResourceKey<Item> KITSUNE_TAIL_VISUAL_KEY = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "kitsune_tail_visual"));
    private static final ResourceKey<Item> KITSUNE_FOXFIRE_VISUAL_KEY = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "kitsune_foxfire_visual"));
    private static final ResourceKey<Item> ONI_MASK_VISUAL_KEY = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "oni_mask_visual"));
    private static final ResourceKey<Item> KITSUNE_TAIL_TUNER_KEY = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "kitsune_tail_tuner"));
    private static final ResourceKey<Item> FOX_LANTERN_KEY = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "fox_lantern"));
    private static final ResourceKey<Item> SIREN_ELIXIR_KEY = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "siren_elixir"));
    private static final ResourceKey<Item> RAGE_TALISMAN_KEY = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "rage_talisman"));
    private static final ResourceKey<Item> CLINGING_GEL_KEY = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "clinging_gel"));
    private static final ResourceKey<Item> RESONANCE_SHARD_KEY = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "resonance_shard"));
    private static final ResourceKey<Item> ETHEREAL_CANDLE_KEY = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "ethereal_candle"));

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
    public static final DeferredItem<Item> ONI_MASK_VISUAL = ITEMS.register(
        "oni_mask_visual",
        () -> new Item(
            new Item.Properties()
                .setId(ONI_MASK_VISUAL_KEY)
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
    public static final DeferredItem<Item> RAGE_TALISMAN = ITEMS.register(
        "rage_talisman",
        () -> new RageTalismanItem(
            new Item.Properties()
                .setId(RAGE_TALISMAN_KEY)
                .stacksTo(16)
                .rarity(Rarity.UNCOMMON)
        )
    );
    public static final DeferredItem<Item> CLINGING_GEL = ITEMS.register(
        "clinging_gel",
        () -> new ClingingGelItem(
            new Item.Properties()
                .setId(CLINGING_GEL_KEY)
                .stacksTo(16)
                .rarity(Rarity.UNCOMMON)
        )
    );
    public static final DeferredItem<Item> RESONANCE_SHARD = ITEMS.register(
        "resonance_shard",
        () -> new Item(
            new Item.Properties()
                .setId(RESONANCE_SHARD_KEY)
                .stacksTo(16)
                .rarity(Rarity.RARE)
        )
    );
    public static final DeferredItem<Item> ETHEREAL_CANDLE = ITEMS.register(
        "ethereal_candle",
        () -> new EtherealCandleItem(
            new Item.Properties()
                .setId(ETHEREAL_CANDLE_KEY)
                .stacksTo(16)
                .rarity(Rarity.UNCOMMON)
        )
    );
    public static final DeferredItem<Item> REINFORCED_SHULKER_BOX = ITEMS.register(
        "reinforced_shulker_box",
        () -> new FoilBlockItem(
            MythosBlocks.REINFORCED_SHULKER_BOX.get(),
            new Item.Properties()
                .setId(itemKey("reinforced_shulker_box"))
                .stacksTo(1)
                .rarity(Rarity.RARE)
                .component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)
        )
    );
    private MythosItems() {
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }

    public static DeferredItem<Item> reinforcedShulkerBox(DyeColor color) {
        return DYED_REINFORCED_SHULKER_BOXES.get(color);
    }

    public static Item[] reinforcedShulkerBoxItems() {
        ArrayList<Item> items = new ArrayList<>(1 + DYED_REINFORCED_SHULKER_BOXES.size());
        items.add(REINFORCED_SHULKER_BOX.get());
        for (DeferredItem<Item> item : DYED_REINFORCED_SHULKER_BOXES.values()) {
            items.add(item.get());
        }
        return items.toArray(Item[]::new);
    }

    private static Map<DyeColor, DeferredItem<Item>> registerDyedReinforcedShulkerBoxes() {
        EnumMap<DyeColor, DeferredItem<Item>> items = new EnumMap<>(DyeColor.class);
        for (DyeColor color : DyeColor.values()) {
            String name = color.getName() + "_reinforced_shulker_box";
            items.put(
                color,
                ITEMS.register(
                    name,
                    () -> new FoilBlockItem(
                        MythosBlocks.reinforcedShulkerBox(color).get(),
                        new Item.Properties()
                            .setId(itemKey(name))
                            .stacksTo(1)
                            .rarity(Rarity.RARE)
                            .component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)
                    )
                )
            );
        }
        return Map.copyOf(items);
    }

    private static ResourceKey<Item> itemKey(String path) {
        return ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Mythos.MOD_ID, path));
    }
}
