package com.twily.mythos.registry;

import com.twily.mythos.Mythos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Optional;

public final class MythosCreativeTabs {

    private static final ResourceKey<Enchantment> SOULBOUND_KEY =
        ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath("soulbound_enchantment", "soulbound"));

    private static final DeferredRegister<CreativeModeTab> TABS =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Mythos.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MYTHOS_TAB = TABS.register(
        "mythos",
        () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.mythos"))
            .icon(() -> new ItemStack(MythosItems.MYTHOS_GUIDE.get()))
            .displayItems((parameters, output) -> {
                for (Item item : MythosItems.creativeTabItems()) {
                    output.accept(item);
                }
                addSoulboundBook(parameters, output);
            })
            .build()
    );

    private MythosCreativeTabs() {
    }

    public static void register(IEventBus modBus) {
        TABS.register(modBus);
    }

    private static void addSoulboundBook(CreativeModeTab.ItemDisplayParameters parameters, CreativeModeTab.Output output) {
        Optional<Holder.Reference<Enchantment>> soulbound = parameters.holders().lookupOrThrow(Registries.ENCHANTMENT).get(SOULBOUND_KEY);
        if (soulbound.isEmpty()) {
            return;
        }

        output.accept(EnchantmentHelper.createBook(new EnchantmentInstance(soulbound.get(), 1)));
    }
}
