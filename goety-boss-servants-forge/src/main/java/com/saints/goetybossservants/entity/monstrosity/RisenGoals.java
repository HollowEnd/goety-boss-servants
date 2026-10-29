package com.saints.goetybossservants.entity.monstrosity;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;

import java.util.EnumSet;

/**
 * The goals a risen mob (see RisenMobs) gets on top of its own: to fight what its monstrosity fights,
 * ahead of all its own target goals (which stay, but only ever find what it may fight); to strike back
 * when hurt, if it never did; and a plain melee attack, if it has no fighting of its own at all (a calm
 * mob in the rises_as_itself tag).
 */
final class RisenGoals {
    private RisenGoals() {
    }

    /** Gives {@code mob} the goals (once, however often it's loaded). */
    static void install(Mob mob) {
        GoalSelector targets = mob.targetSelector, goals = mob.goalSelector;
        if (targets.getAvailableGoals().stream().anyMatch(goal -> goal.getGoal() instanceof CopyMasterTarget)) {
            return;
        }
        boolean fights = RisenMobs.fightsByItself(mob);
        targets.addGoal(-2, new CopyMasterTarget(mob));
        if (mob instanceof PathfinderMob walker && targets.getAvailableGoals().stream().noneMatch(goal -> goal.getGoal() instanceof HurtByTargetGoal)) {
            targets.addGoal(1, new HurtByTargetGoal(walker));
        }
        if (!fights) {
            goals.addGoal(2, new Melee(mob));
        }
    }

    /**
     * Its monstrosity's fight: what the monstrosity is fighting, or just hit or was hit by. Holding the
     * target flag (first of all target goals), so none of its own can take the target away meanwhile.
     */
    static final class CopyMasterTarget extends TargetGoal {
        private LivingEntity wanted;

        CopyMasterTarget(Mob mob) {
            super(mob, false);
            this.setFlags(EnumSet.of(Goal.Flag.TARGET));
        }

        private LivingEntity wanted() {
            MaterialMonstrosity master = RisenMobs.master(this.mob);
            return master == null || RisenMobs.rising(this.mob) ? null : RisenMobs.wanted(this.mob, master, this.getFollowDistance());
        }

        @Override
        public boolean canUse() {
            this.wanted = this.wanted();
            return this.wanted != null && this.wanted != this.mob.getTarget();
        }

        @Override
        public void start() {
            this.mob.setTarget(this.wanted);
            this.targetMob = this.wanted;
            super.start();
        }

        @Override
        public boolean canContinueToUse() {
            // (Off to its monstrosity's new fight, when it has one; and never on with one it may not have.)
            LivingEntity wanted = this.wanted();
            LivingEntity fighting = this.mob.getTarget() != null ? this.mob.getTarget() : this.targetMob;
            return (wanted == null || wanted == this.targetMob) && fighting != null && RisenMobs.mayAttack(this.mob, fighting) && super.canContinueToUse();
        }
    }

    /** A plain melee attack, for a mob with no fighting of its own: as strong as it is (or as its health). */
    static final class Melee extends Goal {
        private final Mob mob;
        private final float damage;
        private int cooldown;

        Melee(Mob mob) {
            this.mob = mob;
            AttributeInstance attack = mob.getAttribute(Attributes.ATTACK_DAMAGE);
            this.damage = (float) (attack != null ? Math.max(2.0, attack.getValue()) : Math.max(2.0, Math.min(12.0, mob.getMaxHealth() * 0.15)));
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = this.mob.getTarget();
            return target != null && target.isAlive();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity target = this.mob.getTarget();
            if (target == null) {
                return;
            }
            this.mob.getLookControl().setLookAt(target, 30.0f, 30.0f);
            if (this.mob.tickCount % 10 == 0 || this.mob.getNavigation().isDone()) {
                this.mob.getNavigation().moveTo(target, 1.0);
            }
            double reach = this.mob.getBbWidth() * 2.0f * this.mob.getBbWidth() * 2.0f + target.getBbWidth();
            if (--this.cooldown <= 0 && this.mob.distanceToSqr(target) <= reach && this.mob.getSensing().hasLineOfSight(target)) {
                this.cooldown = 20;
                this.mob.swing(InteractionHand.MAIN_HAND);
                if (target.hurt(this.mob.damageSources().mobAttack(this.mob), this.damage)) {
                    this.mob.setLastHurtMob(target);
                }
            }
        }

        @Override
        public void stop() {
            this.mob.getNavigation().stop();
        }
    }
}
