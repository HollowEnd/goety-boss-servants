/*
 * Parts of this file are adapted from Bosses of Mass Destruction by Barribob, licensed under the
 * GNU Lesser General Public License v3.0.
 * Modified for Goety Boss Servants: the lich's comet, volley, minion and teleport actions
 * rewritten to work on any target, with capped minions and a different attack schedule.
 * Modified by Saints, 2026.
 */
package com.saints.goetybossservants.entity.lich;

import com.saints.goetybossservants.config.ServantConfig;
import com.saints.goetybossservants.entity.bmd.Bmd;
import com.saints.goetybossservants.entity.servant.ServantMinions;
import net.barribob.boss.config.LichConfig;
import net.barribob.boss.mob.ai.action.CooldownAction;
import net.barribob.boss.mob.ai.action.IActionStop;
import net.barribob.boss.mob.ai.action.IActionWithCooldown;
import net.barribob.boss.mob.ai.action.ThrowProjectileAction;
import net.barribob.boss.mob.ai.goals.ActionGoal;
import net.barribob.boss.mob.mobs.lich.CometAction;
import net.barribob.boss.mob.mobs.lich.CometRageAction;
import net.barribob.boss.mob.mobs.lich.LichActions;
import net.barribob.boss.mob.mobs.lich.MinionAction;
import net.barribob.boss.mob.mobs.lich.VolleyAction;
import net.barribob.boss.mob.mobs.lich.VolleyRageAction;
import net.barribob.boss.mob.spawn.CompoundTagEntityProvider;
import net.barribob.boss.mob.spawn.IMobSpawner;
import net.barribob.boss.mob.spawn.ISpawnPredicate;
import net.barribob.boss.mob.spawn.MobEntitySpawnPredicate;
import net.barribob.boss.mob.spawn.MobPlacementLogic;
import net.barribob.boss.mob.spawn.RangedSpawnPosition;
import net.barribob.boss.mob.spawn.SimpleMobSpawner;
import net.barribob.boss.mob.utils.ProjectileData;
import net.barribob.boss.mob.utils.ProjectileThrower;
import net.barribob.boss.particle.Particles;
import net.barribob.boss.projectile.MagicMissileProjectile;
import net.barribob.boss.projectile.comet.CometProjectile;
import net.barribob.maelstrom.general.event.EventScheduler;
import net.barribob.maelstrom.general.random.WeightedRandom;
import net.barribob.maelstrom.static_utilities.MathUtils;
import net.barribob.maelstrom.static_utilities.MobUtilsKt;
import net.barribob.maelstrom.static_utilities.VecUtilsKt;
import kotlin.Unit;
import net.barribob.maelstrom.general.random.ModRandom;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

/**
 * The Night Lich's attacks for the servant. BMD's own actions (CometAction, VolleyAction,
 * MinionAction, TeleportAction and their rage versions) refuse to fire unless the target is a player,
 * so these are versions that work on any target, using BMD's comets, missiles, sounds and particles.
 *
 * <p>Its second phase is BMD's: each time damage takes it past 75/50/25% health, its next moves are
 * the comet rage, the missile-volley rage and the minion rage, in that order. Minions are vanilla
 * phantoms, as on the boss, but tagged to this lich and capped (see ServantMinions). Teleporting toward
 * the target only happens while it's free to move (not staying or guarding).
 */
public class NightLichServantAttacks {
    /** BMD's minion rage summons nine. */
    private static final int RAGE_MINIONS = 9;

    private final NightLichServant entity;
    private final LichConfig config;
    private final EventScheduler eventScheduler;
    private final BooleanSupplier shouldCancel;
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
        return Bmd.actionGoal(() -> !this.shouldCancel.getAsBoolean(), attackAction, onCancel);
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

    /** Projectiles of BMD's own lich minions (phantoms) don't hit them; nor do the servant's. */
    private List<EntityType<?>> exempt() {
        EntityType<?> minion = MinionAction.Companion.getSummonEntityType();
        return minion != null ? List.of(minion) : List.of();
    }

    private Function<Vec3, ProjectileThrower> cometThrower() {
        return offset -> new ProjectileThrower(() -> {
            CometProjectile projectile = new CometProjectile(this.entity, this.entity.level(), pos -> {
                this.entity.level().explode(this.entity, pos.x, pos.y, pos.z, this.config.getComet().getExplosionStrength(), Level.ExplosionInteraction.MOB);
                return Unit.INSTANCE;
            }, this.exempt());
            MobUtilsKt.setPos(projectile, MobUtilsKt.eyePos(this.entity).add(offset));
            return new ProjectileData(projectile, 1.6f, 0.0f, 0.2);
        });
    }

    private Function<Vec3, ProjectileThrower> missileThrower() {
        Optional<MobEffect> effect = Optional.ofNullable(BuiltInRegistries.MOB_EFFECT.get(ResourceLocation.tryParse(this.config.getMissile().getStatusEffectId())));
        int duration = this.config.getMissile().getStatusEffectDuration();
        int amplifier = this.config.getMissile().getStatusEffectPotency();
        return offset -> new ProjectileThrower(() -> {
            MagicMissileProjectile projectile = new MagicMissileProjectile(this.entity, this.entity.level(), hit -> {
                effect.ifPresent(e -> hit.addEffect(new MobEffectInstance(e, duration, amplifier)));
                return Unit.INSTANCE;
            }, this.exempt());
            MobUtilsKt.setPos(projectile, MobUtilsKt.eyePos(this.entity).add(offset));
            return new ProjectileData(projectile, 1.6f, 0.0f, 0.2);
        });
    }

    // CometAction: one exploding comet.
    private int comet() {
        if (this.entity.getTarget() == null) {
            return 80;
        }
        ServerLevel level = this.level();
        this.eventScheduler.addEvent(Bmd.after(10, this.shouldCancel, () -> Bmd.playSound(level, this.entity.position(), Bmd.sounds().getCometPrepare(), 3.0f, 64.0)));
        this.eventScheduler.addEvent(Bmd.after(60, this.shouldCancel, () -> {
            new ThrowProjectileAction(this.entity, this.cometThrower().apply(CometAction.Companion.getCometLaunchOffset())).perform();
            Bmd.playSound(level, this.entity.position(), Bmd.sounds().getCometShoot(), 3.0f, 64.0);
        }));
        return 80;
    }

    // CometRageAction: six comets in a ring, one after another.
    private int cometRage() {
        LivingEntity target = this.entity.getTarget();
        if (target == null) {
            return 240;
        }
        ServerLevel level = this.level();
        List<Vec3> offsets = CometRageAction.Companion.getRageCometOffsets(this.entity);
        for (int i = 0; i < offsets.size(); ++i) {
            Vec3 offset = offsets.get(i);
            this.eventScheduler.addEvent(Bmd.after(60 + i * 30, this.shouldCancel, () -> {
                this.cometThrower().apply(offset).throwProjectile(target.getBoundingBox().getCenter());
                Bmd.playSound(level, this.entity.position(), Bmd.sounds().getCometShoot(), 3.0f, 64.0);
            }));
        }
        Bmd.playSound(level, this.entity.position(), Bmd.sounds().getRagePrepare(), 1.0f, 64.0);
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
        this.eventScheduler.addEvent(Bmd.after(46, this.shouldCancel, () -> {
            Vec3 aim = target.getBoundingBox().getCenter();
            for (Vec3 offset : VolleyAction.Companion.getMissileLaunchOffsets(this.entity)) {
                thrower.apply(offset).throwProjectile(aim.add(VecUtilsKt.planeProject(offset, new Vec3(0.0, 1.0, 0.0))));
            }
            Bmd.playSound(level, this.entity.position(), Bmd.sounds().getMissileShoot(), 3.0f, 64.0);
        }));
        this.eventScheduler.addEvent(Bmd.after(10, this.shouldCancel, () -> Bmd.playSound(level, this.entity.position(), Bmd.sounds().getMissilePrepare(), 4.0f, 64.0)));
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
        int volleys = VolleyRageAction.Companion.getRageMissileVolleys(this.entity).size();
        Bmd.playSound(level, this.entity.position(), Bmd.sounds().getMissilePrepare(), 4.0f, 64.0);
        for (int i = 0; i < volleys; ++i) {
            int volley = i;
            this.eventScheduler.addEvent(Bmd.after(60 + volley * 30, this.shouldCancel, () -> {
                Vec3 aim = target.getBoundingBox().getCenter();
                for (Vec3 offset : VolleyRageAction.Companion.getRageMissileVolleys(this.entity).get(volley)) {
                    thrower.apply(offset).throwProjectile(aim.add(offset));
                }
                Bmd.playSound(level, this.entity.position(), Bmd.sounds().getMissileShoot(), 3.0f, 64.0);
            }));
        }
        return 60 + volleys * 30;
    }

    // MinionAction: one phantom near the target.
    private int minions() {
        if (this.entity.getTarget() == null) {
            return 80;
        }
        this.eventScheduler.addEvent(Bmd.after(40, this.shouldCancel, this::summonOne));
        return 80;
    }

    // MinionRageAction: nine phantoms in quickening succession (up to the minion cap).
    private int minionRage() {
        List<Integer> delays = new ArrayList<>();
        for (int i = 0; i < RAGE_MINIONS; ++i) {
            delays.add(40 + i * 40 - MathUtils.INSTANCE.consecutiveSum(0, i) * 3);
        }
        if (this.entity.getTarget() != null) {
            for (int delay : delays) {
                this.eventScheduler.addEvent(Bmd.after(delay, this.shouldCancel, this::summonOne));
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
        CompoundTag tag = MinionAction.Companion.getSummonNbt().copy();
        tag.putString("id", MinionAction.summonId);
        SimpleMobSpawner spawner = new SimpleMobSpawner(level);
        CompoundTagEntityProvider provider = new CompoundTagEntityProvider(tag, level, net.barribob.boss.Mod.INSTANCE.getLOGGER());
        MobEntitySpawnPredicate predicate = new MobEntitySpawnPredicate(level);
        IMobSpawner summonCircleFirst = (pos, summon) -> {
            Bmd.spawnParticle(level, Particles.INSTANCE.getLICH_MAGIC_CIRCLE(), pos, Vec3.ZERO, 0, 0.0);
            Bmd.playSound(level, pos, Bmd.sounds().getMinionRune(), 1.0f, 64.0);
            this.eventScheduler.addEvent(Bmd.after(40, this.shouldCancel, () -> {
                // Checked again: other summons may have filled the cap during the rune's delay.
                if (ServantMinions.count(this.entity) < ServantConfig.NightLichMaxMinions.get()) {
                    ServantMinions.tag(summon, this.entity, false);
                    spawner.spawn(pos, summon);
                    this.entity.playSound(Bmd.sounds().getMinionSummon(), 0.7f, 1.0f);
                }
            }));
        };
        new MobPlacementLogic(new RangedSpawnPosition(target.position(), 4.0, 8.0, new ModRandom()), provider, predicate, summonCircleFirst).tryPlacement(30);
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
            Vec3 safe = Vec3.atLowerCornerOf(this.level().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, BlockPos.containing(target.position())));
            this.teleportLogic(target, safe, canStand).tryPlacement(100);
        }
        return 80;
    }

    private MobPlacementLogic teleportLogic(LivingEntity target, Vec3 around, ISpawnPredicate predicate) {
        ServerLevel level = this.level();
        return new MobPlacementLogic(new RangedSpawnPosition(around, 20.0, 35.0, new ModRandom()), () -> this.entity, predicate,
                (pos, e) -> this.eventScheduler.addEvent(Bmd.after(10, this.shouldCancel, () -> {
                    Bmd.playSound(level, this.entity.position(), Bmd.sounds().getTeleportPrepare(), 3.0f, 64.0);
                    this.entity.setCollides(false);
                    this.eventScheduler.addEvent(Bmd.after(30, () -> {
                        e.teleportTo(pos.x, pos.y, pos.z);
                        e.level().broadcastEntityEvent(e, LichActions.endTeleport);
                        Bmd.playSound(level, this.entity.position(), Bmd.sounds().getLichTeleport(), 2.0f, 64.0);
                        this.entity.setCollides(true);
                    }));
                })));
    }
}
