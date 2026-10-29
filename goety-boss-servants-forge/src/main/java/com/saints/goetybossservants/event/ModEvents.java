package com.saints.goetybossservants.event;

import com.Polarice3.Goety.api.entities.IOwned;
import com.saints.goetybossservants.GoetyBossServants;
import com.saints.goetybossservants.config.ServantConfig;
import com.saints.goetybossservants.entity.monstrosity.RisenMobs;
import com.saints.goetybossservants.entity.servant.BossServant;
import com.saints.goetybossservants.entity.servant.ServantMinions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.animal.AbstractGolem;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.EntityMobGriefingEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingConversionEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingDestroyBlockEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.WeakHashMap;

@Mod.EventBusSubscriber(modid = GoetyBossServants.MOD_ID)
public class ModEvents {

    // Boss-specific handlers (the Mira mold, the lich's death, the Soulslike summons) live in each
    // boss mod's compat class and are only registered when that mod is installed (see ModCompat).

    // ----- Friendly fire -----------------------------------------------------------------------

    /** Entities a boss servant's damage was just stopped on, and the game tick it happened. */
    private static final Map<LivingEntity, Long> sparedThisTick = new WeakHashMap<>();

    /** The boss servant an attack really comes from: the servant itself, or the boss behind a minion. */
    @Nullable
    private static BossServant attackingServant(@Nullable Entity attacker) {
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
    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide) {
            return;
        }
        Entity attacker = event.getSource().getEntity();
        BossServant servant = attackingServant(attacker);
        if (servant == null || servant == victim) {
            return;
        }
        boolean isTarget = attacker instanceof Mob attackerMob && attackerMob.getTarget() == victim || servant instanceof Mob servantMob && servantMob.getTarget() == victim;
        if (BossServant.isFriendly(servant, victim) || !isTarget && BossServant.sparedByAreaAttacks(servant, victim)) {
            event.setCanceled(true);
            sparedThisTick.put(victim, victim.level().getGameTime());
        }
    }

    /**
     * Some attacks add an effect right after their damage (the spore cloud's poison, the lich
     * missiles' slowness). Forge doesn't say who applies an effect, so block new effects on anyone
     * the guard above just protected.
     */
    @SubscribeEvent
    public static void onEffectApplicable(MobEffectEvent.Applicable event) {
        Long tick = sparedThisTick.get(event.getEntity());
        if (tick != null && tick == event.getEntity().level().getGameTime()) {
            event.setResult(Event.Result.DENY);
        }
    }

    /**
     * Explosions push everything nearby even when the damage is stopped, and break blocks. For a
     * boss servant's explosions (the lich's comets): spare friendlies the push, and keep blocks
     * intact unless block breaking is turned on.
     */
    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Detonate event) {
        // A risen mob's (a creeper's): it spares what it must, and with block breaking off, what hangs or lies about.
        if (event.getExplosion().getIndirectSourceEntity() instanceof Mob risen && RisenMobs.isRisen(risen)) {
            boolean griefing = ServantConfig.BossesBreakBlocks.get();
            event.getAffectedEntities().removeIf(entity -> entity != risen && (entity instanceof LivingEntity ? RisenMobs.spares(risen, entity) : !griefing));
            if (!griefing) {
                event.getAffectedBlocks().clear();
            }
            return;
        }
        // ...and what a risen mob shot breaks no blocks, even once the mob is gone.
        if (RisenMobs.isRisenShot(event.getExplosion().getDirectSourceEntity()) && !ServantConfig.BossesBreakBlocks.get()) {
            event.getAffectedBlocks().clear();
        }
        BossServant servant = attackingServant(event.getExplosion().getIndirectSourceEntity());
        if (servant == null) {
            return;
        }
        if (!ServantConfig.BossesBreakBlocks.get()) {
            event.getAffectedBlocks().clear();
        }
        event.getAffectedEntities().removeIf(entity -> entity instanceof LivingEntity living && entity != servant
                && (BossServant.isFriendly(servant, living) || living != ((Mob) servant).getTarget() && BossServant.sparedByAreaAttacks(servant, living)));
    }

    // ----- Block breaking ----------------------------------------------------------------------

    /** Every entity this mod adds is a boss servant (the Mira Monstrosity gets IServant by mixin, not BossServant). */
    private static boolean isBossServant(@Nullable Entity entity) {
        return entity != null && GoetyBossServants.MOD_ID.equals(EntityType.getKey(entity.getType()).getNamespace());
    }

    /** Explosions and other griefing checks from boss servants (and risen mobs) follow the bossesBreakBlocks option. */
    @SubscribeEvent
    public static void onMobGriefing(EntityMobGriefingEvent event) {
        if ((isBossServant(event.getEntity()) || RisenMobs.isRisen(event.getEntity())) && !ServantConfig.BossesBreakBlocks.get()) {
            event.setResult(Event.Result.DENY);
        }
    }

    /** Boss servants smashing through blocks (the Mira Monstrosity's charges and quakes). */
    @SubscribeEvent
    public static void onDestroyBlock(LivingDestroyBlockEvent event) {
        if (isBossServant(event.getEntity()) && !ServantConfig.BossesBreakBlocks.get()) {
            event.setCanceled(true);
        }
    }

    // ----- Minions, and what boss servants leave behind ----------------------------------------

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (event.getEntity() instanceof Mob mob && !mob.level().isClientSide && ServantMinions.isMinion(mob)) {
            ServantMinions.tick(mob);
        }
        if (event.getEntity() instanceof Mob mob && !mob.level().isClientSide && RisenMobs.isRisen(mob)) {
            RisenMobs.tick(mob);
        }
        if (event.getEntity() instanceof BossServant servant) {
            BossServant.followOwnersFight(servant);
        }
    }

    // Fired for removals and chunk unloads alike; only a removal for good takes the minions along.
    @SubscribeEvent
    public static void onLeaveLevel(EntityLeaveLevelEvent event) {
        Entity entity = event.getEntity();
        RisenMobs.left(entity);
        if (entity instanceof BossServant && event.getLevel() instanceof ServerLevel level
                && entity.getRemovalReason() != null && entity.getRemovalReason().shouldDestroy()) {
            ServantMinions.discardMinionsOf(level, entity);
        }
    }

    /** Minions and risen mobs drop no loot, gear or experience (no farming a servant's summons). */
    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        if (ServantMinions.isMinion(event.getEntity()) || RisenMobs.isRisen(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onExperienceDrop(LivingExperienceDropEvent event) {
        // Minions, risen mobs, and boss servants whose boss drops experience on its own (the Netherman's
        // death animation awards it through this event every half second).
        if (ServantMinions.isMinion(event.getEntity()) || RisenMobs.isRisen(event.getEntity()) || event.getEntity() instanceof BossServant) {
            event.setCanceled(true);
        }
    }

    // ----- Risen mobs (see RisenMobs) -----------------------------------------------------------

    /** Gear a corpse that rose as itself dropped isn't kept on what rose (last, so only drops that happen). */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onCorpseDrops(LivingDropsEvent event) {
        RisenMobs.afterCorpseDrops(event.getEntity());
    }

    /** Nobody tames, feeds, rides, trades with, leashes or breeds a risen mob. */
    @SubscribeEvent
    public static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (RisenMobs.isRisen(event.getTarget())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
        }
    }

    @SubscribeEvent
    public static void onInteractAt(PlayerInteractEvent.EntityInteractSpecific event) {
        if (RisenMobs.isRisen(event.getTarget())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
        }
    }

    /** It stays what it rose as (no drowned, stray or zombified piglin). */
    @SubscribeEvent
    public static void onConvert(LivingConversionEvent.Pre event) {
        if (RisenMobs.isRisen(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onJoinLevel(EntityJoinLevelEvent event) {
        if (!event.isCanceled()) {
            RisenMobs.joined(event.getEntity());
        }
    }

    // ----- Targeting ---------------------------------------------------------------------------

    @SubscribeEvent
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        LivingEntity attacker = event.getEntity();
        LivingEntity target = event.getNewTarget();
        if (target == null) {
            return;
        }
        // Risen mobs fight only their monstrosity's foes, and its side never fights them (also for brains).
        if (!attacker.level().isClientSide && RisenMobs.anyLoaded() && RisenMobs.blocksTargeting(attacker, target)) {
            event.setCanceled(true);
            return;
        }
        // Boss servants that extend a boss's Monster class still count as "Enemy", which iron and snow
        // golems attack on sight. Goety's own servants aren't Monsters, so golems leave them alone.
        if (attacker instanceof AbstractGolem && target instanceof BossServant servant && !servant.isHostile() && servant.getTrueOwner() != null) {
            event.setCanceled(true);
            return;
        }
        // A boss servant's minions only go after what the boss may hurt; healers only at the boss they heal.
        if (ServantMinions.isMinion(attacker)) {
            Mob boss = ServantMinions.summoner(attacker);
            if (boss == null || (ServantMinions.isHealer(attacker) ? target != boss : ServantMinions.spares(boss, target))) {
                event.setCanceled(true);
            }
            return;
        }
        // Nothing on the boss's side (its owner's servants and pets, golems) turns on its minions.
        if (ServantMinions.isMinion(target)) {
            Mob boss = ServantMinions.summoner(target);
            LivingEntity owner = boss instanceof BossServant servant ? servant.getTrueOwner() : null;
            if (boss != null && (attacker instanceof AbstractGolem || boss.isAlliedTo(attacker)
                    || attacker instanceof IOwned owned && owned.getTrueOwner() == owner
                    || attacker instanceof OwnableEntity pet && owner != null && owner.getUUID().equals(pet.getOwnerUUID()))) {
                event.setCanceled(true);
            }
        }
    }
}
