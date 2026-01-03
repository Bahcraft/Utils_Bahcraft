package com.utils_bahcraft.utils.goals;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

public class BossAttackGoal extends Goal {
    private final Mob mob;
    private final int cooldownTicks;
    private final double attackRange;
    private int cooldown;

    public BossAttackGoal(Mob mob, int cooldownTicks, double attackRange) {
        this.mob = mob;
        this.cooldownTicks = cooldownTicks;
        this.attackRange = attackRange;
        this.setFlags(EnumSet.of(Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity t = mob.getTarget();
        return t != null && t.isAlive();
    }

    @Override
    public void start() {
        cooldown = 0;
    }

    @Override
    public void tick() {
        LivingEntity t = mob.getTarget();
        if (t == null) return;

        mob.getLookControl().setLookAt(t, 30.0F, 30.0F);

        if (cooldown > 0) {
            cooldown--;
            return;
        }

        double distSqr = mob.distanceToSqr(t);
        double rangeSqr = attackRange * attackRange;

        if (distSqr <= rangeSqr) { // test without LoS first
            cooldown = cooldownTicks;
            mob.swing(InteractionHand.MAIN_HAND);
            mob.doHurtTarget(t);
        }
    }

    @Override
    public void stop() {
        mob.getNavigation().stop();
    }
}
