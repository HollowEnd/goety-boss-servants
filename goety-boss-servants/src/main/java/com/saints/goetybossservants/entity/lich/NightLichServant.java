package com.saints.goetybossservants.entity.lich;

import com.Polarice3.Goety.common.entities.ai.ServantHurtByTargetGoal;
import com.Polarice3.Goety.common.entities.ai.SummonTargetGoal;
import com.Polarice3.Goety.common.entities.neutral.Owned;
import net.barribob.boss.config.LichConfig;
import net.barribob.boss.mob.ai.VelocitySteering;
import net.barribob.boss.mob.mobs.lich.LichActions;
import net.barribob.boss.mob.mobs.lich.LichEntity;
import net.barribob.boss.mob.mobs.lich.LichMovement;
import net.barribob.boss.mob.utils.EntityAdapter;
import com.saints.goetybossservants.config.ServantConfig;
import com.saints.goetybossservants.entity.bmd.Bmd;
import com.saints.goetybossservants.entity.servant.BossServant;
import com.saints.goetybossservants.entity.servant.ServantMinions;
import com.saints.goetybossservants.entity.servant.ServantData;
import com.saints.goetybossservants.registry.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BooleanSupplier;

/**
 * Bosses of Mass Destruction's Night Lich as a Goety servant. Its model, animations, particles,
 * comets, missiles and flight come from BMD's own class; this swaps its player-only boss AI for
 * Goety's targeting and attacks that work on any target ({@link NightLichServantAttacks}), with its
 * rage phase at 75/50/25% health and capped phantom minions ({@link ServantMinions}).
 *
 * <p>It flies: following its owner it steers toward them and teleports over if left far behind;
 * staying, it hovers in place; wandering or guarding, it drifts around like the boss does when idle.
 *
 * <p>Boss-only parts turned off: eternal night, the boss bar, its 1500-experience death shower, and
 * its habit of killing every phantom within 100 blocks when it dies (only its own minions go).
 */
public class NightLichServant extends LichEntity implements BossServant {
    private static final EntityDataAccessor<Byte> SERVANT_FLAGS = SynchedEntityData.defineId(NightLichServant.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Optional<UUID>> OWNER = SynchedEntityData.defineId(NightLichServant.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Integer> OWNER_CLIENT = SynchedEntityData.defineId(NightLichServant.class, EntityDataSerializers.INT);
    private static final int OUT_OF_COMBAT_TICKS = 100;
    private static final double FOLLOW_DISTANCE = 8.0;
    private static final double TELEPORT_DISTANCE = 32.0;

    /** The lich servant currently inside BMD's die(), whose phantom cull ModEvents limits to its own minions. */
    @Nullable
    public static NightLichServant dying;
    /** Where dying lich servants are, so ModEvents can stop BMD's experience shower there. */
    public static final List<XpBlock> noExperience = new ArrayList<>();

    public record XpBlock(ResourceKey<Level> dimension, Vec3 pos, long until) {
    }

    /** Whether this orb comes from a dying lich servant's experience shower (stopped by BmdCompat). */
    public static boolean blocksExperience(ExperienceOrb orb) {
        long now = orb.level().getGameTime();
        noExperience.removeIf(block -> block.until() < now);
        for (XpBlock block : noExperience) {
            if (block.dimension() == orb.level().dimension() && orb.position().distanceToSqr(block.pos()) < 64.0) {
                return true;
            }
        }
        return false;
    }

    private final ServantData servantData = new ServantData(this, SERVANT_FLAGS, OWNER, OWNER_CLIENT);
    @Nullable
    private NightLichServantAttacks attacks;
    private int lastHurtTick;

    public NightLichServant(EntityType<? extends NightLichServant> type, Level level) {
        super(type, level, Bmd.lichConfig());
        this.xpReward = 0;
        // BMD adds its boss AI in the constructor (targets players only); replace it.
        this.goalSelector.removeAllGoals(goal -> true);
        this.targetSelector.removeAllGoals(goal -> true);
        if (!level.isClientSide()) {
            LichConfig config = Bmd.lichConfig();
            this.attacks = new NightLichServantAttacks(this, config, this.getPreTickEvents());
            LichMovement movement = new LichMovement(this);
            this.goalSelector.addGoal(1, new FloatGoal(this));
            this.goalSelector.addGoal(2, this.attacks.buildAttackGoal());
            this.goalSelector.addGoal(3, new GatedGoal(movement.buildAttackMovement(), () -> this.getTarget() != null && !this.isStaying()));
            this.goalSelector.addGoal(4, new FlyToOwnerGoal());
            this.goalSelector.addGoal(5, new GatedGoal(movement.buildWanderGoal(), () -> this.getTarget() == null && (this.isWandering() || this.isGuardingArea())));
            this.targetSelector.addGoal(1, new Owned.OwnerHurtByTargetGoal(this));
            this.targetSelector.addGoal(1, new Owned.OwnerHurtTargetGoal(this));
            this.targetSelector.addGoal(2, new ServantHurtByTargetGoal(this));
            this.targetSelector.addGoal(3, new SummonTargetGoal(this));
        }
    }

    @Override
    public ServantData servantData() {
        return this.servantData;
    }

    /** Runs a BMD goal only while a condition holds (e.g. drifting about only when wandering). */
    private static final class GatedGoal extends Goal {
        private final Goal goal;
        private final BooleanSupplier condition;

        GatedGoal(Goal goal, BooleanSupplier condition) {
            this.goal = goal;
            this.condition = condition;
            this.setFlags(goal.getFlags());
        }

        @Override
        public boolean canUse() {
            return this.condition.getAsBoolean() && this.goal.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return this.condition.getAsBoolean() && this.goal.canContinueToUse();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            this.goal.start();
        }

        @Override
        public void stop() {
            this.goal.stop();
        }

        @Override
        public void tick() {
            this.goal.tick();
        }
    }

    /**
     * Follow mode for a flier: steer toward a point above the owner with BMD's own velocity steering,
     * and teleport next to them (with its teleport effect) if they get far away.
     */
    private final class FlyToOwnerGoal extends Goal {
        private final VelocitySteering steering;

        FlyToOwnerGoal() {
            NightLichServant lich = NightLichServant.this;
            this.steering = new VelocitySteering(new EntityAdapter(lich), lich.getAttributeValue(Attributes.FLYING_SPEED), 120.0);
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Nullable
        private LivingEntity owner() {
            NightLichServant lich = NightLichServant.this;
            return lich.getTarget() == null && lich.isFollowing() ? lich.getTrueOwner() : null;
        }

        @Override
        public boolean canUse() {
            LivingEntity owner = this.owner();
            return owner != null && owner.level() == NightLichServant.this.level()
                    && NightLichServant.this.distanceToSqr(owner) > FOLLOW_DISTANCE * FOLLOW_DISTANCE;
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity owner = this.owner();
            return owner != null && owner.level() == NightLichServant.this.level()
                    && NightLichServant.this.distanceToSqr(owner) > (FOLLOW_DISTANCE * 0.5) * (FOLLOW_DISTANCE * 0.5);
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            NightLichServant lich = NightLichServant.this;
            LivingEntity owner = this.owner();
            if (owner == null) {
                return;
            }
            Vec3 above = owner.position().add(0.0, 3.0, 0.0);
            if (lich.distanceToSqr(owner) > TELEPORT_DISTANCE * TELEPORT_DISTANCE) {
                lich.teleportTo(above.x, above.y, above.z);
                lich.level().broadcastEntityEvent(lich, LichActions.endTeleport);
                lich.playSound(Bmd.sounds().getLichTeleport(), 2.0f, 1.0f);
                return;
            }
            lich.addDeltaMovement(this.steering.accelerateTo(above));
            lich.getLookControl().setLookAt(owner);
        }
    }

    // ----- Synced data, saving, spawning -------------------------------------------------------

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        ServantData.define(this.entityData, SERVANT_FLAGS, OWNER, OWNER_CLIENT);
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        BossServant.save(this, tag);
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        BossServant.read(this, tag);
    }

    @Override
    public SpawnGroupData finalizeSpawn(@NotNull ServerLevelAccessor level, @NotNull DifficultyInstance difficulty, @NotNull MobSpawnType reason,
                                        @Nullable SpawnGroupData data, @Nullable CompoundTag tag) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, data, tag);
        this.setWandering(false);
        this.setStaying(false);
        return result;
    }

    /** No boss bar for a servant. */
    @Override
    protected ServerBossEvent getBossBar() {
        return null;
    }

    @Override
    public void serverTick(ServerLevel level) {
        // BMD forces eternal night here every tick; a servant leaves the time alone.
    }

    @Override
    public void tick() {
        super.tick();
        this.ownedTick();
        this.servantTick();
        if (!this.level().isClientSide) {
            if (this.isGuardingArea()) {
                this.setIdlePosition(this.vec3BoundPos().add(0.0, 4.0, 0.0));
            }
            this.healOutOfCombat();
        }
    }

    /**
     * BMD's idle heal (CappedHeal, still running) stops at the 75/50/25% milestone above it. As a
     * servant it heals to full at BMD's idle rate once it has had no target and no damage for 5 s.
     */
    private void healOutOfCombat() {
        if (this.isAlive() && this.getTarget() == null && this.getHealth() < this.getMaxHealth()
                && this.tickCount - this.lastHurtTick > OUT_OF_COMBAT_TICKS) {
            this.heal(Bmd.lichConfig().getIdleHealingPerTick());
        }
    }

    // ----- Targeting and allies ----------------------------------------------------------------

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        BossServant.handleSetTarget(this, target);
    }

    @Override
    public void overrideSetTarget(@Nullable LivingEntity target) {
        super.setTarget(target);
    }

    @Override
    public boolean isAlliedTo(@NotNull Entity entity) {
        return super.isAlliedTo(entity) || BossServant.isAlliedTo(this, entity) || ServantMinions.isMinionOf(entity, this);
    }

    @Override
    public int getSummonLimit(LivingEntity owner) {
        return ServantConfig.NightLichSummonLimit.get();
    }

    // ----- Boss-only behaviour turned off ------------------------------------------------------

    @Override
    public void checkDespawn() {
        // BMD despawns it on Peaceful; a servant stays.
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean canBeLeashed(@NotNull Player player) {
        return false;
    }

    // ----- Damage and death --------------------------------------------------------------------

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        if (BossServant.blocksHurt(this, source)) {
            return false;
        }
        float before = this.getHealth() / this.getMaxHealth();
        boolean hurt = super.hurt(source, amount);
        if (hurt) {
            BossServant.afterHurt(this);
            this.lastHurtTick = this.tickCount;
            float after = this.getHealth() / this.getMaxHealth();
            for (float milestone : new float[]{0.75f, 0.5f, 0.25f}) {
                if (before > milestone && after <= milestone && after > 0.0f && this.attacks != null) {
                    this.attacks.queueRage();
                }
            }
        }
        return hurt;
    }

    @Override
    public void setHealth(float health) {
        BossServant.logHealing(this, health);
        super.setHealth(health);
    }

    @Override
    public void die(@NotNull DamageSource source) {
        if (this.level().isClientSide) {
            super.die(source);
            return;
        }
        // BMD's die() showers experience and kills every phantom within 100 blocks; ModEvents keeps
        // both to this lich (no experience, only its own minions).
        noExperience.add(new XpBlock(this.level().dimension(), this.getEyePosition(), this.level().getGameTime() + 60));
        dying = this;
        try {
            super.die(source);
        } finally {
            dying = null;
        }
        if (this.getOwnerId() != null) {
            this.spawnAtLocation(ModItems.NIGHT_LICH_SOUL);
        }
    }
}
