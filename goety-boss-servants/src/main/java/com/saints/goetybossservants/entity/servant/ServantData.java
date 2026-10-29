package com.saints.goetybossservants.entity.servant;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

/**
 * The state Goety keeps on its Owned/Summoned base classes, for a boss servant that has to extend the
 * boss's own class instead. {@link BossServant} reads and writes everything through this.
 *
 * <p>The synced values need accessors defined against the entity's own class, so the entity defines
 * them and hands them in; see {@link #define}.
 */
public class ServantData {
    public static final int FLAG_WANDERING = 1;
    public static final int FLAG_STAYING = 2;
    public static final int FLAG_UPGRADED = 4;
    public static final int FLAG_HOSTILE = 8;
    public static final int FLAG_NATURAL = 16;

    final Mob mob;
    private final EntityDataAccessor<Byte> flags;
    private final EntityDataAccessor<Optional<UUID>> owner;
    private final EntityDataAccessor<Integer> ownerClient;

    @Nullable
    LivingEntity priorityTarget;
    @Nullable
    LivingEntity commandPosEntity;
    @Nullable
    BlockPos commandPos;
    @Nullable
    BlockPos priorityPos;
    @Nullable
    BlockPos boundPos;
    String boundDim = Level.OVERWORLD.location().toString();
    int priorityTime;
    int commandTick;
    int killChance;
    int noHealTime;
    long ticketTime;
    boolean limitedLifespan;
    int limitedLifeTicks;
    int hasSummonCheck;
    int revivingTime;
    int lastHealLogTick;
    /** Owner tick of the last owner attack / owner injury the servant has acted on (see followOwnersFight). */
    int ownerAttackSeen = -1;
    int ownerHurtSeen = -1;
    int lastTargetStatusTick;
    int lastTargetRequestTick;
    String lastTargetRequest = "";

    public ServantData(Mob mob, EntityDataAccessor<Byte> flags, EntityDataAccessor<Optional<UUID>> owner, EntityDataAccessor<Integer> ownerClient) {
        this.mob = mob;
        this.flags = flags;
        this.owner = owner;
        this.ownerClient = ownerClient;
    }

    /** Call from the entity's defineSynchedData(). */
    public static void define(SynchedEntityData data, EntityDataAccessor<Byte> flags, EntityDataAccessor<Optional<UUID>> owner, EntityDataAccessor<Integer> ownerClient) {
        data.define(flags, (byte) 0);
        data.define(owner, Optional.empty());
        data.define(ownerClient, -1);
    }

    public boolean getFlag(int mask) {
        return (this.mob.getEntityData().get(this.flags) & mask) != 0;
    }

    public void setFlag(int mask, boolean value) {
        int current = this.mob.getEntityData().get(this.flags);
        this.mob.getEntityData().set(this.flags, (byte) (value ? current | mask : current & ~mask));
    }

    @Nullable
    public UUID getOwnerId() {
        return this.mob.getEntityData().get(this.owner).orElse(null);
    }

    public void setOwnerId(@Nullable UUID uuid) {
        this.mob.getEntityData().set(this.owner, Optional.ofNullable(uuid));
    }

    public int getOwnerClientId() {
        return this.mob.getEntityData().get(this.ownerClient);
    }

    public void setOwnerClientId(int id) {
        this.mob.getEntityData().set(this.ownerClient, id);
    }
}
