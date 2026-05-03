package com.twily.mythos.registry;

import com.twily.mythos.Mythos;
import com.twily.mythos.world.entity.FairyMinecartEntity;
import com.twily.mythos.world.entity.KitsuneFoxfireEntity;
import com.twily.mythos.world.entity.SlimeRemnantEntity;
import com.twily.mythos.world.entity.SlimeSeedEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.world.entity.ai.attributes.Attributes;

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
    public static final DeferredHolder<EntityType<?>, EntityType<KitsuneFoxfireEntity>> KITSUNE_FOXFIRE = ENTITY_TYPES.register(
        "kitsune_foxfire",
        () -> EntityType.Builder.<KitsuneFoxfireEntity>of(KitsuneFoxfireEntity::new, MobCategory.MISC)
            .sized(0.3125F, 0.3125F)
            .clientTrackingRange(4)
            .updateInterval(10)
            .build(ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "kitsune_foxfire")))
    );
    public static final DeferredHolder<EntityType<?>, EntityType<SlimeRemnantEntity>> SLIME_REMNANT = ENTITY_TYPES.register(
        "slime_remnant",
        () -> EntityType.Builder.<SlimeRemnantEntity>of(SlimeRemnantEntity::new, MobCategory.MONSTER)
            .sized(0.6F, 1.95F)
            .clientTrackingRange(8)
            .build(ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "slime_remnant")))
    );
    public static final DeferredHolder<EntityType<?>, EntityType<SlimeSeedEntity>> SLIME_SEED = ENTITY_TYPES.register(
        "slime_seed",
        () -> EntityType.Builder.<SlimeSeedEntity>of(SlimeSeedEntity::new, MobCategory.MONSTER)
            .sized(0.52F, 0.52F)
            .clientTrackingRange(8)
            .build(ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "slime_seed")))
    );

    private MythosEntities() {
    }

    public static void register(IEventBus modBus) {
        ENTITY_TYPES.register(modBus);
    }

    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(SLIME_REMNANT.get(), SlimeRemnantEntity.createAttributes().build());
        event.put(SLIME_SEED.get(), Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 1.0D)
            .add(Attributes.MOVEMENT_SPEED, 0.3D)
            .add(Attributes.ATTACK_DAMAGE, 1.0D)
            .build());
    }
}
