package com.saints.goetybossservants.entity.servant;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Minions summoned by a boss servant (the Night Lich's phantoms, the Returning Knight's Remnants and
 * Dark Sorcerers). Like Goety's Redstone Monstrosity cubes, they belong to the boss, which belongs to
 * the player. Each one carries its boss's UUID in its persistent data. Every tick (ModEvents) a minion
 * looks its boss up: if the boss is gone (dead, unloaded, or stored away in something like Goety's
 * Esoteric Tesseract) the minion is discarded; otherwise it fights the boss's target, stays near the
 * boss, and never turns on the boss's friends. Healers (Dark Sorcerers) target the boss itself, which
 * is how their beam heals it.
 */
public final class ServantMinions {
    public static final String SUMMONER_TAG = "goetybossservants:summoner";
    public static final String HEALER_TAG = "goetybossservants:healer";
    private static final double FOLLOW_DISTANCE = 8.0;
    private static final double TELEPORT_DISTANCE = 32.0;

    private ServantMinions() {
    }

    public static void tag(Entity minion, Mob summoner, boolean healer) {
        minion.getPersistentData().putUUID(SUMMONER_TAG, summoner.getUUID());
        if (healer) {
            minion.getPersistentData().putBoolean(HEALER_TAG, true);
        }
        // Most of these despawn when no player is near; ours go only with their boss (see tick).
        if (minion instanceof Mob mob) {
            mob.setPersistenceRequired();
        }
    }

    public static boolean isMinion(Entity entity) {
        return entity.getPersistentData().hasUUID(SUMMONER_TAG);
    }

    public static boolean isHealer(Entity entity) {
        return entity.getPersistentData().getBoolean(HEALER_TAG);
    }

    /** The boss servant that summoned this minion, if it is loaded and alive. */
    @Nullable
    public static Mob summoner(Entity minion) {
        CompoundTag data = minion.getPersistentData();
        if (!data.hasUUID(SUMMONER_TAG) || !(minion.level() instanceof ServerLevel level)) {
            return null;
        }
        return level.getEntity(data.getUUID(SUMMONER_TAG)) instanceof Mob boss && boss instanceof BossServant && boss.isAlive() ? boss : null;
    }

    public static boolean isMinionOf(Entity entity, Entity summoner) {
        CompoundTag data = entity.getPersistentData();
        return data.hasUUID(SUMMONER_TAG) && data.getUUID(SUMMONER_TAG).equals(summoner.getUUID());
    }

    /** How many live minions this boss has nearby (they never stray far from the fight). */
    public static int count(Mob summoner) {
        return summoner.level().getEntitiesOfClass(Mob.class, summoner.getBoundingBox().inflate(128.0),
                mob -> mob.isAlive() && isMinionOf(mob, summoner)).size();
    }

    /** Server tick for a minion: despawn without its boss, otherwise follow the boss's lead. */
    /**
     * The boss servant was removed for good (killed, dismissed, stored in a Tesseract): discard its
     * minions now, wherever they are. Minions also go on their own next tick (see tick), but one in a
     * chunk that isn't ticking would otherwise linger until it ticks again.
     */
    public static void discardMinionsOf(ServerLevel level, Entity boss) {
        List<Entity> minions = new ArrayList<>();
        for (Entity entity : level.getAllEntities()) {
            if (entity != boss && isMinionOf(entity, boss)) {
                minions.add(entity);
            }
        }
        minions.forEach(Entity::discard);
    }

    public static void tick(Mob minion) {
        Mob boss = summoner(minion);
        if (boss == null) {
            minion.discard();
            return;
        }
        minion.clearFire();
        if (isHealer(minion)) {
            if (minion.getTarget() != boss) {
                minion.setTarget(boss);
            }
            return;
        }
        LivingEntity target = boss.getTarget();
        if (target != null && target.isAlive() && minion.getTarget() != target) {
            minion.setTarget(target);
        } else if (minion.getTarget() != null && spares(boss, minion.getTarget())) {
            minion.setTarget(null);
        }
        if (minion.getTarget() == null && minion instanceof PathfinderMob walker) {
            stayNear(walker, boss);
        }
    }

    /** Walkers keep to their boss between fights, and catch up by teleport if left far behind. */
    private static void stayNear(PathfinderMob minion, Mob boss) {
        double distance = minion.distanceToSqr(boss);
        if (distance > TELEPORT_DISTANCE * TELEPORT_DISTANCE) {
            minion.teleportTo(boss.getX(), boss.getY(), boss.getZ());
            minion.getNavigation().stop();
        } else if (distance > FOLLOW_DISTANCE * FOLLOW_DISTANCE && minion.tickCount % 10 == 0) {
            minion.getNavigation().moveTo(boss, 1.0);
        }
    }

    /** Whether a minion of this boss must leave the entity alone (same rules as the boss's own area attacks). */
    public static boolean spares(Mob boss, LivingEntity entity) {
        BossServant servant = (BossServant) boss;
        return entity != boss.getTarget() && (BossServant.sparedByAreaAttacks(servant, entity) || isMinionOf(entity, boss))
                || BossServant.isFriendly(servant, entity);
    }
}
