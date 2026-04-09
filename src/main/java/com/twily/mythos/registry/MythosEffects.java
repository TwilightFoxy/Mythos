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
