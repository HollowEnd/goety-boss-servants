package com.saints.goetybossservants.entity.monstrosity;

import com.saints.goetybossservants.registry.ModRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

/**
 * A glowing crack in the ground left by the Netherite Monstrosity's special: a molten fissure
 * splitting outward from where it slammed (growing as the wave runs), or the crater it leaves
 * where it lands. Only something to see: it hurts nothing, cools and fades, and is never saved.
 */
public class NetheriteRift extends Entity {
    private static final EntityDataAccessor<Float> SIZE = SynchedEntityData.defineId(NetheriteRift.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> GROW_TICKS = SynchedEntityData.defineId(NetheriteRift.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> CRATER = SynchedEntityData.defineId(NetheriteRift.class, EntityDataSerializers.BOOLEAN);
    /** How long a rift lasts, cooling and fading over the last FADE_TICKS. */
    public static final int LIFE = 120;
    public static final int FADE_TICKS = 50;

    public NetheriteRift(EntityType<? extends NetheriteRift> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    /** A fissure from {@code from} along {@code angle} (radians, round the y axis), reaching {@code length} over {@code growTicks}. */
    static void fissure(ServerLevel level, Vec3 from, double angle, double length, int growTicks) {
        spawn(level, from, (float) Math.toDegrees(angle), (float) length, growTicks, false);
    }

    /** A crater of radial cracks round {@code at}. */
    static void crater(ServerLevel level, Vec3 at, double radius) {
        spawn(level, at, 0.0f, (float) radius, 4, true);
    }

    private static void spawn(ServerLevel level, Vec3 at, float yaw, float size, int growTicks, boolean crater) {
        NetheriteRift rift = ModRegistry.NETHERITE_RIFT.create(level);
        if (rift != null) {
            rift.moveTo(at.x, at.y, at.z, yaw, 0.0f);
            rift.entityData.set(SIZE, size);
            rift.entityData.set(GROW_TICKS, growTicks);
            rift.entityData.set(CRATER, crater);
            level.addFreshEntity(rift);
        }
    }

    /** A fissure's full length, or a crater's radius, in blocks. */
    public float size() {
        return this.entityData.get(SIZE);
    }

    public int growTicks() {
        return this.entityData.get(GROW_TICKS);
    }

    public boolean isCrater() {
        return this.entityData.get(CRATER);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(SIZE, 1.0f);
        this.entityData.define(GROW_TICKS, 1);
        this.entityData.define(CRATER, false);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide && this.tickCount >= LIFE) {
            this.discard();
        }
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

    // Drawn whenever any of it is in view, not just its middle.
    @Override
    public @NotNull AABB getBoundingBoxForCulling() {
        return this.getBoundingBox().inflate(this.size(), 2.0, this.size());
    }

    // Seen from as far as the fissures reach.
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

    // Its age goes with it to clients (which draw it by its age), also to those that only see it later.
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
