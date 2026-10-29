/*
 * Parts of this file are adapted from Bosses of Mass Destruction (Forge port by CerbonXD, original
 * Fabric mod by Barribob), licensed under the GNU Lesser General Public License v3.0.
 * Modified for Goety Boss Servants: the void blossom's spike, spike wave, spore and blade
 * actions rewritten to work on any target and to spare the owner and their allies.
 * Modified by Saints, 2026.
 */
package com.saints.goetybossservants.entity.voidblossom;

import com.cerbon.bosses_of_mass_destruction.entity.ai.action.CooldownAction;
import com.cerbon.bosses_of_mass_destruction.entity.ai.action.IActionStop;
import com.cerbon.bosses_of_mass_destruction.entity.ai.action.IActionWithCooldown;
import com.cerbon.bosses_of_mass_destruction.entity.ai.goals.ActionGoal;
import com.cerbon.bosses_of_mass_destruction.entity.custom.obsidilith.ObsidilithUtils;
import com.cerbon.bosses_of_mass_destruction.entity.custom.void_blossom.Spikes;
import com.cerbon.bosses_of_mass_destruction.entity.custom.void_blossom.VoidBlossomAttacks;
import com.cerbon.bosses_of_mass_destruction.entity.custom.void_blossom.VoidBlossomEntity;
import com.cerbon.bosses_of_mass_destruction.entity.custom.void_blossom.hitbox.HitboxId;
import com.cerbon.bosses_of_mass_destruction.entity.custom.void_blossom.hitbox.NetworkedHitboxManager;
import com.cerbon.bosses_of_mass_destruction.entity.util.ProjectileThrower;
import com.cerbon.bosses_of_mass_destruction.packet.BMDPacketHandler;
import com.cerbon.bosses_of_mass_destruction.packet.custom.SpikeS2CPacket;
import com.cerbon.bosses_of_mass_destruction.particle.BMDParticles;
import com.cerbon.bosses_of_mass_destruction.projectile.PetalBladeProjectile;
import com.cerbon.bosses_of_mass_destruction.projectile.SporeBallProjectile;
import com.cerbon.bosses_of_mass_destruction.projectile.util.ExemptEntities;
import com.cerbon.bosses_of_mass_destruction.sound.BMDSounds;
import com.cerbon.cerbons_api.api.general.event.EventScheduler;
import com.cerbon.cerbons_api.api.general.event.EventSeries;
import com.cerbon.cerbons_api.api.general.event.TimedEvent;
import com.cerbon.cerbons_api.api.general.random.WeightedRandom;
import com.cerbon.cerbons_api.api.static_utilities.CapabilityUtils;
import com.cerbon.cerbons_api.api.static_utilities.MathUtils;
import com.cerbon.cerbons_api.api.static_utilities.RandomUtils;
import com.cerbon.cerbons_api.api.static_utilities.SoundUtils;
import com.cerbon.cerbons_api.api.static_utilities.VecUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * The Void Blossom's attacks for the servant. BMD's own actions (SpikeAction, SpikeWaveAction,
 * SporeAction, BladeAction) all refuse to fire unless the target is a player, so these are copies
 * that work on any target. They still use BMD's spikes, projectiles, particles, sounds, hitbox
 * states and client spike packets, so they look and hit the same. The Blossom action is the
 * servant's own (see VoidBlossomServantBlossoms). Friendly fire is handled centrally (ModEvents).
 */
public class VoidBlossomServantAttacks {
    private final VoidBlossomServant entity;
    private final EventScheduler eventScheduler;
    private final Supplier<Boolean> shouldCancel;
    private final List<Vec3> spikeCircle = MathUtils.buildBlockCircle(2.0);
    private final List<Vec3> firstWaveCircle = MathUtils.buildBlockCircle(7.0);
    private final List<Vec3> secondWaveCircle;
    private final List<Vec3> thirdWaveCircle;
    private final Map<Byte, IActionWithCooldown> actions;

    public VoidBlossomServantAttacks(VoidBlossomServant entity, EventScheduler eventScheduler) {
        this.entity = entity;
        this.eventScheduler = eventScheduler;
        this.shouldCancel = () -> entity.isDeadOrDying() || entity.getTarget() == null;
        this.secondWaveCircle = MathUtils.buildBlockCircle(14.0);
        this.secondWaveCircle.removeAll(this.firstWaveCircle);
        this.thirdWaveCircle = MathUtils.buildBlockCircle(21.0);
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
        return new ActionGoal(() -> !this.shouldCancel.get(), null, attackAction, null, onCancel);
    }

    /** BMD's VoidBlossomMoveLogic weights, with the Blossom move when a health milestone was crossed. */
    private int chooseAttack() {
        LivingEntity target = this.entity.getTarget();
        if (target == null) {
            return 20;
        }
        float health = this.entity.getHealth() / this.entity.getMaxHealth();
        double close = target.distanceTo(this.entity) > 21.0f ? 0.0 : 1.0;
        byte move;
        if (this.entity.takeBlossomPending()) {
            move = VoidBlossomAttacks.blossomAction;
        } else {
            WeightedRandom<Byte> random = new WeightedRandom<>();
            random.add(1.0, VoidBlossomAttacks.spikeAttack);
            random.add(close, VoidBlossomAttacks.spikeWaveAttack);
            random.add(health < VoidBlossomEntity.hpMilestones.get(3) ? close : 0.0, VoidBlossomAttacks.sporeAttack);
            random.add(health < VoidBlossomEntity.hpMilestones.get(2) ? 1.0 : 0.0, VoidBlossomAttacks.bladeAttack);
            move = random.next();
        }
        this.entity.level().broadcastEntityEvent(this.entity, move);
        return this.actions.get(move).perform();
    }

    private ServerLevel level() {
        return (ServerLevel) this.entity.level();
    }

    private void setHitbox(HitboxId hitbox) {
        this.entity.getEntityData().set(NetworkedHitboxManager.hitbox, hitbox.getId());
    }

    /** Where a target will probably be: BMD predicts players from their recent movement; mobs, where they are. */
    private static Vec3 predictPosition(LivingEntity target) {
        if (target instanceof ServerPlayer player) {
            return ObsidilithUtils.approximatePlayerNextPosition(CapabilityUtils.getLastPositions(player), player.position());
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
        Spikes riftBurst = new Spikes(this.entity, level, BMDParticles.VOID_BLOSSOM_SPIKE_INDICATOR.get(), 20, this.eventScheduler, this.shouldCancel);
        SoundUtils.playSound(level, this.entity.position(), BMDSounds.VOID_BLOSSOM_BURROW.get(), SoundSource.HOSTILE, 1.5f, 32.0, null);
        this.eventScheduler.addEvent(new EventSeries(
                new TimedEvent(() -> this.setHitbox(HitboxId.Spike), 20, 1, this.shouldCancel),
                new TimedEvent(() -> this.setHitbox(HitboxId.Idle), 100)));
        for (int i = 0; i < 3; ++i) {
            this.eventScheduler.addEvent(new TimedEvent(() -> {
                Vec3 placement = predictPosition(target);
                SoundUtils.playSound(level, placement, BMDSounds.VOID_SPIKE_INDICATOR.get(), SoundSource.HOSTILE, 1.0f, 32.0, null);
                List<BlockPos> spikes = this.spikeCircle.stream().flatMap(point -> riftBurst.tryPlaceRift(placement.add(point)).stream()).toList();
                this.eventScheduler.addEvent(new TimedEvent(() -> {
                    SoundUtils.playSound(level, placement, BMDSounds.VOID_BLOSSOM_SPIKE.get(), SoundSource.HOSTILE, 1.2f, 32.0, null);
                    BMDPacketHandler.sendToAllPlayersTrackingChunk(new SpikeS2CPacket(this.entity.getId(), spikes), level, this.entity.position());
                }, 20, 1, this.shouldCancel));
            }, 30 + i * 30, 1, this.shouldCancel));
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
                new TimedEvent(() -> this.setHitbox(HitboxId.SpikeWave1), 20, 1, this.shouldCancel),
                new TimedEvent(() -> this.setHitbox(HitboxId.SpikeWave2), 26, 1, this.shouldCancel),
                new TimedEvent(() -> this.setHitbox(HitboxId.SpikeWave3), 26, 1, this.shouldCancel),
                new TimedEvent(() -> this.setHitbox(HitboxId.Idle), 26, 1, () -> false)));
        Spikes spikes = new Spikes(this.entity, level, BMDParticles.VOID_BLOSSOM_SPIKE_WAVE_INDICATOR.get(), 30, this.eventScheduler, this.shouldCancel);
        this.wave(() -> {
            this.burst(spikes, this.firstWaveCircle);
            SoundUtils.playSound(level, this.entity.position(), BMDSounds.SPIKE_WAVE_INDICATOR.get(), SoundSource.HOSTILE, 2.0f, 0.7f, 64.0, null);
        }, () -> SoundUtils.playSound(level, this.entity.position(), BMDSounds.VOID_BLOSSOM_SPIKE.get(), SoundSource.HOSTILE, 1.2f, 64.0, null), 20);
        this.wave(() -> {
            this.burst(spikes, this.secondWaveCircle);
            this.soundsAround(level, 14.0, BMDSounds.SPIKE_WAVE_INDICATOR.get(), 2.0f, 0.7f);
        }, () -> this.soundsAround(level, 14.0, BMDSounds.VOID_BLOSSOM_SPIKE.get(), 1.2f, SoundUtils.randomPitch(this.entity.getRandom())), 45);
        this.wave(() -> {
            this.burst(spikes, this.thirdWaveCircle);
            this.soundsAround(level, 21.0, BMDSounds.SPIKE_WAVE_INDICATOR.get(), 2.0f, 0.7f);
        }, () -> this.soundsAround(level, 21.0, BMDSounds.VOID_BLOSSOM_SPIKE.get(), 1.2f, SoundUtils.randomPitch(this.entity.getRandom())), 70);
        return 120;
    }

    private void wave(Runnable indicate, Runnable erupt, int delay) {
        this.eventScheduler.addEvent(new TimedEvent(() -> {
            indicate.run();
            this.eventScheduler.addEvent(new TimedEvent(erupt, 30, 1, this.shouldCancel));
        }, delay, 1, this.shouldCancel));
    }

    private void burst(Spikes spikes, List<Vec3> offsets) {
        List<BlockPos> placed = offsets.stream().flatMap(offset -> spikes.tryPlaceRift(this.entity.position().add(offset)).stream()).toList();
        this.eventScheduler.addEvent(new TimedEvent(() -> BMDPacketHandler.sendToAllPlayersTrackingChunk(
                new SpikeS2CPacket(this.entity.getId(), placed), this.level(), this.entity.position()), 30, 1, this.shouldCancel));
    }

    private void soundsAround(ServerLevel level, double radius, SoundEvent sound, float volume, float pitch) {
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            Vec3 pos = this.entity.position().add(new Vec3(dir.step()).scale(radius));
            SoundUtils.playSound(level, pos, sound, SoundSource.HOSTILE, volume, pitch, 64.0, null);
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
                new TimedEvent(() -> this.setHitbox(HitboxId.Spore), 20, 1, this.shouldCancel),
                new TimedEvent(() -> this.setHitbox(HitboxId.Idle), 27, 1, () -> false)));
        this.eventScheduler.addEvent(new TimedEvent(() -> SoundUtils.playSound(level, this.entity.position(), BMDSounds.SPORE_PREPARE.get(), SoundSource.HOSTILE, 1.5f, 32.0, null), 26));
        this.eventScheduler.addEvent(new TimedEvent(() -> new ProjectileThrower(() -> {
            SporeBallProjectile projectile = new SporeBallProjectile(this.entity, level, new ExemptEntities(List.of(this.entity.getType())));
            projectile.setPos(this.entity.getEyePosition());
            return new ProjectileThrower.ProjectileData(projectile, 0.75f, 0.0f, 0.2);
        }).throwProjectile(target.getEyePosition()), 45, 1, this.shouldCancel));
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
                new TimedEvent(() -> this.setHitbox(HitboxId.Petal), 10, 1, this.shouldCancel),
                new TimedEvent(() -> this.setHitbox(HitboxId.Idle), 90)));
        Runnable thrower = () -> {
            Vec3 aim = target.getBoundingBox().getCenter();
            Vec3 dir = this.entity.getEyePosition().subtract(aim);
            Vec3 left = dir.cross(VecUtils.yAxis).normalize();
            double rotation = RandomUtils.randSign() * 20.0;
            Vec3 angled = VecUtils.rotateVector(left, dir, rotation);
            Vec3 lineStart = aim.add(angled.scale(7.0));
            Vec3 lineEnd = aim.add(angled.scale(-7.0));
            ProjectileThrower projectileThrower = new ProjectileThrower(() -> {
                PetalBladeProjectile projectile = new PetalBladeProjectile(this.entity, level, hit -> {
                }, List.of(this.entity.getType()), (float) rotation);
                projectile.setPos(this.entity.getEyePosition().add(VecUtils.yAxis));
                projectile.setNoGravity(true);
                return new ProjectileThrower.ProjectileData(projectile, 0.9f, 0.0f, 0.0);
            });
            SoundUtils.playSound(level, this.entity.position(), BMDSounds.PETAL_BLADE.get(), SoundSource.HOSTILE, 3.0f, SoundUtils.randomPitch(target.getRandom()), 64.0, null);
            MathUtils.lineCallback(lineStart, lineEnd, 11, (pos, index) -> projectileThrower.throwProjectile(pos));
        };
        this.eventScheduler.addEvent(new TimedEvent(thrower, 28, 1, this.shouldCancel));
        this.eventScheduler.addEvent(new TimedEvent(thrower, 52, 1, this.shouldCancel));
        this.eventScheduler.addEvent(new TimedEvent(thrower, 75, 1, this.shouldCancel));
        return 120;
    }

    // Blossom: healing flowers, placed closer and temporary for the servant.
    private int blossom() {
        this.eventScheduler.addEvent(new EventSeries(
                new TimedEvent(() -> this.setHitbox(HitboxId.SpikeWave3), 20, 1, this.shouldCancel),
                new TimedEvent(() -> this.setHitbox(HitboxId.Idle), 80)));
        this.entity.blossoms().placeAround(this.level(), this.eventScheduler, this.shouldCancel);
        return 120;
    }
}
