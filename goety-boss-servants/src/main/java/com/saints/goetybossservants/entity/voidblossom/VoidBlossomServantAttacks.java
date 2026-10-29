/*
 * Parts of this file are adapted from Bosses of Mass Destruction by Barribob, licensed under the
 * GNU Lesser General Public License v3.0.
 * Modified for Goety Boss Servants: the void blossom's spike, spike wave, spore and blade
 * actions rewritten to work on any target and to spare the owner and their allies.
 * Modified by Saints, 2026.
 */
package com.saints.goetybossservants.entity.voidblossom;

import com.saints.goetybossservants.entity.bmd.Bmd;
import kotlin.Unit;
import net.barribob.boss.cardinalComponents.ModComponents;
import net.barribob.boss.mob.ai.action.CooldownAction;
import net.barribob.boss.mob.ai.action.IActionStop;
import net.barribob.boss.mob.ai.action.IActionWithCooldown;
import net.barribob.boss.mob.ai.goals.ActionGoal;
import net.barribob.boss.mob.mobs.obsidilith.ObsidilithUtils;
import net.barribob.boss.mob.mobs.void_blossom.Spikes;
import net.barribob.boss.mob.mobs.void_blossom.VoidBlossomAttacks;
import net.barribob.boss.mob.mobs.void_blossom.VoidBlossomEntity;
import net.barribob.boss.mob.mobs.void_blossom.hitbox.HitboxId;
import net.barribob.boss.mob.mobs.void_blossom.hitbox.NetworkedHitboxManager;
import net.barribob.boss.mob.utils.ProjectileData;
import net.barribob.boss.mob.utils.ProjectileThrower;
import net.barribob.boss.particle.Particles;
import net.barribob.boss.projectile.PetalBladeProjectile;
import net.barribob.boss.projectile.SporeBallProjectile;
import net.barribob.boss.projectile.util.ExemptEntities;
import net.barribob.boss.utils.ModUtils;
import net.barribob.boss.utils.NetworkUtils;
import net.barribob.maelstrom.general.event.EventScheduler;
import net.barribob.maelstrom.general.event.EventSeries;
import net.barribob.maelstrom.general.random.WeightedRandom;
import net.barribob.maelstrom.static_utilities.MathUtils;
import net.barribob.maelstrom.static_utilities.RandomUtils;
import net.barribob.maelstrom.static_utilities.VecUtilsKt;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

/**
 * The Void Blossom's attacks for the servant. BMD's own actions (SpikeAction, SpikeWaveAction,
 * SporeAction, BladeAction) all refuse to fire unless the target is a player, so these are versions
 * that work on any target. They still use BMD's spikes, projectiles, particles, sounds, hitbox
 * states and client spike packets, so they look and hit the same. The Blossom action is the
 * servant's own (see VoidBlossomServantBlossoms). Friendly fire is handled centrally (ServantEvents).
 */
public class VoidBlossomServantAttacks {
    private static final Vec3 UP = new Vec3(0.0, 1.0, 0.0);

    private final VoidBlossomServant entity;
    private final EventScheduler eventScheduler;
    private final BooleanSupplier shouldCancel;
    private final List<Vec3> spikeCircle = MathUtils.INSTANCE.buildBlockCircle(2.0);
    private final List<Vec3> firstWaveCircle = MathUtils.INSTANCE.buildBlockCircle(7.0);
    private final List<Vec3> secondWaveCircle;
    private final List<Vec3> thirdWaveCircle;
    private final Map<Byte, IActionWithCooldown> actions;

    public VoidBlossomServantAttacks(VoidBlossomServant entity, EventScheduler eventScheduler) {
        this.entity = entity;
        this.eventScheduler = eventScheduler;
        this.shouldCancel = () -> entity.isDeadOrDying() || entity.getTarget() == null;
        this.secondWaveCircle = new ArrayList<>(MathUtils.INSTANCE.buildBlockCircle(14.0));
        this.secondWaveCircle.removeAll(this.firstWaveCircle);
        this.thirdWaveCircle = new ArrayList<>(MathUtils.INSTANCE.buildBlockCircle(21.0));
        this.thirdWaveCircle.removeAll(this.secondWaveCircle);
        this.thirdWaveCircle.removeAll(this.firstWaveCircle);
        this.actions = Map.of(
                VoidBlossomAttacks.spikeAttack, this::spike,
                VoidBlossomAttacks.spikeWaveAttack, this::spikeWave,
                VoidBlossomAttacks.sporeAttack, this::spore,
                VoidBlossomAttacks.bladeAttack, this::blade,
                VoidBlossomAttacks.blossomAction, this::blossom);
    }

    public ActionGoal buildAttackGoal() {
        CooldownAction attackAction = new CooldownAction(this::chooseAttack, 80);
        IActionStop onCancel = () -> {
            this.entity.level().broadcastEntityEvent(this.entity, VoidBlossomAttacks.stopAttackAnimation);
            attackAction.stop();
        };
        return Bmd.actionGoal(() -> !this.shouldCancel.getAsBoolean(), attackAction, onCancel);
    }

    /** BMD's VoidBlossomMoveLogic weights, with the Blossom move when a health milestone was crossed. */
    private int chooseAttack() {
        LivingEntity target = this.entity.getTarget();
        if (target == null) {
            return 20;
        }
        float health = this.entity.getHealth() / this.entity.getMaxHealth();
        double close = target.distanceTo(this.entity) > 21.0f ? 0.0 : 1.0;
        List<Float> milestones = VoidBlossomEntity.Companion.getHpMilestones();
        byte move;
        if (this.entity.takeBlossomPending()) {
            move = VoidBlossomAttacks.blossomAction;
        } else {
            WeightedRandom<Byte> random = new WeightedRandom<>();
            random.add(1.0, VoidBlossomAttacks.spikeAttack);
            random.add(close, VoidBlossomAttacks.spikeWaveAttack);
            random.add(health < milestones.get(3) ? close : 0.0, VoidBlossomAttacks.sporeAttack);
            random.add(health < milestones.get(2) ? 1.0 : 0.0, VoidBlossomAttacks.bladeAttack);
            move = random.next();
        }
        this.entity.level().broadcastEntityEvent(this.entity, move);
        return this.actions.get(move).perform();
    }

    private ServerLevel level() {
        return (ServerLevel) this.entity.level();
    }

    private void setHitbox(HitboxId hitbox) {
        this.entity.getEntityData().set(NetworkedHitboxManager.Companion.getHitbox(), hitbox.getId());
    }

    /** Where a target will probably be: BMD predicts players from their recent movement; mobs, where they are. */
    private static Vec3 predictPosition(LivingEntity target) {
        if (target instanceof ServerPlayer player) {
            return ObsidilithUtils.INSTANCE.approximatePlayerNextPosition(ModComponents.Companion.getPlayerPositions(player), player.position());
        }
        return target.position();
    }

    // SpikeAction: three bursts of spikes under the target.
    private int spike() {
        LivingEntity target = this.entity.getTarget();
        if (target == null) {
            return 80;
        }
        ServerLevel level = this.level();
        Spikes riftBurst = new Spikes(this.entity, level, Particles.INSTANCE.getVOID_BLOSSOM_SPIKE_INDICATOR(), 20, this.eventScheduler, Bmd.test(this.shouldCancel));
        Bmd.playSound(level, this.entity.position(), Bmd.sounds().getVoidBlossomBurrow(), 1.5f, 32.0);
        this.eventScheduler.addEvent(new EventSeries(
                Bmd.after(20, this.shouldCancel, () -> this.setHitbox(HitboxId.Spike)),
                Bmd.after(100, () -> this.setHitbox(HitboxId.Idle))));
        for (int i = 0; i < 3; ++i) {
            this.eventScheduler.addEvent(Bmd.after(30 + i * 30, this.shouldCancel, () -> {
                Vec3 placement = predictPosition(target);
                Bmd.playSound(level, placement, Bmd.sounds().getVoidSpikeIndicator(), 1.0f, 32.0);
                List<BlockPos> spikes = this.spikeCircle.stream().flatMap(point -> riftBurst.tryPlaceRift(placement.add(point)).stream()).toList();
                this.eventScheduler.addEvent(Bmd.after(20, this.shouldCancel, () -> {
                    Bmd.playSound(level, placement, Bmd.sounds().getVoidBlossomSpike(), 1.2f, 32.0);
                    NetworkUtils.Companion.sendSpikePacket(this.entity, spikes);
                }));
            }));
        }
        return 150;
    }

    // SpikeWaveAction: three expanding rings of spikes around the blossom.
    private int spikeWave() {
        if (this.entity.getTarget() == null) {
            return 80;
        }
        ServerLevel level = this.level();
        this.eventScheduler.addEvent(new EventSeries(
                Bmd.after(20, this.shouldCancel, () -> this.setHitbox(HitboxId.SpikeWave1)),
                Bmd.after(26, this.shouldCancel, () -> this.setHitbox(HitboxId.SpikeWave2)),
                Bmd.after(26, this.shouldCancel, () -> this.setHitbox(HitboxId.SpikeWave3)),
                Bmd.after(26, () -> this.setHitbox(HitboxId.Idle))));
        Spikes spikes = new Spikes(this.entity, level, Particles.INSTANCE.getVOID_BLOSSOM_SPIKE_WAVE_INDICATOR(), 30, this.eventScheduler, Bmd.test(this.shouldCancel));
        float pitch = ModUtils.INSTANCE.randomPitch(this.entity.getRandom());
        this.wave(() -> {
            this.burst(spikes, this.firstWaveCircle);
            Bmd.playSound(level, this.entity.position(), Bmd.sounds().getSpikeWaveIndicator(), 2.0f, 0.7f, 64.0);
        }, () -> Bmd.playSound(level, this.entity.position(), Bmd.sounds().getVoidBlossomSpike(), 1.2f, 64.0), 20);
        this.wave(() -> {
            this.burst(spikes, this.secondWaveCircle);
            this.soundsAround(level, 14.0, Bmd.sounds().getSpikeWaveIndicator(), 2.0f, 0.7f);
        }, () -> this.soundsAround(level, 14.0, Bmd.sounds().getVoidBlossomSpike(), 1.2f, pitch), 45);
        this.wave(() -> {
            this.burst(spikes, this.thirdWaveCircle);
            this.soundsAround(level, 21.0, Bmd.sounds().getSpikeWaveIndicator(), 2.0f, 0.7f);
        }, () -> this.soundsAround(level, 21.0, Bmd.sounds().getVoidBlossomSpike(), 1.2f, pitch), 70);
        return 120;
    }

    private void wave(Runnable indicate, Runnable erupt, int delay) {
        this.eventScheduler.addEvent(Bmd.after(delay, this.shouldCancel, () -> {
            indicate.run();
            this.eventScheduler.addEvent(Bmd.after(30, this.shouldCancel, erupt));
        }));
    }

    private void burst(Spikes spikes, List<Vec3> offsets) {
        List<BlockPos> placed = offsets.stream().flatMap(offset -> spikes.tryPlaceRift(this.entity.position().add(offset)).stream()).toList();
        this.eventScheduler.addEvent(Bmd.after(30, this.shouldCancel, () -> NetworkUtils.Companion.sendSpikePacket(this.entity, placed)));
    }

    private void soundsAround(ServerLevel level, double radius, SoundEvent sound, float volume, float pitch) {
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            Vec3 pos = this.entity.position().add(new Vec3(dir.step()).scale(radius));
            Bmd.playSound(level, pos, sound, volume, pitch, 64.0);
        }
    }

    // SporeAction: a spore ball that bursts into a poison cloud where it lands.
    private int spore() {
        LivingEntity target = this.entity.getTarget();
        if (target == null) {
            return 80;
        }
        ServerLevel level = this.level();
        this.eventScheduler.addEvent(new EventSeries(
                Bmd.after(20, this.shouldCancel, () -> this.setHitbox(HitboxId.Spore)),
                Bmd.after(27, () -> this.setHitbox(HitboxId.Idle))));
        this.eventScheduler.addEvent(Bmd.after(26, () -> Bmd.playSound(level, this.entity.position(), Bmd.sounds().getSporePrepare(), 1.5f, 32.0)));
        this.eventScheduler.addEvent(Bmd.after(45, this.shouldCancel, () -> new ProjectileThrower(() -> {
            SporeBallProjectile projectile = new SporeBallProjectile(this.entity, level, new ExemptEntities(List.of(this.entity.getType())));
            projectile.setPos(this.entity.getEyePosition());
            return new ProjectileData(projectile, 0.75f, 0.0f, 0.2);
        }).throwProjectile(target.getEyePosition())));
        return 100;
    }

    // BladeAction: three volleys of petal blades in a line across the target.
    private int blade() {
        LivingEntity target = this.entity.getTarget();
        if (target == null) {
            return 80;
        }
        ServerLevel level = this.level();
        this.eventScheduler.addEvent(new EventSeries(
                Bmd.after(10, this.shouldCancel, () -> this.setHitbox(HitboxId.Petal)),
                Bmd.after(90, () -> this.setHitbox(HitboxId.Idle))));
        Runnable thrower = () -> {
            Vec3 aim = target.getBoundingBox().getCenter();
            Vec3 dir = this.entity.getEyePosition().subtract(aim);
            Vec3 left = dir.cross(UP).normalize();
            double rotation = RandomUtils.INSTANCE.randSign() * 20.0;
            Vec3 angled = VecUtilsKt.rotateVector(left, dir, rotation);
            Vec3 lineStart = aim.add(angled.scale(7.0));
            Vec3 lineEnd = aim.add(angled.scale(-7.0));
            ProjectileThrower projectileThrower = new ProjectileThrower(() -> {
                PetalBladeProjectile projectile = new PetalBladeProjectile(this.entity, level, hit -> Unit.INSTANCE, List.of(this.entity.getType()), (float) rotation);
                projectile.setPos(this.entity.getEyePosition().add(UP));
                projectile.setNoGravity(true);
                return new ProjectileData(projectile, 0.9f, 0.0f, 0.0);
            });
            Bmd.playSound(level, this.entity.position(), Bmd.sounds().getPetalBlade(), 3.0f, ModUtils.INSTANCE.randomPitch(target.getRandom()), 64.0);
            MathUtils.INSTANCE.lineCallback(lineStart, lineEnd, 11, (pos, index) -> {
                projectileThrower.throwProjectile(pos);
                return Unit.INSTANCE;
            });
        };
        this.eventScheduler.addEvent(Bmd.after(28, this.shouldCancel, thrower));
        this.eventScheduler.addEvent(Bmd.after(52, this.shouldCancel, thrower));
        this.eventScheduler.addEvent(Bmd.after(75, this.shouldCancel, thrower));
        return 120;
    }

    // Blossom: healing flowers, placed closer and temporary for the servant.
    private int blossom() {
        this.eventScheduler.addEvent(new EventSeries(
                Bmd.after(20, this.shouldCancel, () -> this.setHitbox(HitboxId.SpikeWave3)),
                Bmd.after(80, () -> this.setHitbox(HitboxId.Idle))));
        this.entity.blossoms().placeAround(this.level(), this.eventScheduler, this.shouldCancel);
        return 120;
    }
}
