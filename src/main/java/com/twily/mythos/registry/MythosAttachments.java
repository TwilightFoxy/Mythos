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
        () -> AttachmentType.builder(() -> "mythos:none")
            .serialize(Codec.STRING.fieldOf("myth"))
            .sync((holder, player) -> true, ByteBufCodecs.STRING_UTF8)
            .copyOnDeath()
            .build()
    );

    public static final Supplier<AttachmentType<Integer>> DWARF_SOBER_TICKS = ATTACHMENT_TYPES.register(
        "dwarf_sober_ticks",
        () -> AttachmentType.builder(() -> 0)
            .serialize(Codec.INT.fieldOf("ticks_without_ale"))
            .build()
    );

    public static final Supplier<AttachmentType<Integer>> SIREN_DRY_TICKS = ATTACHMENT_TYPES.register(
        "siren_dry_ticks",
        () -> AttachmentType.builder(() -> -1)
            .serialize(Codec.INT.fieldOf("ticks_without_water"))
            .build()
    );

    public static final Supplier<AttachmentType<Integer>> SIREN_DRY_STAGE = ATTACHMENT_TYPES.register(
        "siren_dry_stage",
        () -> AttachmentType.builder(() -> 0)
            .serialize(Codec.INT.fieldOf("dry_stage"))
            .build()
    );

    public static final Supplier<AttachmentType<Boolean>> SIREN_DRY_FATIGUE_ACTIVE = ATTACHMENT_TYPES.register(
        "siren_dry_fatigue_active",
        () -> AttachmentType.builder(() -> false)
            .serialize(Codec.BOOL.fieldOf("dry_fatigue_active"))
            .build()
    );

    public static final Supplier<AttachmentType<Boolean>> SIREN_WATER_BUFFS_ACTIVE = ATTACHMENT_TYPES.register(
        "siren_water_buffs_active",
        () -> AttachmentType.builder(() -> false)
            .serialize(Codec.BOOL.fieldOf("water_buffs_active"))
            .build()
    );

    public static final Supplier<AttachmentType<Boolean>> SIREN_WATER_BREATHING_OWNED = ATTACHMENT_TYPES.register(
        "siren_water_breathing_owned",
        () -> AttachmentType.builder(() -> false)
            .serialize(Codec.BOOL.fieldOf("water_breathing_owned"))
            .build()
    );

    public static final Supplier<AttachmentType<Boolean>> SIREN_DOLPHINS_GRACE_OWNED = ATTACHMENT_TYPES.register(
        "siren_dolphins_grace_owned",
        () -> AttachmentType.builder(() -> false)
            .serialize(Codec.BOOL.fieldOf("dolphins_grace_owned"))
            .build()
    );

    public static final Supplier<AttachmentType<Boolean>> SIREN_NIGHT_VISION_OWNED = ATTACHMENT_TYPES.register(
        "siren_night_vision_owned",
        () -> AttachmentType.builder(() -> false)
            .serialize(Codec.BOOL.fieldOf("night_vision_owned"))
            .build()
    );

    public static final Supplier<AttachmentType<Boolean>> FAIRY_ELYTRA_MODE = ATTACHMENT_TYPES.register(
        "fairy_elytra_mode",
        () -> AttachmentType.builder(() -> false)
            .serialize(Codec.BOOL.fieldOf("elytra_mode"))
            .build()
    );

    public static final Supplier<AttachmentType<Integer>> FAIRY_VISION_COOLDOWN = ATTACHMENT_TYPES.register(
        "fairy_vision_cooldown",
        () -> AttachmentType.builder(() -> 0)
            .serialize(Codec.INT.fieldOf("vision_cooldown"))
            .build()
    );

    public static final Supplier<AttachmentType<Boolean>> KITSUNE_MASKED = ATTACHMENT_TYPES.register(
        "kitsune_masked",
        () -> AttachmentType.builder(() -> false)
            .serialize(Codec.BOOL.fieldOf("masked"))
            .sync((holder, player) -> true, ByteBufCodecs.BOOL)
            .build()
    );

    public static final Supplier<AttachmentType<Integer>> KITSUNE_DASH_COOLDOWN = ATTACHMENT_TYPES.register(
        "kitsune_dash_cooldown",
        () -> AttachmentType.builder(() -> 0)
            .serialize(Codec.INT.fieldOf("dash_cooldown"))
            .build()
    );

    public static final Supplier<AttachmentType<Integer>> KITSUNE_DASH_IMMUNITY = ATTACHMENT_TYPES.register(
        "kitsune_dash_immunity",
        () -> AttachmentType.builder(() -> 0)
            .serialize(Codec.INT.fieldOf("dash_immunity"))
            .build()
    );

    public static final Supplier<AttachmentType<Integer>> KITSUNE_FOXFIRE_COOLDOWN = ATTACHMENT_TYPES.register(
        "kitsune_foxfire_cooldown",
        () -> AttachmentType.builder(() -> 0)
            .serialize(Codec.INT.fieldOf("foxfire_cooldown"))
            .build()
    );

    public static final Supplier<AttachmentType<Integer>> KITSUNE_WRATH_COOLDOWN = ATTACHMENT_TYPES.register(
        "kitsune_wrath_cooldown",
        () -> AttachmentType.builder(() -> 0)
            .serialize(Codec.INT.fieldOf("wrath_cooldown"))
            .build()
    );

    public static final Supplier<AttachmentType<Integer>> ONI_BATTLE_FORM_TICKS = ATTACHMENT_TYPES.register(
        "oni_battle_form_ticks",
        () -> AttachmentType.builder(() -> 0)
            .serialize(Codec.INT.fieldOf("battle_form_ticks"))
            .build()
    );

    public static final Supplier<AttachmentType<Integer>> ONI_BATTLE_FORM_COOLDOWN = ATTACHMENT_TYPES.register(
        "oni_battle_form_cooldown",
        () -> AttachmentType.builder(() -> 0)
            .serialize(Codec.INT.fieldOf("battle_form_cooldown"))
            .build()
    );

    public static final Supplier<AttachmentType<Integer>> RAGE_TALISMAN_AFTERMATH_TICKS = ATTACHMENT_TYPES.register(
        "rage_talisman_aftermath_ticks",
        () -> AttachmentType.builder(() -> 0)
            .serialize(Codec.INT.fieldOf("rage_talisman_aftermath_ticks"))
            .build()
    );

    public static final Supplier<AttachmentType<Integer>> SLIME_STAGE = ATTACHMENT_TYPES.register(
        "slime_stage",
        () -> AttachmentType.builder(() -> -1)
            .serialize(Codec.INT.fieldOf("slime_stage"))
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
