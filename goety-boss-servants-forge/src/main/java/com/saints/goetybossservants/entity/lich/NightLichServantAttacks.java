/*
 * Parts of this file are adapted from Bosses of Mass Destruction (Forge port by CerbonXD, original
 * Fabric mod by Barribob), licensed under the GNU Lesser General Public License v3.0.
 * Modified for Goety Boss Servants: the lich's comet, volley, minion and teleport actions
 * rewritten to work on any target, with capped minions and a different attack schedule.
 * Modified by Saints, 2026.
 */
package com.saints.goetybossservants.entity.lich;

import com.cerbon.bosses_of_mass_destruction.config.mob.LichConfig;
import com.cerbon.bosses_of_mass_destruction.entity.ai.action.CooldownAction;
import com.cerbon.bosses_of_mass_destruction.entity.ai.action.IActionStop;
import com.cerbon.bosses_of_mass_destruction.entity.ai.action.IActionWithCooldown;
import com.cerbon.bosses_of_mass_destruction.entity.ai.action.ThrowProjectileAction;
import com.cerbon.bosses_of_mass_destruction.entity.ai.goals.ActionGoal;
import com.cerbon.bosses_of_mass_destruction.entity.custom.lich.CometAction;
import com.cerbon.bosses_of_mass_destruction.entity.custom.lich.CometRageAction;
import com.cerbon.bosses_of_mass_destruction.entity.custom.lich.LichActions;
import com.cerbon.bosses_of_mass_destruction.entity.custom.lich.MinionAction;
import com.cerbon.bosses_of_mass_destruction.entity.custom.lich.MinionRageAction;
import com.cerbon.bosses_of_mass_destruction.entity.custom.lich.VolleyAction;
import com.cerbon.bosses_of_mass_destruction.entity.custom.lich.VolleyRageAction;
import com.cerbon.bosses_of_mass_destruction.entity.spawn.CompoundTagEntityProvider;
import com.cerbon.bosses_of_mass_destruction.entity.spawn.IMobSpawner;
import com.cerbon.bosses_of_mass_destruction.entity.spawn.ISpawnPredicate;
import com.cerbon.bosses_of_mass_destruction.entity.spawn.MobEntitySpawnPredicate;
import com.cerbon.bosses_of_mass_destruction.entity.spawn.MobPlacementLogic;
import com.cerbon.bosses_of_mass_destruction.entity.spawn.RangedSpawnPosition;
import com.cerbon.bosses_of_mass_destruction.entity.spawn.SimpleMobSpawner;
import com.cerbon.bosses_of_mass_destruction.entity.util.ProjectileThrower;
import com.cerbon.bosses_of_mass_destruction.particle.BMDParticles;
import com.cerbon.bosses_of_mass_destruction.projectile.MagicMissileProjectile;
import com.cerbon.bosses_of_mass_destruction.projectile.comet.CometProjectile;
import com.cerbon.bosses_of_mass_destruction.sound.BMDSounds;
import com.cerbon.cerbons_api.api.general.event.EventScheduler;
import com.cerbon.cerbons_api.api.general.event.TimedEvent;
import com.cerbon.cerbons_api.api.general.random.WeightedRandom;
import com.cerbon.cerbons_api.api.static_utilities.MathUtils;
import com.cerbon.cerbons_api.api.static_utilities.MobUtils;
import com.cerbon.cerbons_api.api.static_utilities.ParticleUtils;
import com.cerbon.cerbons_api.api.static_utilities.SoundUtils;
import com.cerbon.cerbons_api.api.static_utilities.VecUtils;
import com.saints.goetybossservants.config.ServantConfig;
import com.saints.goetybossservants.entity.servant.ServantMinions;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * The Night Lich's attacks for the servant. BMD's own actions (CometAction, VolleyAction,
 * MinionAction, TeleportAction and their rage versions) refuse to fire unless the target is a player,
 * so these are copies that work on any target, using BMD's comets, missiles, sounds and particles.
 *
 * <p>Its second phase is BMD's: each time damage takes it past 75/50/25% health, its next moves are
 * the comet rage, the missile-volley rage and the minion rage, in that order. Minions are vanilla
 * phantoms, as on the boss, but tagged to this lich and capped (see ServantMinions). Teleporting toward
 * the target only happens while it's free to move (not staying or guarding).
 */
public class NightLichServantAttacks {
    private final NightLichServant entity;
    private final LichConfig config;
    private final EventScheduler eventScheduler;
    private final Supplier<Boolean> shouldCancel;
    private final Map<Byte, IActionWithCooldown> actions;
    private final List<Byte> priorityMoves = new ArrayList<>();
    private byte lastMove;

    public NightLichServantAttacks(NightLichServant entity, LichConfig config, EventScheduler eventScheduler) {
        this.entity = entity;
        this.config = config;
        this.eventScheduler = eventScheduler;
        this.shouldCancel = () -> entity.isDeadOrDying() || entity.getTarget() == null;
        this.actions = Map.of(
                LichActions.cometAttack, this::comet,
                LichActions.volleyAttack, this::volley,
                LichActions.minionAttack, this::minions,
                LichActions.minionRageAttack, this::minionRage,
                LichActions.teleportAction, this::teleport,
                LichActions.volleyRageAttack, this::volleyRage,
                LichActions.cometRageAttack, this::cometRage);
    }

    /** Called when damage takes it past a health milestone: its rage moves come next. */
    public void queueRage() {
        this.priorityMoves.addAll(List.of(LichActions.cometRageAttack, LichActions.volleyRageAttack, LichActions.minionRageAttack));
    }

    public ActionGoal buildAttackGoal() {
        CooldownAction attackAction = new CooldownAction(this::chooseMove, 80);
        IActionStop onCancel = () -> {
            this.entity.level().broadcastEntityEvent(this.entity, LichActions.volleyAttack);
            attackAction.stop();
        };
        return new ActionGoal(() -> !this.shouldCancel.get(), null, attackAction, null, onCancel);
    }

    private int chooseMove() {
        LivingEntity target = this.entity.getTarget();
        if (target == null) {
            return 20;
        }
        byte move;
        if (!this.priorityMoves.isEmpty()) {
            move = this.priorityMoves.remove(0);
        } else {
            WeightedRandom<Byte> random = new WeightedRandom<>();
            random.add(1.0, LichActions.cometAttack);
            random.add(1.0, LichActions.volleyAttack);
            random.add(this.lastMove == LichActions.minionAttack ? 0.0 : 2.0, LichActions.minionAttack);
            random.add(this.teleportWeight(target), LichActions.teleportAction);
            move = random.next();
            this.lastMove = move;
        }
        this.entity.level().broadcastEntityEvent(this.entity, move);
        return this.actions.get(move).perform();
    }

    /** BMD teleports when it can't see the target or the target is too close; only if it may move. */
    private double teleportWeight(LivingEntity target) {
        if (this.entity.isStaying() || this.entity.isGuardingArea()) {
            return 0.0;
        }
        return (this.entity.hasLineOfSight(target) ? 0.0 : 4.0) + (this.entity.position().distanceTo(target.position()) < 6.0 ? 8.0 : 0.0);
    }

    private ServerLevel level() {
        return (ServerLevel) this.entity.level();
    }

    private Function<Vec3, ProjectileThrower> cometThrower() {
        return offset -> new ProjectileThrower(() -> {
            CometProjectile projectile = new CometProjectile(this.entity, this.entity.level(),
                    pos -> this.entity.level().explode(this.entity, pos.x, pos.y, pos.z, this.config.comet.explosionStrength, Level.ExplosionInteraction.MOB),
                    List.of(MinionAction.summonEntityType));
            MobUtils.setPos(projectile, MobUtils.eyePos(this.entity).add(offset));
            return new ProjectileThrower.ProjectileData(projectile, 1.6f, 0.0f, 0.2);
        });
    }

    private Function<Vec3, ProjectileThrower> missileThrower() {
        Optional<MobEffect> effect = Optional.ofNullable(ForgeRegistries.MOB_EFFECTS.getValue(ResourceLocation.tryParse(this.config.missile.mobEffectId)));
        int duration = this.config.missile.mobEffectDuration;
        int amplifier = this.config.missile.mobEffectAmplifier;
        return offset -> new ProjectileThrower(() -> {
            MagicMissileProjectile projectile = new MagicMissileProjectile(this.entity, this.entity.level(),
                    hit -> effect.ifPresent(e -> hit.addEffect(new MobEffectInstance(e, duration, amplifier))),
                    MinionAction.summonEntityType != null ? List.of(MinionAction.summonEntityType) : List.of());
            MobUtils.setPos(projectile, MobUtils.eyePos(this.entity).add(offset));
            return new ProjectileThrower.ProjectileData(projectile, 1.6f, 0.0f, 0.2);
        });
    }

    // CometAction: one exploding comet.
    private int comet() {
        if (this.entity.getTarget() == null) {
            return 80;
        }
        ServerLevel level = this.level();
        this.eventScheduler.addEvent(new TimedEvent(() -> SoundUtils.playSound(level, this.entity.position(), BMDSounds.COMET_PREPARE.get(), SoundSource.HOSTILE, 3.0f, 64.0, null), 10, 1, this.shouldCancel));
        this.eventScheduler.addEvent(new TimedEvent(() -> {
            new ThrowProjectileAction(this.entity, this.cometThrower().apply(CometAction.getCometLaunchOffset())).perform();
            SoundUtils.playSound(level, this.entity.position(), BMDSounds.COMET_SHOOT.get(), SoundSource.HOSTILE, 3.0f, 64.0, null);
        }, 60, 1, this.shouldCancel));
        return 80;
    }

    // CometRageAction: six comets in a ring, one after another.
    private int cometRage() {
        LivingEntity target = this.entity.getTarget();
        if (target == null) {
            return 240;
        }
        ServerLevel level = this.level();
        List<Vec3> offsets = CometRageAction.getRageCometOffsets(this.entity);
        for (int i = 0; i < offsets.size(); ++i) {
            Vec3 offset = offsets.get(i);
            this.eventScheduler.addEvent(new TimedEvent(() -> {
                this.cometThrower().apply(offset).throwProjectile(target.getBoundingBox().getCenter());
                SoundUtils.playSound(level, this.entity.position(), BMDSounds.COMET_SHOOT.get(), SoundSource.HOSTILE, 3.0f, 64.0, null);
            }, 60 + i * 30, 1, this.shouldCancel));
        }
        SoundUtils.playSound(level, this.entity.position(), BMDSounds.RAGE_PREPARE.get(), SoundSource.HOSTILE, 1.0f, 64.0, null);
        return 240;
    }

    // VolleyAction: a fan of five magic missiles.
    private int volley() {
        LivingEntity target = this.entity.getTarget();
        if (target == null) {
            return 80;
        }
        ServerLevel level = this.level();
        Function<Vec3, ProjectileThrower> thrower = this.missileThrower();
        this.eventScheduler.addEvent(new TimedEvent(() -> {
            Vec3 aim = target.getBoundingBox().getCenter();
            for (Vec3 offset : VolleyAction.getMissileLaunchOffsets(this.entity)) {
                thrower.apply(offset).throwProjectile(aim.add(VecUtils.planeProject(offset, VecUtils.yAxis)));
            }
            SoundUtils.playSound(level, this.entity.position(), BMDSounds.MISSILE_SHOOT.get(), SoundSource.HOSTILE, 3.0f, 64.0, null);
        }, 46, 1, this.shouldCancel));
        this.eventScheduler.addEvent(new TimedEvent(() -> SoundUtils.playSound(level, this.entity.position(), BMDSounds.MISSILE_PREPARE.get(), SoundSource.HOSTILE, 4.0f, 64.0, null), 10, 1, this.shouldCancel));
        return 80;
    }

    // VolleyRageAction: four patterned volleys of missiles.
    private int volleyRage() {
        LivingEntity target = this.entity.getTarget();
        if (target == null) {
            return 80;
        }
        ServerLevel level = this.level();
        Function<Vec3, ProjectileThrower> thrower = this.missileThrower();
        int volleys = VolleyRageAction.getRageMissileVolleys(this.entity).size();
        SoundUtils.playSound(level, this.entity.position(), BMDSounds.MISSILE_PREPARE.get(), SoundSource.HOSTILE, 4.0f, 64.0, null);
        for (int i = 0; i < volleys; ++i) {
            int volley = i;
            this.eventScheduler.addEvent(new TimedEvent(() -> {
                Vec3 aim = target.getBoundingBox().getCenter();
                for (Vec3 offset : VolleyRageAction.getRageMissileVolleys(this.entity).get(volley)) {
                    thrower.apply(offset).throwProjectile(aim.add(offset));
                }
                SoundUtils.playSound(level, this.entity.position(), BMDSounds.MISSILE_SHOOT.get(), SoundSource.HOSTILE, 3.0f, 64.0, null);
            }, 60 + volley * 30, 1, this.shouldCancel));
        }
        return 60 + volleys * 30;
    }

    // MinionAction: one phantom near the target.
    private int minions() {
        if (this.entity.getTarget() == null) {
            return 80;
        }
        this.eventScheduler.addEvent(new TimedEvent(this::summonOne, 40, 1, this.shouldCancel));
        return 80;
    }

    // MinionRageAction: nine phantoms in quickening succession (up to the minion cap).
    private int minionRage() {
        List<Integer> delays = new ArrayList<>();
        for (int i = 0; i < MinionRageAction.numMobs; ++i) {
            delays.add(40 + i * 40 - MathUtils.consecutiveSum(0, i) * 3);
        }
        if (this.entity.getTarget() != null) {
            for (int delay : delays) {
                this.eventScheduler.addEvent(new TimedEvent(this::summonOne, delay, 1, this.shouldCancel));
            }
        }
        return delays.get(delays.size() - 1) + 40;
    }

    private void summonOne() {
        LivingEntity target = this.entity.getTarget();
        if (target == null || ServantMinions.count(this.entity) >= ServantConfig.NightLichMaxMinions.get()) {
            return;
        }
        ServerLevel level = this.level();
        CompoundTag tag = MinionAction.summonNbt.copy();
        tag.putString("id", MinionAction.summonId);
        SimpleMobSpawner spawner = new SimpleMobSpawner(level);
        CompoundTagEntityProvider provider = new CompoundTagEntityProvider(tag, level);
        MobEntitySpawnPredicate predicate = new MobEntitySpawnPredicate(level);
        IMobSpawner summonCircleFirst = (pos, summon) -> {
            ParticleUtils.spawnParticle(level, BMDParticles.MAGIC_CIRCLE.get(), pos, Vec3.ZERO, 0, 0.0);
            SoundUtils.playSound(level, pos, BMDSounds.MINION_RUNE.get(), SoundSource.HOSTILE, 1.0f, 64.0, null);
            this.eventScheduler.addEvent(new TimedEvent(() -> {
                // Checked again: other summons may have filled the cap during the rune's delay.
                if (ServantMinions.count(this.entity) < ServantConfig.NightLichMaxMinions.get()) {
                    ServantMinions.tag(summon, this.entity, false);
                    spawner.spawn(pos, summon);
                    this.entity.playSound(BMDSounds.MINION_SUMMON.get(), 0.7f, 1.0f);
                }
            }, 40, 1, this.shouldCancel));
        };
        new MobPlacementLogic(new RangedSpawnPosition(target.position(), 4.0, 8.0), provider, predicate, summonCircleFirst).tryPlacement(30);
    }

    // TeleportAction: blink to a spot 20-35 blocks from the target that can see it.
    private int teleport() {
        LivingEntity target = this.entity.getTarget();
        if (target == null) {
            return 80;
        }
        MobEntitySpawnPredicate canStand = new MobEntitySpawnPredicate(this.level());
        ISpawnPredicate canStandAndSee = (pos, e) -> canStand.canSpawn(pos, e) && this.entity.hasLineOfSight(target);
        if (!this.teleportLogic(target, target.position(), canStandAndSee).tryPlacement(100)) {
            Vec3 safe = VecUtils.asVec3(this.level().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, BlockPos.containing(target.position())));
            this.teleportLogic(target, safe, canStand).tryPlacement(100);
        }
        return 80;
    }

    private MobPlacementLogic teleportLogic(LivingEntity target, Vec3 around, ISpawnPredicate predicate) {
        ServerLevel level = this.level();
        return new MobPlacementLogic(new RangedSpawnPosition(around, 20.0, 35.0), () -> this.entity, predicate, (pos, e) -> this.eventScheduler.addEvent(new TimedEvent(() -> {
            SoundUtils.playSound(level, this.entity.position(), BMDSounds.TELEPORT_PREPARE.get(), SoundSource.HOSTILE, 3.0f, 64.0, null);
            this.entity.collides = false;
            this.eventScheduler.addEvent(new TimedEvent(() -> {
                e.teleportTo(pos.x, pos.y, pos.z);
                e.level().broadcastEntityEvent(e, LichActions.endTeleport);
                SoundUtils.playSound(level, this.entity.position(), BMDSounds.LICH_TELEPORT.get(), SoundSource.HOSTILE, 2.0f, 64.0, null);
                this.entity.collides = true;
            }, 30));
        }, 10, 1, this.shouldCancel)));
    }
}
