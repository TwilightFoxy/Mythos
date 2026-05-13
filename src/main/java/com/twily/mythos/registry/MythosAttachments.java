package com.twily.mythos.registry;

import com.mojang.serialization.Codec;
import com.twily.mythos.Mythos;
import com.twily.mythos.data.ShulkerbornInventoryData;
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

    public static final Supplier<AttachmentType<Boolean>> ARCH_FAIRY_SMALL = ATTACHMENT_TYPES.register(
        "arch_fairy_small",
        () -> AttachmentType.builder(() -> false)
            .serialize(Codec.BOOL.fieldOf("small"))
            .copyOnDeath()
            .build()
    );

    public static final Supplier<AttachmentType<Boolean>> SPIRIT_FORM_ACTIVE = ATTACHMENT_TYPES.register(
        "spirit_form_active",
        () -> AttachmentType.builder(() -> false)
            .serialize(Codec.BOOL.fieldOf("active"))
            .sync((holder, player) -> true, ByteBufCodecs.BOOL)
            .build()
    );

    public static final Supplier<AttachmentType<Integer>> SPIRIT_IN_MATTER_TICKS = ATTACHMENT_TYPES.register(
        "spirit_in_matter_ticks",
        () -> AttachmentType.builder(() -> 0)
            .serialize(Codec.INT.fieldOf("in_matter_ticks"))
            .build()
    );

    public static final Supplier<AttachmentType<Boolean>> SPIRIT_SNEAK_WAS_DOWN = ATTACHMENT_TYPES.register(
        "spirit_sneak_was_down",
        () -> AttachmentType.builder(() -> false)
            .serialize(Codec.BOOL.fieldOf("sneak_was_down"))
            .build()
    );

    public static final Supplier<AttachmentType<Integer>> SPIRIT_PHASE_COOLDOWN = ATTACHMENT_TYPES.register(
        "spirit_phase_cooldown",
        () -> AttachmentType.builder(() -> 0)
            .serialize(Codec.INT.fieldOf("phase_cooldown"))
            .build()
    );

    public static final Supplier<AttachmentType<Integer>> SPIRIT_PHASE_TICKS = ATTACHMENT_TYPES.register(
        "spirit_phase_ticks",
        () -> AttachmentType.builder(() -> 0)
            .serialize(Codec.INT.fieldOf("phase_ticks"))
            .build()
    );

    public static final Supplier<AttachmentType<Double>> SPIRIT_PHASE_TARGET_X = ATTACHMENT_TYPES.register(
        "spirit_phase_target_x",
        () -> AttachmentType.builder(() -> 0.0D)
            .serialize(Codec.DOUBLE.fieldOf("phase_target_x"))
            .build()
    );

    public static final Supplier<AttachmentType<Double>> SPIRIT_PHASE_TARGET_Y = ATTACHMENT_TYPES.register(
        "spirit_phase_target_y",
        () -> AttachmentType.builder(() -> 0.0D)
            .serialize(Codec.DOUBLE.fieldOf("phase_target_y"))
            .build()
    );

    public static final Supplier<AttachmentType<Double>> SPIRIT_PHASE_TARGET_Z = ATTACHMENT_TYPES.register(
        "spirit_phase_target_z",
        () -> AttachmentType.builder(() -> 0.0D)
            .serialize(Codec.DOUBLE.fieldOf("phase_target_z"))
            .build()
    );

    public static final Supplier<AttachmentType<Double>> SPIRIT_PHASE_START_X = ATTACHMENT_TYPES.register(
        "spirit_phase_start_x",
        () -> AttachmentType.builder(() -> 0.0D)
            .serialize(Codec.DOUBLE.fieldOf("phase_start_x"))
            .build()
    );

    public static final Supplier<AttachmentType<Double>> SPIRIT_PHASE_START_Y = ATTACHMENT_TYPES.register(
        "spirit_phase_start_y",
        () -> AttachmentType.builder(() -> 0.0D)
            .serialize(Codec.DOUBLE.fieldOf("phase_start_y"))
            .build()
    );

    public static final Supplier<AttachmentType<Double>> SPIRIT_PHASE_START_Z = ATTACHMENT_TYPES.register(
        "spirit_phase_start_z",
        () -> AttachmentType.builder(() -> 0.0D)
            .serialize(Codec.DOUBLE.fieldOf("phase_start_z"))
            .build()
    );

    public static final Supplier<AttachmentType<Integer>> SPIRIT_PHASE_FALL_IMMUNITY = ATTACHMENT_TYPES.register(
        "spirit_phase_fall_immunity",
        () -> AttachmentType.builder(() -> 0)
            .serialize(Codec.INT.fieldOf("phase_fall_immunity"))
            .build()
    );

    public static final Supplier<AttachmentType<Integer>> SPIRIT_STEPS_TICKS = ATTACHMENT_TYPES.register(
        "spirit_steps_ticks",
        () -> AttachmentType.builder(() -> 0)
            .serialize(Codec.INT.fieldOf("spirit_steps_ticks"))
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

    public static final Supplier<AttachmentType<Boolean>> FEMBOY_LOGIN_LIGHTNING_ENABLED = ATTACHMENT_TYPES.register(
        "femboy_login_lightning_enabled",
        () -> AttachmentType.builder(() -> true)
            .serialize(Codec.BOOL.fieldOf("enabled"))
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

    public static final Supplier<AttachmentType<ShulkerbornInventoryData>> SHULKERBORN_EXTRA_SLOTS = ATTACHMENT_TYPES.register(
        "shulkerborn_extra_slots",
        () -> AttachmentType.builder(ShulkerbornInventoryData::empty)
            .serialize(ShulkerbornInventoryData.CODEC)
            .sync((holder, player) -> true, ShulkerbornInventoryData.STREAM_CODEC)
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
