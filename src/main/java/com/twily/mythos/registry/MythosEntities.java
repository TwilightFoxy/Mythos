package com.twily.mythos.registry;

import com.twily.mythos.Mythos;
import com.twily.mythos.world.entity.FairyMinecartEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class MythosEntities {

    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
        DeferredRegister.create(Registries.ENTITY_TYPE, Mythos.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<FairyMinecartEntity>> FAIRY_MINECART = ENTITY_TYPES.register(
        "fairy_minecart",
        () -> EntityType.Builder.<FairyMinecartEntity>of(FairyMinecartEntity::new, MobCategory.MISC)
            .sized(0.98F, 0.7F)
            .clientTrackingRange(8)
            .updateInterval(3)
            .build(ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "fairy_minecart")))
    );

    private MythosEntities() {
    }

    public static void register(IEventBus modBus) {
        ENTITY_TYPES.register(modBus);
    }
}
