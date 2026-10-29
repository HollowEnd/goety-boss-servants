package com.saints.goetybossservants.entity.monstrosity;

import com.saints.goetybossservants.registry.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * The Diamond Monstrosity's spikes: either bursting out of the ground (hurting and launching what
 * stands on them) or falling from the sky (shattering on whatever they hit). They only hurt what
 * their monstrosity's area attacks may hurt, and are never saved.
 */
public class DiamondSpike extends Entity {
    private static final EntityDataAccessor<Boolean> FALLING = SynchedEntityData.defineId(DiamondSpike.class, EntityDataSerializers.BOOLEAN);
    /** A ground spike rises over this many ticks, then hurts what it hit. */
    public static final int RISE_TICKS = 4;
    /** Ground spikes stay up this long, sinking back over the last few ticks. */
    public static final int GROUND_LIFE = 30;
    public static final int SINK_TICKS = 6;
    private static final int FALL_LIFE = 100;

    @Nullable
    private MaterialMonstrosity owner;
    private float damage;
    private final Set<UUID> hit = new HashSet<>();

    public DiamondSpike(EntityType<? extends DiamondSpike> type, Level level) {
        super(type, level);
    }

    public static void spawnGround(ServerLevel level, MaterialMonstrosity owner, double x, double z, double y, float damage) {
        BlockPos pos = groundAt(level, BlockPos.containing(x, y, z));
        if (pos == null) {
            return;
        }
        DiamondSpike spike = ModRegistry.DIAMOND_SPIKE.get().create(level);
        if (spike != null) {
            spike.owner = owner;
            spike.damage = damage;
            spike.moveTo(x, pos.getY(), z, owner.getRandom().nextFloat() * 360.0f, 0.0f);
            level.addFreshEntity(spike);
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, level.getBlockState(pos.below())), x, pos.getY() + 0.1, z, 12, 0.3, 0.1, 0.3, 0.1);
        }
    }

    public static void spawnFalling(ServerLevel level, MaterialMonstrosity owner, double x, double y, double z, float damage) {
        DiamondSpike spike = ModRegistry.DIAMOND_SPIKE.get().create(level);
        if (spike != null) {
            spike.owner = owner;
            spike.damage = damage;
            spike.setFalling(true);
            spike.moveTo(x, y, z, owner.getRandom().nextFloat() * 360.0f, 0.0f);
            spike.setDeltaMovement(0.0, -0.6, 0.0);
            level.addFreshEntity(spike);
        }
    }

    /** The air block standing on solid ground closest below (or a little above) {@code start}, or null. */
    @Nullable
    private static BlockPos groundAt(Level level, BlockPos start) {
        BlockPos pos = start.above(2);
        for (int i = 0; i < 10; ++i) {
            if (level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP) && level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) {
                return pos;
            }
            pos = pos.below();
        }
        return null;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(FALLING, false);
    }

    public boolean isFalling() {
        return this.entityData.get(FALLING);
    }

    public void setFalling(boolean falling) {
        this.entityData.set(FALLING, falling);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.isFalling()) {
            this.setDeltaMovement(0.0, Math.max(this.getDeltaMovement().y - 0.08, -1.6), 0.0);
            this.move(MoverType.SELF, this.getDeltaMovement());
        }
        if (this.level().isClientSide) {
            return;
        }
        if (this.owner == null || !this.owner.isAlive()) {
            this.discard();
            return;
        }
        if (this.isFalling()) {
            boolean struck = !this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox(), this.owner::canHitWithArea).isEmpty();
            if (struck || this.onGround() || this.verticalCollision || this.tickCount > FALL_LIFE) {
                this.shatter();
            }
        } else {
            if (this.tickCount == RISE_TICKS) {
                for (LivingEntity target : this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(0.4, 0.0, 0.4), this.owner::canHitWithArea)) {
                    if (this.hit.add(target.getUUID()) && target.hurt(this.owner.getServantAttack(), this.damage)) {
                        MonstrositySpecials.launch(target, this.position(), 0.2, 1.1);
                    }
                }
                this.playSound(SoundEvents.AMETHYST_CLUSTER_BREAK, 1.5f, 0.6f + this.random.nextFloat() * 0.3f);
            }
            if (this.tickCount >= GROUND_LIFE) {
                this.discard();
            }
        }
    }

    private void shatter() {
        for (LivingEntity target : this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(1.0), this.owner::canHitWithArea)) {
            target.hurt(this.owner.getServantAttack(), this.damage);
        }
        if (this.level() instanceof ServerLevel level) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DIAMOND_BLOCK.defaultBlockState()), this.getX(), this.getY() + 0.3, this.getZ(), 20, 0.3, 0.3, 0.3, 0.15);
        }
        this.playSound(SoundEvents.AMETHYST_BLOCK_BREAK, 1.5f, 1.2f + this.random.nextFloat() * 0.3f);
        this.discard();
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
