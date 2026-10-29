package com.saints.goetybossservants.event;

import com.Polarice3.Goety.api.entities.IOwned;
import com.saints.goetybossservants.GoetyBossServants;
import com.saints.goetybossservants.config.ServantConfig;
import com.saints.goetybossservants.entity.monstrosity.RisenMobs;
import com.saints.goetybossservants.entity.servant.BossServant;
import com.saints.goetybossservants.entity.servant.ServantMinions;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.animal.AbstractGolem;
import net.minecraft.world.entity.projectile.Projectile;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

/**
 * The rules every boss servant shares, hooked into Fabric events and our own small mixins (Fabric
 * has no events for most of these): friendly fire, block breaking, minions, and targeting.
 * Boss-specific hooks (a boss's death, its summons) are added by that boss mod's compat class when
 * the mod is installed (see ModCompat).
 */
public final class ServantEvents {
    /** Extra checks for entities about to be added to a level; returning false stops the spawn. */
    public static final List<Predicate<Entity>> SPAWN_FILTERS = new CopyOnWriteArrayList<>();

    /** Entities a boss servant's damage was just stopped on, and the game tick it happened. */
    private static final Map<LivingEntity, Long> sparedThisTick = new WeakHashMap<>();

    private ServantEvents() {
    }

    public static void register() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register(ServantEvents::allowDamage);
        // Fired for removals and chunk unloads alike; only a removal for good takes the minions along.
        ServerEntityEvents.ENTITY_UNLOAD.register((entity, level) -> {
            if (entity instanceof BossServant && entity.getRemovalReason() != null && entity.getRemovalReason().shouldDestroy()) {
                ServantMinions.discardMinionsOf(level, entity);
            }
        });
    }

    // ----- Friendly fire -----------------------------------------------------------------------

    /** The boss servant an attack really comes from: the servant itself, or the boss behind a minion. */
    @Nullable
    public static BossServant attackingServant(@Nullable Entity attacker) {
        if (attacker instanceof BossServant servant) {
            return servant;
        }
        return attacker != null && ServantMinions.isMinion(attacker) && ServantMinions.summoner(attacker) instanceof BossServant summoner ? summoner : null;
    }

    /**
     * Central friendly-fire guard. Boss attacks often hurt everything in an area (spikes, thorn
     * auras, spore clouds, explosions, projectiles, minions). Damage that comes from a boss servant
     * or its minions never lands on its owner or allies, nor on players, pets or tame servants (a
     * hostile summoner's minions are fair game), unless that is the target being fought.
     */
    private static boolean allowDamage(LivingEntity victim, DamageSource source, float amount) {
        if (victim.level().isClientSide) {
            return true;
        }
        Entity attacker = source.getEntity();
        BossServant servant = attackingServant(attacker);
        if (servant == null || servant == victim) {
            return true;
        }
        boolean isTarget = attacker instanceof Mob attackerMob && attackerMob.getTarget() == victim || servant instanceof Mob servantMob && servantMob.getTarget() == victim;
        if (BossServant.isFriendly(servant, victim) || !isTarget && BossServant.sparedByAreaAttacks(servant, victim)) {
            sparedThisTick.put(victim, victim.level().getGameTime());
            return false;
        }
        return true;
    }

    /**
     * Some attacks add an effect right after their damage (the spore cloud's poison, the lich
     * missiles' slowness), without saying who applies it. Block new effects on anyone the guard
     * above just protected (LivingEntityMixin).
     */
    public static boolean blocksEffect(LivingEntity entity) {
        Long tick = sparedThisTick.get(entity);
        return tick != null && tick == entity.level().getGameTime();
    }

    /** For a risen mob's explosions (a creeper's, ExplosionMixin): who is spared the push and harm (with block
     * breaking off, also what hangs or lies about). */
    public static boolean sparedByRisenExplosion(Mob risen, Entity entity) {
        return entity != risen && (entity instanceof LivingEntity ? RisenMobs.spares(risen, entity) : !breaksBlocks());
    }

    /** For a boss servant's explosions (ExplosionMixin): who is spared the push. */
    public static boolean sparedByExplosion(BossServant servant, Entity entity) {
        return entity instanceof LivingEntity living && entity != servant
                && (BossServant.isFriendly(servant, living) || living != ((Mob) servant).getTarget() && BossServant.sparedByAreaAttacks(servant, living));
    }

    // ----- Block breaking ----------------------------------------------------------------------

    /** Every entity this mod adds is a boss servant. */
    public static boolean isBossServant(@Nullable Entity entity) {
        return entity != null && GoetyBossServants.MOD_ID.equals(EntityType.getKey(entity.getType()).getNamespace());
    }

    public static boolean breaksBlocks() {
        return ServantConfig.BossesBreakBlocks.get();
    }

    // The thread ticking a risen mob, or something a risen mob shot, right now, when it may not grief.
    private static volatile Thread risenTicking;

    /**
     * ServerLevelMixin, around each entity's tick: while a risen mob (or its projectile) ticks, mob
     * griefing is off unless block breaking is on (GameRulesMixin; Forge: EntityMobGriefingEvent), so it
     * opens no doors, takes no blocks and sets nothing alight.
     */
    public static void startTicking(Entity entity) {
        Entity source = entity instanceof Projectile projectile ? projectile.getOwner() : entity;
        // (Boss servants too, as on Forge; and what a risen mob shot, even once the mob is gone.)
        boolean guarded = isBossServant(source) || RisenMobs.anyLoaded() && RisenMobs.isRisen(source) || RisenMobs.isRisenShot(entity);
        risenTicking = guarded && !breaksBlocks() ? Thread.currentThread() : null;
    }

    public static void stopTicking() {
        risenTicking = null;
    }

    public static boolean griefingBlocked() {
        return risenTicking == Thread.currentThread();
    }

    // ----- Minions, and what boss servants leave behind ----------------------------------------

    /** ServerLevelMixin: whether this entity may be added to the level. */
    public static boolean allowSpawn(Entity entity) {
        for (Predicate<Entity> filter : SPAWN_FILTERS) {
            if (!filter.test(entity)) {
                return false;
            }
        }
        return true;
    }

    // ----- Targeting ---------------------------------------------------------------------------

    /** MobMixin: whether the attacker may switch its target to this entity. */
    public static boolean allowTarget(Mob attacker, @Nullable LivingEntity target) {
        if (target == null || attacker.level().isClientSide) {
            return true;
        }
        // Risen mobs fight only their monstrosity's foes, and its side never fights them (see RisenMobs).
        if (RisenMobs.anyLoaded() && RisenMobs.blocksTargeting(attacker, target)) {
            return false;
        }
        // Boss servants that extend a boss's Monster class still count as "Enemy", which iron and snow
        // golems attack on sight. Goety's own servants aren't Monsters, so golems leave them alone.
        if (attacker instanceof AbstractGolem && target instanceof BossServant servant && !servant.isHostile() && servant.getTrueOwner() != null) {
            return false;
        }
        // A boss servant's minions only go after what the boss may hurt; healers only at the boss they heal.
        if (ServantMinions.isMinion(attacker)) {
            Mob boss = ServantMinions.summoner(attacker);
            return boss != null && (ServantMinions.isHealer(attacker) ? target == boss : !ServantMinions.spares(boss, target));
        }
        // Nothing on the boss's side (its owner's servants and pets, golems) turns on its minions.
        if (ServantMinions.isMinion(target)) {
            Mob boss = ServantMinions.summoner(target);
            LivingEntity owner = boss instanceof BossServant servant ? servant.getTrueOwner() : null;
            return boss == null || !(attacker instanceof AbstractGolem || boss.isAlliedTo(attacker)
                    || attacker instanceof IOwned owned && owned.getTrueOwner() == owner
                    || attacker instanceof OwnableEntity pet && owner != null && owner.getUUID().equals(pet.getOwnerUUID()));
        }
        return true;
    }
}
