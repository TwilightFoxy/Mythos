package com.twily.mythos.world.entity;

import com.twily.mythos.registry.MythosBlocks;
import com.twily.mythos.registry.MythosEntities;
import com.twily.mythos.registry.MythosItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.hurtingprojectile.Fireball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class KitsuneFoxfireEntity extends Fireball {

    public KitsuneFoxfireEntity(net.minecraft.world.entity.EntityType<? extends Fireball> type, Level level) {
        super(type, level);
        this.setItem(new ItemStack(MythosItems.KITSUNE_FOXFIRE_VISUAL.asItem()));
    }

    public KitsuneFoxfireEntity(Level level, LivingEntity owner, Vec3 direction) {
        super(MythosEntities.KITSUNE_FOXFIRE.get(), owner, direction, level);
        this.setItem(new ItemStack(MythosItems.KITSUNE_FOXFIRE_VISUAL.asItem()));
    }

    public KitsuneFoxfireEntity(Level level, double x, double y, double z, Vec3 direction) {
        super(MythosEntities.KITSUNE_FOXFIRE.get(), x, y, z, direction, level);
        this.setItem(new ItemStack(MythosItems.KITSUNE_FOXFIRE_VISUAL.asItem()));
    }

    @Override
    protected ParticleOptions getTrailParticle() {
        return ParticleTypes.SOUL_FIRE_FLAME;
    }

    @Override
    protected void onHitEntity(EntityHitResult hitResult) {
        if (this.level() instanceof ServerLevel serverLevel) {
            Entity target = hitResult.getEntity();
            Entity owner = this.getOwner();
            int remainingFireTicks = target.getRemainingFireTicks();
            target.igniteForSeconds(5.0F);
            DamageSource damageSource = this.damageSources().fireball(this, owner);
            if (!target.hurtServer(serverLevel, damageSource, 5.0F)) {
                target.setRemainingFireTicks(remainingFireTicks);
            } else {
                EnchantmentHelper.doPostAttackEffects(serverLevel, target, damageSource);
            }
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult hitResult) {
        if (this.level() instanceof ServerLevel serverLevel) {
            Entity owner = this.getOwner();
            if (!(owner instanceof Mob) || net.neoforged.neoforge.event.EventHooks.canEntityGrief(serverLevel, owner)) {
                BlockPos pos = hitResult.getBlockPos().relative(hitResult.getDirection());
                if (this.level().isEmptyBlock(pos)) {
                    this.level().setBlockAndUpdate(pos, MythosBlocks.KITSUNE_FIRE.get().defaultBlockState());
                }
            }
        }
    }

    @Override
    protected void onHit(HitResult hitResult) {
        super.onHit(hitResult);
        if (!this.level().isClientSide()) {
            this.discard();
        }
    }
}
