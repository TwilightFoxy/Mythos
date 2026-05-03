package com.twily.mythos.world.entity;

import com.twily.mythos.Mythos;
import com.twily.mythos.myth.MythState;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public final class SlimeSeedEntity extends Slime {

    private static final Identifier SLIME = Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "slime");

    public SlimeSeedEntity(EntityType<? extends Slime> entityType, Level level) {
        super(entityType, level);
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
    public void playerTouch(Player player) {
        if (isFriendlySlimeborn(player)) {
            return;
        }

        super.playerTouch(player);
    }

    @Override
    protected void dealDamage(LivingEntity target) {
        if (isFriendlySlimeborn(target)) {
            return;
        }

        super.dealDamage(target);
    }

    private static boolean isFriendlySlimeborn(net.minecraft.world.entity.Entity target) {
        return target instanceof Player player && MythState.is(player, SLIME);
    }
}
