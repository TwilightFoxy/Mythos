package com.twily.mythos.registry;

import com.mojang.serialization.Codec;
import com.twily.mythos.Mythos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public final class MythosAttachments {

    private static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
        DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Mythos.MOD_ID);

    public static final Supplier<AttachmentType<String>> CURRENT_MYTH = ATTACHMENT_TYPES.register(
        "current_myth",
        () -> AttachmentType.builder(() -> "none")
            .serialize(Codec.STRING.fieldOf("myth"))
            .sync((holder, player) -> holder == player, ByteBufCodecs.STRING_UTF8)
            .copyOnDeath()
            .build()
    );

    public static final Supplier<AttachmentType<Integer>> DWARF_SOBER_TICKS = ATTACHMENT_TYPES.register(
        "dwarf_sober_ticks",
        () -> AttachmentType.builder(() -> 0)
            .serialize(Codec.INT.fieldOf("ticks_without_ale"))
            .build()
    );

    public static final Supplier<AttachmentType<Integer>> HUMAN_TRADE_VILLAGER = ATTACHMENT_TYPES.register(
        "human_trade_villager",
        () -> AttachmentType.builder(() -> -1)
            .serialize(Codec.INT.fieldOf("villager_id"))
            .build()
    );

    public static final Supplier<AttachmentType<Boolean>> HUMAN_TRADE_BOOSTED = ATTACHMENT_TYPES.register(
        "human_trade_boosted",
        () -> AttachmentType.builder(() -> false)
            .serialize(Codec.BOOL.fieldOf("boosted"))
            .build()
    );

    private MythosAttachments() {
    }

    public static void register(IEventBus modBus) {
        ATTACHMENT_TYPES.register(modBus);
    }
}
