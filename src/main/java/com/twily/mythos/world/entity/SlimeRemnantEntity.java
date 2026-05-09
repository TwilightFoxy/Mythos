package com.twily.mythos.world.entity;

import com.twily.mythos.Mythos;
import com.twily.mythos.myth.MythState;
import com.mojang.authlib.GameProfile;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.UUID;

public final class SlimeRemnantEntity extends Zombie {

    private static final Identifier SLIME = Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "slime");
    private static final EntityDataAccessor<ResolvableProfile> PROFILE = SynchedEntityData.defineId(
        SlimeRemnantEntity.class,
        EntityDataSerializers.RESOLVABLE_PROFILE
    );
    private static final EntityDataAccessor<Integer> SLIME_STAGE = SynchedEntityData.defineId(
        SlimeRemnantEntity.class,
        EntityDataSerializers.INT
    );

    public SlimeRemnantEntity(EntityType<? extends Zombie> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(PROFILE, ResolvableProfile.createUnresolved("slime_remnant"));
        builder.define(SLIME_STAGE, 2);
    }

    public void setAppearance(GameProfile profile, int slimeStage) {
        this.entityData.set(PROFILE, ResolvableProfile.createResolved(profile));
        this.entityData.set(SLIME_STAGE, Mth.clamp(slimeStage, 2, 3));
        if (this.getAttribute(Attributes.SCALE) != null) {
            this.getAttribute(Attributes.SCALE).setBaseValue(0.75D);
        }
    }

    public ResolvableProfile getAppearanceProfile() {
        return this.entityData.get(PROFILE);
    }

    public int getSlimeStage() {
        return Mth.clamp(this.entityData.get(SLIME_STAGE), 2, 3);
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        return !isFriendlySlimeborn(target) && super.canAttack(target);
    }

    @Override
    public void setTarget(LivingEntity target) {
        super.setTarget(isFriendlySlimeborn(target) ? null : target);
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        if (isFriendlySlimeborn(target)) {
            return false;
        }

        return super.doHurtTarget(level, target);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.store("appearance_profile", ResolvableProfile.CODEC, this.getAppearanceProfile());
        output.putInt("slime_stage", this.getSlimeStage());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        input.read("appearance_profile", ResolvableProfile.CODEC).ifPresent(profile -> this.entityData.set(PROFILE, profile));
        this.entityData.set(SLIME_STAGE, Mth.clamp(input.getIntOr("slime_stage", 2), 2, 3));
    }

    private static boolean isFriendlySlimeborn(Entity target) {
        return target instanceof Player player && MythState.matches(player, SLIME);
    }

    public static GameProfile fallbackProfile(UUID uuid) {
        return new GameProfile(uuid, "Slime Remnant");
    }
}
