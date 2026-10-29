package com.saints.goetybossservants.entity.monstrosity;

import com.Polarice3.Goety.client.particles.SlamParticleOption;
import com.Polarice3.Goety.common.entities.ally.golem.RedstoneMonstrosity;
import com.Polarice3.Goety.common.entities.util.CameraShake;
import com.Polarice3.Goety.utils.ColorUtil;
import com.Polarice3.Goety.utils.MathHelper;
import com.saints.goetybossservants.config.ServantConfig;
import com.saints.goetybossservants.registry.ModEffects;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The Netherite Monstrosity's special, in three beats. Its slam splits the ground into molten
 * fissures (glowing cracks, see NetheriteRift) that race outward, scorching and flinging
 * everything they pass. Then it hurls itself into the air at the strongest enemy left and comes
 * down on it like a meteor, playing the slam again as it lands: a crater of fire that crushes,
 * launches and Sunders everything near (half armor and toughness). Server side; one runs per
 * monstrosity, ticked by it. (When it has no minions out, its special calls them up instead.)
 */
public class NetheriteCataclysm {
    static final double FISSURE_RADIUS = 16.0;
    private static final int FISSURES = 12;
    private static final int WAVE_TICKS = 14;
    private static final double LEAP_RANGE = 24.0;
    private static final double LEAP_HEIGHT = 9.0;
    private static final double IMPACT_RADIUS = 7.0;
    /** The animation event that replays the slam (Goety's events stop at 7). */
    public static final byte REPLAY_SLAM = 61;
    private static final int LEAP_START = MathHelper.secondsToTicks(RedstoneMonstrosity.SUMMON_SECONDS_TIME);

    private final MaterialMonstrosity monstrosity;
    private final Vec3 origin;
    private final double angle;
    private final Set<LivingEntity> scorched = new HashSet<>();
    private int tick;
    @Nullable
    private Vec3 from;
    @Nullable
    private Vec3 to;

    NetheriteCataclysm(MaterialMonstrosity monstrosity) {
        this.monstrosity = monstrosity;
        this.origin = monstrosity.position();
        this.angle = monstrosity.getRandom().nextDouble() * Math.PI * 2.0;
    }

    /** Runs one tick; false once it is over. */
    boolean tick(ServerLevel level) {
        if (!this.monstrosity.isAlive()) {
            return false;
        }
        if (this.from == null) {
            ++this.tick;
            this.fissures(level);
            return this.tick < WAVE_TICKS || this.leap(level);
        }
        return this.fly(level);
    }

    private float damage(double multiplier) {
        return (float) (this.monstrosity.getAttributeValue(Attributes.ATTACK_DAMAGE) * multiplier);
    }

    // ----- molten fissures -------------------------------------------------------------------------

    private void fissures(ServerLevel level) {
        double inner = 2.0 + (this.tick - 1) * (FISSURE_RADIUS - 2.0) / WAVE_TICKS;
        double outer = 2.0 + this.tick * (FISSURE_RADIUS - 2.0) / WAVE_TICKS;
        BlockParticleOption magma = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.MAGMA_BLOCK.defaultBlockState());
        for (int i = 0; i < FISSURES; ++i) {
            double a = this.angle + i * Math.PI * 2.0 / FISSURES;
            if (this.tick == 1) {
                // The crack itself, splitting open along with the wave.
                NetheriteRift.fissure(level, this.origin, a, FISSURE_RADIUS, WAVE_TICKS);
            }
            double x = this.origin.x + Math.cos(a) * outer, z = this.origin.z + Math.sin(a) * outer;
            double y = this.groundY(level, x, z);
            level.sendParticles(ParticleTypes.LAVA, x, y + 0.2, z, 2, 0.3, 0.1, 0.3, 0.0);
            level.sendParticles(ParticleTypes.FLAME, x, y + 0.3, z, 6, 0.35, 0.4, 0.35, 0.04);
            level.sendParticles(magma, x, y + 0.2, z, 6, 0.4, 0.2, 0.4, 0.15);
            level.sendParticles(ParticleTypes.LARGE_SMOKE, x, y + 0.5, z, 1, 0.2, 0.3, 0.2, 0.02);
            if (this.tick == WAVE_TICKS) {
                this.pillar(level, x, y, z);
            }
        }
        if (this.tick % 3 == 1) {
            this.monstrosity.playSound(SoundEvents.FIRECHARGE_USE, 2.5f, 0.5f + this.tick * 0.03f);
        }
        float damage = this.damage(0.6);
        for (LivingEntity target : MonstrositySpecials.enemiesAround(level, this.monstrosity, outer)) {
            if (this.scorched.contains(target) || horizontal(target.position(), this.origin) < inner - 1.0) {
                continue;
            }
            this.scorched.add(target);
            target.hurt(this.monstrosity.getServantAttack(), damage);
            target.setSecondsOnFire(5);
            MonstrositySpecials.launch(target, this.origin, 0.4, 0.9);
            this.pillar(level, target.getX(), target.getY(), target.getZ());
        }
    }

    // A column of fire bursting up from the ground.
    private void pillar(ServerLevel level, double x, double y, double z) {
        for (int i = 0; i < 6; ++i) {
            level.sendParticles(ParticleTypes.FLAME, x, y + i * 0.8, z, 5, 0.25, 0.3, 0.25, 0.02);
        }
        level.sendParticles(ParticleTypes.LAVA, x, y + 1.0, z, 4, 0.3, 0.6, 0.3, 0.0);
    }

    private double groundY(ServerLevel level, double x, double z) {
        int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(x), Mth.floor(z));
        return Math.abs(top - this.origin.y) <= 4.0 ? top : this.origin.y;
    }

    private static double horizontal(Vec3 a, Vec3 b) {
        return Math.sqrt(Mth.square(a.x - b.x) + Mth.square(a.z - b.z));
    }

    // ----- the leap ----------------------------------------------------------------------------------

    /** Picks the strongest enemy in reach and jumps at it; false (over) when there is none. */
    private boolean leap(ServerLevel level) {
        LivingEntity target = MonstrositySpecials.enemiesAround(level, this.monstrosity, LEAP_RANGE).stream()
                .filter(LivingEntity::isAlive)
                .max(Comparator.comparingDouble(LivingEntity::getMaxHealth).thenComparingDouble(entity -> -entity.distanceToSqr(this.monstrosity)))
                .orElse(null);
        if (target == null) {
            return false;
        }
        this.from = this.monstrosity.position();
        this.to = this.landing(level, target);
        // Replays the summon attack from the start: its slam comes down as it lands.
        this.monstrosity.summonTick = LEAP_START;
        level.broadcastEntityEvent(this.monstrosity, REPLAY_SLAM);
        this.monstrosity.playSound(SoundEvents.RAVAGER_ROAR, 3.0f, 0.5f);
        this.monstrosity.playSound(SoundEvents.FIRECHARGE_USE, 3.0f, 0.4f);
        return true;
    }

    // Where it comes down: on the target if it fits there among the blocks (it lands on mobs, its
    // own minions included, regardless), otherwise where it is.
    private Vec3 landing(ServerLevel level, LivingEntity target) {
        for (int dy = 2; dy >= -4; --dy) {
            Vec3 pos = new Vec3(target.getX(), Math.floor(target.getY()) + dy, target.getZ());
            AABB box = this.monstrosity.getDimensions(this.monstrosity.getPose()).makeBoundingBox(pos);
            if (!level.getBlockCollisions(this.monstrosity, box).iterator().hasNext() && level.getBlockCollisions(this.monstrosity, box.move(0.0, -0.5, 0.0)).iterator().hasNext()) {
                return pos;
            }
        }
        return this.monstrosity.position();
    }

    private boolean fly(ServerLevel level) {
        int flight = LEAP_START - MaterialMonstrosity.SLAM_TICK;
        double progress = Mth.clamp((LEAP_START - this.monstrosity.summonTick) / (double) flight, 0.0, 1.0);
        Vec3 pos = this.from.lerp(this.to, progress).add(0.0, 4.0 * LEAP_HEIGHT * progress * (1.0 - progress), 0.0);
        this.monstrosity.getNavigation().stop();
        this.monstrosity.setDeltaMovement(Vec3.ZERO);
        this.monstrosity.fallDistance = 0.0f;
        this.monstrosity.setPos(pos.x, pos.y, pos.z);
        Vec3 heading = this.to.subtract(this.from);
        if (heading.horizontalDistanceSqr() > 0.01) {
            float yaw = (float) (Mth.atan2(heading.z, heading.x) * Mth.RAD_TO_DEG) - 90.0f;
            this.monstrosity.setYRot(yaw);
            this.monstrosity.setYBodyRot(yaw);
            this.monstrosity.setYHeadRot(yaw);
        }
        // A trail of fire and smoke behind it.
        level.sendParticles(ParticleTypes.FLAME, pos.x, pos.y + 1.5, pos.z, 12, 1.2, 1.5, 1.2, 0.02);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, pos.x, pos.y + 2.0, pos.z, 4, 1.0, 1.0, 1.0, 0.02);
        level.sendParticles(ParticleTypes.LAVA, pos.x, pos.y + 1.0, pos.z, 2, 1.0, 1.0, 1.0, 0.0);
        if (progress >= 1.0 || this.monstrosity.summonTick <= MaterialMonstrosity.SLAM_TICK) {
            this.monstrosity.setPos(this.to.x, this.to.y, this.to.z);
            this.impact(level);
            return false;
        }
        return true;
    }

    // ----- the landing ---------------------------------------------------------------------------

    private void impact(ServerLevel level) {
        Vec3 pos = this.monstrosity.position();
        MonstrositySpecials.slamEffects(level, this.monstrosity, IMPACT_RADIUS);
        NetheriteRift.crater(level, pos, IMPACT_RADIUS);
        level.sendParticles(new SlamParticleOption(new ColorUtil(0xFFB040), (float) IMPACT_RADIUS * 3.0f, 30), pos.x, pos.y + 0.1, pos.z, 1, 0.0, 0.0, 0.0, 0.0);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, pos.x, pos.y + 1.0, pos.z, 1, 0.0, 0.0, 0.0, 0.0);
        level.sendParticles(ParticleTypes.LAVA, pos.x, pos.y + 0.5, pos.z, 60, IMPACT_RADIUS / 2.0, 0.5, IMPACT_RADIUS / 2.0, 0.0);
        for (int i = 0; i < 24; ++i) {
            double a = i * Math.PI * 2.0 / 24.0;
            this.pillar(level, pos.x + Math.cos(a) * IMPACT_RADIUS * 0.8, this.groundY(level, pos.x + Math.cos(a) * IMPACT_RADIUS * 0.8, pos.z + Math.sin(a) * IMPACT_RADIUS * 0.8),
                    pos.z + Math.sin(a) * IMPACT_RADIUS * 0.8);
        }
        CameraShake.cameraShake(level, pos, 40.0f, 0.6f, 0, 30);
        this.monstrosity.playSound(SoundEvents.GENERIC_EXPLODE, 4.0f, 0.5f);
        this.monstrosity.playSound(SoundEvents.ANVIL_LAND, 3.0f, 0.5f);
        float damage = this.damage(1.5);
        int sunder = MathHelper.secondsToTicks(ServantConfig.NetheriteMonstrositySunderSeconds.get());
        List<LivingEntity> targets = MonstrositySpecials.enemiesAround(level, this.monstrosity, IMPACT_RADIUS);
        for (LivingEntity target : targets) {
            target.hurt(this.monstrosity.getServantAttack(), damage);
            if (target.isAlive()) {
                target.addEffect(new MobEffectInstance(ModEffects.SUNDERED, sunder, 0), this.monstrosity);
                target.setSecondsOnFire(6);
                MonstrositySpecials.launch(target, pos, 1.8, 1.0);
            }
        }
    }
}
