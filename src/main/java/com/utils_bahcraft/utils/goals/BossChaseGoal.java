package com.utils_bahcraft.utils.goals;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

public class BossChaseGoal extends Goal {
    private final Mob mob;
    private final double speed;
    private final int repathIntervalTicks;
    private int repathCooldown;

    public BossChaseGoal(Mob mob, double speed) {
        this(mob, speed, 10);
    }

    public BossChaseGoal(Mob mob, double speed, int repathIntervalTicks) {
        this.mob = mob;
        this.speed = speed;
        this.repathIntervalTicks = Math.max(1, repathIntervalTicks);
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        LivingEntity t = mob.getTarget();
        return t != null && t.isAlive();
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity t = mob.getTarget();
        return t != null && t.isAlive();
    }

    @Override
    public void start() {
        repathCooldown = 0;
    }

    @Override
    public void stop() {
        mob.getNavigation().stop();
        repathCooldown = 0;
    }

    @Override
    public void tick() {
        LivingEntity t = mob.getTarget();
        if (t == null) return;

        mob.getLookControl().setLookAt(t, 30.0F, 30.0F);

        if (--repathCooldown <= 0) {
            repathCooldown = repathIntervalTicks;
            mob.getNavigation().moveTo(t, speed);
        }
    }
}
