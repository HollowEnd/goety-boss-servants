package com.saints.goetybossservants.entity.monstrosity;

import com.saints.goetybossservants.registry.ModRegistry;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Comparator;

/**
 * One of the sculk tentacles the Sculk Monstrosity's special calls up out of the ground beside its
 * foes (its own tentacle arm, at the same size): it rises, rears back and slams down on the nearest
 * foe, hurting everything along it and leaving a line of sculk mire there, does that once
 * more, then sinks back into the ground. The server picks its targets and deals the damage; its
 * pose comes from its age alone (see curl), so the client draws it in step (SculkTentacleRenderer).
 */
public class SculkTentacle extends Entity {
    public static final int RISE_TICKS = 10;
    public static final int SINK_TICKS = 10;
    /** When each of its two slams starts rearing back. */
    private static final int[] REAR = {12, 44};
    private static final int REAR_TICKS = 12;
    private static final int STRIKE_TICKS = 4;
    private static final int LIE_TICKS = 8;
    private static final int LIFT_TICKS = 8;
    public static final int LIFE = REAR[1] + REAR_TICKS + STRIKE_TICKS + LIE_TICKS + LIFT_TICKS + SINK_TICKS;
    /** How far along the ground a slam reaches (its length), in blocks. */
    public static final double REACH = 6.0;
    private static final double HALF_WIDTH = 1.0;

    @Nullable
    private MaterialMonstrosity owner;
    private float damage;

    public SculkTentacle(EntityType<? extends SculkTentacle> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    /** A tentacle rising out of the ground at {@code at} (on the floor there), first slamming toward {@code yaw}. */
    public static void spawn(ServerLevel level, MaterialMonstrosity owner, Vec3 at, float yaw, float damage) {
        SculkTentacle tentacle = ModRegistry.SCULK_TENTACLE.get().create(level);
        if (tentacle == null) {
            return;
        }
        // On the floor at the height its foe stands at (not a roof or ledge over it).
        double floor = Ground.floorAt(level, at.x, at.y, at.z);
        tentacle.owner = owner;
        tentacle.damage = damage;
        tentacle.moveTo(at.x, floor, at.z, yaw, 0.0f);
        level.addFreshEntity(tentacle);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SCULK.defaultBlockState()), tentacle.getX(), tentacle.getY() + 0.2, tentacle.getZ(),
                30, 0.6, 0.2, 0.6, 0.1);
        tentacle.playSound(SoundEvents.SCULK_BLOCK_BREAK, 2.0f, 0.5f);
    }

    /** How far out of the ground it is, 0 to 1, at {@code age} ticks. */
    public static float risen(float age) {
        return Mth.clamp(Math.min(age / RISE_TICKS, (LIFE - age) / SINK_TICKS), 0.0f, 1.0f);
    }

    /**
     * How it is bent at {@code age} ticks, toward the way it faces: -1 reared all the way back, 0
     * upright, 1 lying flat along the ground where it slammed.
     */
    public static float curl(float age) {
        for (int rear : REAR) {
            float t = age - rear;
            if (t < 0.0f) {
                break;
            }
            if (t < REAR_TICKS) {
                return -Mth.sin(t / REAR_TICKS * Mth.HALF_PI);
            }
            t -= REAR_TICKS;
            if (t < STRIKE_TICKS) {
                // Whipping down, faster and faster.
                float strike = t / STRIKE_TICKS;
                return -1.0f + 2.0f * strike * strike;
            }
            t -= STRIKE_TICKS + LIE_TICKS;
            if (t < 0.0f) {
                return 1.0f;
            }
            if (t < LIFT_TICKS) {
                return 1.0f - t / LIFT_TICKS;
            }
        }
        return 0.0f;
    }

    /** The way it slams (along the ground). */
    public Vec3 direction() {
        float yaw = this.getYRot() * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.sin(yaw), 0.0, Mth.cos(yaw));
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    public void tick() {
        super.tick();
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        if (this.owner == null || !this.owner.isAlive() || this.tickCount >= LIFE) {
            this.discard();
            return;
        }
        for (int rear : REAR) {
            if (this.tickCount == rear) {
                this.aim(level);
                this.playSound(SoundEvents.SCULK_SHRIEKER_SHRIEK, 1.0f, 1.4f);
            } else if (this.tickCount == rear + REAR_TICKS + STRIKE_TICKS) {
                this.slam(level);
            }
        }
    }

    // Turns to the nearest enemy in reach (or stays as it is).
    private void aim(ServerLevel level) {
        if (this.owner == null) {
            return;
        }
        level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(REACH + 2.0, 3.0, REACH + 2.0), this.owner::isFoe).stream()
                .min(Comparator.comparingDouble(this::distanceToSqr))
                .ifPresent(target -> {
                    float yaw = (float) (Mth.atan2(-(target.getX() - this.getX()), target.getZ() - this.getZ()) * Mth.RAD_TO_DEG);
                    this.setYRot(yaw);
                    this.yRotO = yaw;
                });
    }

    // Lands: everything along it is hurt (and decays, as from every Sculk Monstrosity attack), and sculk mire is left there.
    private void slam(ServerLevel level) {
        if (this.owner == null) {
            return;
        }
        Vec3 dir = this.direction();
        Vec3 end = this.position().add(dir.scale(REACH));
        AABB area = new AABB(this.position(), end).inflate(HALF_WIDTH + 1.0, 2.0, HALF_WIDTH + 1.0);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area, this.owner::canHitWithArea)) {
            double dx = target.getX() - this.getX(), dz = target.getZ() - this.getZ();
            double along = dx * dir.x + dz * dir.z;
            double across = Math.abs(dx * dir.z - dz * dir.x);
            if (along >= -0.5 && along <= REACH + 0.5 && across <= HALF_WIDTH + target.getBbWidth() / 2.0 && target.hurt(this.owner.getServantAttack(), this.damage)) {
                double scale = Math.max(0.0, 1.0 - target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
                target.push(0.0, 0.4 * scale, 0.0);
                target.hurtMarked = true;
            }
        }
        SculkMire.line(level, this.owner, this.position(), this.getYRot(), REACH);
        BlockParticleOption sculk = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SCULK.defaultBlockState());
        for (int i = 1; i <= REACH; ++i) {
            Vec3 at = this.position().add(dir.scale(i));
            level.sendParticles(sculk, at.x, at.y + 0.2, at.z, 8, 0.5, 0.1, 0.5, 0.15);
            level.sendParticles(ParticleTypes.SCULK_CHARGE_POP, at.x, at.y + 0.3, at.z, 2, 0.4, 0.1, 0.4, 0.02);
        }
        this.playSound(SoundEvents.WARDEN_ATTACK_IMPACT, 2.0f, 0.6f);
        this.playSound(SoundEvents.SCULK_BLOCK_BREAK, 2.0f, 0.6f);
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public @NotNull AABB getBoundingBoxForCulling() {
        return this.getBoundingBox().inflate(REACH + 1.0, REACH, REACH + 1.0);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 128.0 * 128.0;
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag) {
    }

    // Its age goes with it to clients (which pose it by its age), also to those that only see it later.
    @Override
    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket() {
        return new ClientboundAddEntityPacket(this, this.tickCount);
    }

    @Override
    public void recreateFromPacket(@NotNull ClientboundAddEntityPacket packet) {
        super.recreateFromPacket(packet);
        this.tickCount = packet.getData();
    }
}
