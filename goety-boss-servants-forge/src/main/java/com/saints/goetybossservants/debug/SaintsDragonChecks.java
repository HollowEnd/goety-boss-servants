package com.saints.goetybossservants.debug;

import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import net.minecraft.world.entity.Mob;

import static com.saints.goetybossservants.debug.SelfTest.check;

/** The Saints Dragons part of RiseCompatChecks (only loaded with Saints Dragons). */
final class SaintsDragonChecks {
    private SaintsDragonChecks() {
    }

    // (Only some dragons can be stunned for taming in this version, Raevyx among them.)
    private static boolean stunned(DragonEntity dragon) {
        return dragon instanceof Raevyx raevyx && raevyx.isTamingStunned();
    }

    /** A dragon hurt down into its taming stun (before it dies decaying). */
    static void stun(Mob mob) {
        if (!(mob instanceof DragonEntity dragon)) {
            return;
        }
        dragon.hurt(dragon.damageSources().generic(), dragon.getHealth() * 0.8f);
        check(true, "(the dragon fell stunned to be tamed: " + stunned(dragon) + ")");
    }

    /** What a dragon's sleep is up to, for a failed check's message. */
    static String sleepState(Mob mob) {
        return mob instanceof DragonEntity dragon ? dragon.getSleepDecision() + ", asleep " + dragon.isSleeping() + ", going to or from sleep "
                + dragon.isSleepTransitioning() + ", sit progress " + dragon.getSitProgress() + ", pressure " + (int) dragon.getSleepPressure()
                + ", target " + dragon.getTarget() + ", on ground " + dragon.onGround() + ", aerial " + dragon.isAerial() + ", hunting "
                + dragon.isHuntFoodPursuitActive() + ", sleep suppressed " + dragon.isSleepSuppressed() + ", last hurt at " + dragon.getLastDamagerTimestamp()
                + " (now " + dragon.tickCount + ")" : "not a dragon";
    }

    /** Sends a dragon to bed the way its own AI does once it decides to sleep (Saints Dragons' sleep entry). */
    static void sendToBed(Mob mob) {
        if (mob instanceof DragonEntity dragon) {
            dragon.startSleepEnter();
        }
    }

    static boolean asleep(Mob mob) {
        return mob instanceof DragonEntity dragon && dragon.isSleeping();
    }

    /** Whether a dragon is asleep, or on its way into or out of sleep. */
    static boolean sleepy(Mob mob) {
        return mob instanceof DragonEntity dragon && (dragon.isSleeping() || dragon.isSleepTransitioning());
    }

    /** It rose standing, and doesn't fall stunned again when hurt low. */
    static void checkRisen(Mob mob) {
        if (!(mob instanceof DragonEntity dragon)) {
            return;
        }
        check(!stunned(dragon) && !dragon.isNoAi() && !dragon.isOrderedToSit(), "a dragon that fell stunned rises standing, its AI on");
        dragon.hurt(dragon.damageSources().generic(), dragon.getHealth() - dragon.getMaxHealth() * 0.2f);
        check(!stunned(dragon) && dragon.isAlive(), "a risen dragon never falls stunned to be tamed");
    }
}
