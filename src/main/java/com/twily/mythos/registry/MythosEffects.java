package com.twily.mythos.registry;

import com.twily.mythos.Mythos;
import com.twily.mythos.world.effect.DwarvenAleEffect;
import com.twily.mythos.world.effect.SimpleDisplayEffect;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class MythosEffects {

    private static final DeferredRegister<MobEffect> MOB_EFFECTS =
        DeferredRegister.create(BuiltInRegistries.MOB_EFFECT, Mythos.MOD_ID);

    public static final DeferredHolder<MobEffect, DwarvenAleEffect> DWARVEN_ALE = MOB_EFFECTS.register(
        "dwarven_ale",
        DwarvenAleEffect::new
    );
    public static final DeferredHolder<MobEffect, SimpleDisplayEffect> DWARF_ALE_SLOWED = MOB_EFFECTS.register(
        "dwarf_ale_slowed",
        () -> new SimpleDisplayEffect(MobEffectCategory.NEUTRAL, 0x8A6B43)
    );
    public static final DeferredHolder<MobEffect, SimpleDisplayEffect> DWARF_ALE_WITHDRAWAL = MOB_EFFECTS.register(
        "dwarf_ale_withdrawal",
        () -> new SimpleDisplayEffect(MobEffectCategory.NEUTRAL, 0xB67E31)
    );
    public static final DeferredHolder<MobEffect, SimpleDisplayEffect> DWARF_ACUTE_ALE_WITHDRAWAL = MOB_EFFECTS.register(
        "dwarf_acute_ale_withdrawal",
        () -> new SimpleDisplayEffect(MobEffectCategory.NEUTRAL, 0x8E4B17)
    );
    public static final DeferredHolder<MobEffect, SimpleDisplayEffect> SIREN_DRY_SLOW_I = MOB_EFFECTS.register(
        "siren_dry_slow_i",
        () -> new SimpleDisplayEffect(MobEffectCategory.NEUTRAL, 0x4C8FB6)
    );
    public static final DeferredHolder<MobEffect, SimpleDisplayEffect> SIREN_DRY_SLOW_II = MOB_EFFECTS.register(
        "siren_dry_slow_ii",
        () -> new SimpleDisplayEffect(MobEffectCategory.NEUTRAL, 0x3D79A0)
    );
    public static final DeferredHolder<MobEffect, SimpleDisplayEffect> SIREN_DRY_SLOW_III = MOB_EFFECTS.register(
        "siren_dry_slow_iii",
        () -> new SimpleDisplayEffect(MobEffectCategory.NEUTRAL, 0x2E627F)
    );
    public static final DeferredHolder<MobEffect, SimpleDisplayEffect> SIREN_DRY_FATIGUE = MOB_EFFECTS.register(
        "siren_dry_fatigue",
        () -> new SimpleDisplayEffect(MobEffectCategory.NEUTRAL, 0x6E8F97)
    );
    public static final DeferredHolder<MobEffect, SimpleDisplayEffect> SIREN_ELIXIR_GRACE = MOB_EFFECTS.register(
        "siren_elixir_grace",
        () -> new SimpleDisplayEffect(MobEffectCategory.BENEFICIAL, 0x2AC7D7)
    );
    public static final DeferredHolder<MobEffect, SimpleDisplayEffect> KITSUNE_DASH_COOLDOWN = MOB_EFFECTS.register(
        "kitsune_dash_cooldown",
        () -> new SimpleDisplayEffect(MobEffectCategory.NEUTRAL, 0xCFA5FF)
    );
    public static final DeferredHolder<MobEffect, SimpleDisplayEffect> KITSUNE_FOXFIRE_COOLDOWN = MOB_EFFECTS.register(
        "kitsune_foxfire_cooldown",
        () -> new SimpleDisplayEffect(MobEffectCategory.NEUTRAL, 0x5CEEFF)
    );
    public static final DeferredHolder<MobEffect, SimpleDisplayEffect> KITSUNE_WRATH_COOLDOWN = MOB_EFFECTS.register(
        "kitsune_wrath_cooldown",
        () -> new SimpleDisplayEffect(MobEffectCategory.NEUTRAL, 0xFFF17A)
    );

    private MythosEffects() {
    }

    public static void register(IEventBus modBus) {
        MOB_EFFECTS.register(modBus);
    }
}
