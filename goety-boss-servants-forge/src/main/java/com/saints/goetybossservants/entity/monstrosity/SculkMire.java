package com.saints.goetybossservants.entity.monstrosity;

import com.Polarice3.Goety.utils.MathHelper;
import com.saints.goetybossservants.config.ServantConfig;
import com.saints.goetybossservants.registry.ModRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

/**
 * Sculk left on the ground by the Sculk Monstrosity's attacks: a line where its tentacles slam, or a
 * pool where its sculk globs burst. For a few seconds it adds a stack of Sculk Decay every second to
 * the monstrosity's foes standing in it (see MaterialMonstrosity.isFoe), then withers away.
 */
public class SculkMire extends Entity {
    private static final EntityDataAccessor<Float> SIZE = SynchedEntityData.defineId(SculkMire.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> POOL = SynchedEntityData.defineId(SculkMire.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> LIFE = SynchedEntityData.defineId(SculkMire.class, EntityDataSerializers.INT);
    /** How long it takes to spread out, and to wither away at the end. */
    public static final int GROW_TICKS = 6;
    public static final int FADE_TICKS = 20;
    /** Half the width of a line. */
    public static final double HALF_WIDTH = 1.0;

    @Nullable
    private MaterialMonstrosity owner;

    public SculkMire(EntityType<? extends SculkMire> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    /** A line from {@code from} along {@code yaw} (degrees, as an entity faces), {@code length} blocks long. */
    public static void line(ServerLevel level, MaterialMonstrosity owner, Vec3 from, float yaw, double length) {
        spawn(level, owner, from, yaw, (float) length, false);
    }

    /** A pool round {@code at}. */
    public static void pool(ServerLevel level, MaterialMonstrosity owner, Vec3 at, double radius) {
        spawn(level, owner, at, 0.0f, (float) radius, true);
    }

    private static void spawn(ServerLevel level, MaterialMonstrosity owner, Vec3 at, float yaw, float size, boolean pool) {
        SculkMire mire = ModRegistry.SCULK_MIRE.get().create(level);
        if (mire != null) {
            mire.owner = owner;
            mire.moveTo(at.x, at.y, at.z, yaw, 0.0f);
            mire.entityData.set(SIZE, size);
            mire.entityData.set(POOL, pool);
            mire.entityData.set(LIFE, MathHelper.secondsToTicks(ServantConfig.SculkMireSeconds.get()));
            level.addFreshEntity(mire);
        }
    }

    /** A line's length, or a pool's radius, in blocks. */
    public float size() {
        return this.entityData.get(SIZE);
    }

    public boolean isPool() {
        return this.entityData.get(POOL);
    }

    /** How long it lasts, in ticks. */
    public int life() {
        return this.entityData.get(LIFE);
    }

    /** The way a line runs (along the ground). */
    public Vec3 direction() {
        float yaw = this.getYRot() * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.sin(yaw), 0.0, Mth.cos(yaw));
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(SIZE, 1.0f);
        this.entityData.define(POOL, false);
        this.entityData.define(LIFE, 100);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        if (this.owner == null || !this.owner.isAlive() || this.tickCount >= this.life()) {
            this.discard();
            return;
        }
        if (this.tickCount % 20 == 10) {
            for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, this.area(), this.owner::isFoe)) {
                if (this.covers(target.position())) {
                    SculkDecay.apply(target, this.owner, 1);
                }
            }
        }
        if (this.tickCount % 4 == 0) {
            Vec3 spot = this.randomSpot();
            level.sendParticles(this.random.nextInt(4) == 0 ? ParticleTypes.SCULK_SOUL : ParticleTypes.SCULK_CHARGE_POP,
                    spot.x, spot.y + 0.1, spot.z, 1, 0.1, 0.05, 0.1, 0.01);
        }
    }

    private AABB area() {
        if (this.isPool()) {
            return new AABB(this.position(), this.position()).inflate(this.size(), 2.0, this.size());
        }
        Vec3 end = this.position().add(this.direction().scale(this.size()));
        return new AABB(this.position(), end).inflate(HALF_WIDTH + 0.5, 2.0, HALF_WIDTH + 0.5);
    }

    /** Whether something standing at {@code pos} is in it. */
    public boolean covers(Vec3 pos) {
        if (Math.abs(pos.y - this.getY()) > 2.5) {
            return false;
        }
        double dx = pos.x - this.getX(), dz = pos.z - this.getZ();
        if (this.isPool()) {
            return dx * dx + dz * dz <= this.size() * this.size();
        }
        Vec3 dir = this.direction();
        double along = dx * dir.x + dz * dir.z;
        double across = Math.abs(dx * dir.z - dz * dir.x);
        return along >= -0.5 && along <= this.size() + 0.5 && across <= HALF_WIDTH + 0.4;
    }

    private Vec3 randomSpot() {
        if (this.isPool()) {
            double angle = this.random.nextDouble() * Math.PI * 2.0, r = Math.sqrt(this.random.nextDouble()) * this.size();
            return this.position().add(Math.cos(angle) * r, 0.0, Math.sin(angle) * r);
        }
        Vec3 dir = this.direction();
        double along = this.random.nextDouble() * this.size(), across = (this.random.nextDouble() - 0.5) * HALF_WIDTH * 2.0;
        return this.position().add(dir.scale(along)).add(dir.z * across, 0.0, -dir.x * across);
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
        return this.getBoundingBox().inflate(this.size() + 1.0, 2.0, this.size() + 1.0);
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
