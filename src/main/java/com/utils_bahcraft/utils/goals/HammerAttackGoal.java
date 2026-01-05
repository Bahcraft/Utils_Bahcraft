package com.utils_bahcraft.utils.goals;

import com.utils_bahcraft.entities.HammerBossEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;

public class HammerAttackGoal extends MeleeAttackGoal {
    private final HammerBossEntity boss;
    private final float attackRange; // How many blocks away it can hit

    public HammerAttackGoal(HammerBossEntity boss, double speedModifier, boolean followingTargetEvenIfNotSeen, float attackRange) {
        super(boss, speedModifier, followingTargetEvenIfNotSeen);
        this.boss = boss;
        this.attackRange = attackRange;
    }

    @Override
    protected double getAttackReachSqr(LivingEntity attackTarget) {
        // 1. Get the Boss's Width (e.g., 2.2 blocks)
        float width = this.boss.getBbWidth();

        // 2. Add the custom "Hammer Reach" (e.g., 4.0 blocks)
        float reach = width + this.attackRange;

        // 3. Square it (Minecraft uses squared distances for efficiency)
        return (double)(reach * reach + attackTarget.getBbWidth());
    }

    @Override
    protected void checkAndPerformAttack(LivingEntity pEnemy, double pDistToEnemySqr) {
        double reach = this.getAttackReachSqr(pEnemy);

        // If we are close enough AND our attack cooldown is ready
        if (pDistToEnemySqr <= reach && this.isTimeToAttack()) {

            // 1. Trigger the Attack Hand Swing
            this.resetAttackCooldown();
            this.mob.swing(InteractionHand.MAIN_HAND);

            // 2. Actually deal the damage
            this.mob.doHurtTarget(pEnemy);

            // 3. (Optional) Trigger GeckoLib Animation here if you want
            // this.boss.triggerAnim("controller", "attack");
        }
    }
}