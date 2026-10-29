package com.saints.goetybossservants.entity.voidblossom;

import com.Polarice3.Goety.common.entities.ai.ServantHurtByTargetGoal;
import com.Polarice3.Goety.common.entities.ai.SummonTargetGoal;
import com.Polarice3.Goety.common.entities.neutral.Owned;
import com.cerbon.bosses_of_mass_destruction.entity.BMDEntities;
import com.cerbon.bosses_of_mass_destruction.entity.ai.goals.ActionGoal;
import com.cerbon.bosses_of_mass_destruction.entity.ai.goals.CompositeGoal;
import com.cerbon.bosses_of_mass_destruction.entity.custom.void_blossom.LightBlockRemover;
import com.cerbon.bosses_of_mass_destruction.entity.custom.void_blossom.VoidBlossomEntity;
import com.cerbon.bosses_of_mass_destruction.entity.custom.void_blossom.hitbox.NetworkedHitboxManager;
import com.cerbon.bosses_of_mass_destruction.entity.util.CompositeEntityTick;
import com.cerbon.cerbons_api.api.static_utilities.MobUtils;
import com.saints.goetybossservants.config.ServantConfig;
import com.saints.goetybossservants.entity.servant.BossServant;
import com.saints.goetybossservants.entity.servant.ServantData;
import com.saints.goetybossservants.registry.ModRegistry;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

/**
 * Bosses of Mass Destruction's Void Blossom as a Goety servant: a rooted turret, like the boss (it
 * only ever moves up and down). Its model, animations, spikes, projectiles, hitboxes, idle heal and
 * light all come from BMD's own class. This swaps its player-only boss AI for Goety's targeting and
 * attacks that work on any target ({@link VoidBlossomServantAttacks}), makes its Blossom move
 * temporary and close by ({@link VoidBlossomServantBlossoms}), and turns off the boss bar and the
 * experience drop. Friendly fire from its spikes, thorns and projectiles is stopped in ModEvents.
 *
 * <p>It always stays where it was summoned: stay and guard work; follow and wander can't move it.
 */
public class VoidBlossomServant extends VoidBlossomEntity implements BossServant {
    // BMD registers this synced value (on BaseEntity) only when its hitbox class first loads, during
    // the boss constructor. Load it before ours get their ids, or both claim the same id and the
    // constructor crashes with "Duplicate id value".
    @SuppressWarnings("unused")
    private static final EntityDataAccessor<Byte> BMD_HITBOX = NetworkedHitboxManager.hitbox;
    private static final EntityDataAccessor<Byte> SERVANT_FLAGS = SynchedEntityData.defineId(VoidBlossomServant.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Optional<UUID>> OWNER = SynchedEntityData.defineId(VoidBlossomServant.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Integer> OWNER_CLIENT = SynchedEntityData.defineId(VoidBlossomServant.class, EntityDataSerializers.INT);

    private final ServantData servantData = new ServantData(this, SERVANT_FLAGS, OWNER, OWNER_CLIENT);
    private final VoidBlossomServantBlossoms blossoms = new VoidBlossomServantBlossoms(this);
    /** Set when damage takes it past 75/50/25% health; its next attack is the Blossom move (as on the boss). */
    private boolean blossomPending;
    /** When it last took damage, for its out-of-combat heal. */
    private int lastHurtTick;
    private static final int OUT_OF_COMBAT_TICKS = 100;
    /** How far below itself removeLight looks for light blocks left while it rose out of the ground. */
    private static final int LIGHT_CLEAR_DEPTH = 4;

    @SuppressWarnings({"unchecked", "rawtypes"})
    public VoidBlossomServant(EntityType<? extends VoidBlossomServant> type, Level level) {
        super(type, level, BMDEntities.mobConfig.voidBlossomConfig);
        this.xpReward = 0;
        this.bossBar = null;
        // Keep BMD's death animation and light fade, without its experience shower.
        this.deathServerTick = new CompositeEntityTick(new LightBlockRemover(this));
        // BMD adds its boss AI in the constructor (targets players only); replace it.
        this.goalSelector.removeAllGoals(goal -> true);
        this.targetSelector.removeAllGoals(goal -> true);
        if (!level.isClientSide() && level instanceof ServerLevel) {
            VoidBlossomServantAttacks attacks = new VoidBlossomServantAttacks(this, this.preTickEvents);
            this.goalSelector.addGoal(1, new CompositeGoal(attacks.buildAttackGoal(),
                    new ActionGoal(() -> this.isAlive() && this.getTarget() != null, null, this::lookAtTarget, null, null)));
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

    VoidBlossomServantBlossoms blossoms() {
        return this.blossoms;
    }

    /** Where its healing flowers currently are (for the dev self-test). */
    public java.util.List<net.minecraft.core.BlockPos> flowerPositions() {
        return this.blossoms.flowerPositions();
    }

    /** Whether its next attack is a Blossom move (for the dev self-test). */
    public boolean isBlossomPending() {
        return this.blossomPending;
    }

    /** Its flowers are out or still being planted (for the dev self-test). */
    public boolean hasBlossomsOut() {
        return this.blossoms.isActive(this.level());
    }

    boolean takeBlossomPending() {
        boolean pending = this.blossomPending;
        this.blossomPending = false;
        return pending;
    }

    private void lookAtTarget() {
        LivingEntity target = this.getTarget();
        if (target != null) {
            this.lookControl.setLookAt(MobUtils.eyePos(target));
            this.lookAt(target, this.getMaxHeadYRot(), this.getMaxHeadXRot());
        }
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        ServantData.define(this.entityData, SERVANT_FLAGS, OWNER, OWNER_CLIENT);
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        BossServant.save(this, tag);
        tag.put("ServantBlossoms", this.blossoms.save());
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        BossServant.read(this, tag);
        this.blossoms.load(this.level(), tag.getList("ServantBlossoms", Tag.TAG_COMPOUND));
    }

    @Override
    public SpawnGroupData finalizeSpawn(@NotNull ServerLevelAccessor level, @NotNull DifficultyInstance difficulty, @NotNull MobSpawnType reason,
                                        @Nullable SpawnGroupData data, @Nullable CompoundTag tag) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, data, tag);
        // Rooted: it stays where it's summoned, like the Fabric servant.
        this.setStaying(true);
        return result;
    }

    @Override
    public void tick() {
        super.tick();
        this.ownedTick();
        this.servantTick();
        if (this.level() instanceof ServerLevel serverLevel) {
            this.blossoms.tick(serverLevel);
            if (this.isOnFire()) {
                // A burning blossom loses all its healing flowers at once.
                this.blossoms.removeAll(serverLevel);
            }
            this.healOutOfCombat();
        }
    }

    /**
     * BMD's own idle heal (CappedHeal, still running) only heals up to the health milestone above it
     * (75/50/25%), so after a real fight it barely heals. As a servant it heals to full instead, at BMD's
     * idle rate, once it has had no target and taken no damage for 5 seconds.
     */
    private void healOutOfCombat() {
        if (this.isAlive() && this.getTarget() == null && this.getHealth() < this.getMaxHealth()
                && this.tickCount - this.lastHurtTick > OUT_OF_COMBAT_TICKS) {
            this.heal(BMDEntities.mobConfig.voidBlossomConfig.idleHealingPerTick);
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
        return super.isAlliedTo(entity) || BossServant.isAlliedTo(this, entity);
    }

    @Override
    public int getSummonLimit(LivingEntity owner) {
        return ServantConfig.VoidBlossomSummonLimit.get();
    }

    /**
     * BMD's Void Blossom is fire-immune and always reports not burning. The servant can catch fire
     * (its entity type isn't fire-immune), so this is vanilla's Entity.isOnFire again.
     */
    @Override
    public boolean isOnFire() {
        return !this.fireImmune() && (this.getRemainingFireTicks() > 0 || this.level().isClientSide && this.getSharedFlag(0));
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

    // ----- Damage, death and removal -----------------------------------------------------------

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        if (BossServant.blocksHurt(this, source)) {
            return false;
        }
        float before = this.getHealth() / this.getMaxHealth();
        boolean hurt = super.hurt(source, amount);
        if (hurt) {
            this.lastHurtTick = this.tickCount;
            BossServant.afterHurt(this);
            float after = this.getHealth() / this.getMaxHealth();
            for (float milestone : new float[]{0.75f, 0.5f, 0.25f}) {
                if (before > milestone && after <= milestone && after > 0.0f) {
                    // Not while its last flowers are still out or being planted (a quick fall through
                    // several milestones would otherwise plant set after set).
                    this.blossomPending |= !this.blossoms.isActive(this.level());
                }
            }
        }
        return hurt;
    }

    @Override
    public void die(@NotNull DamageSource source) {
        super.die(source);
        if (!this.level().isClientSide && this.getOwnerId() != null) {
            this.spawnAtLocation(ModRegistry.VOID_BLOSSOM_SOUL.get());
        }
    }

    @Override
    public void remove(@NotNull RemovalReason reason) {
        // Killed, dismissed or discarded: take its flowers with it. (Not on unload; they're saved.)
        if (!this.level().isClientSide && reason.shouldDestroy()) {
            this.blossoms.removeAll(this.level());
            this.removeLight();
        }
        super.remove(reason);
    }

    /**
     * BMD keeps an invisible light block where the blossom stands (LightBlockPlacer) and only fades
     * it out in its death animation. A servant can also leave without dying (dismissed, stored in a
     * Tesseract), so clear it here, plus any it left lower down while rising out of the ground.
     */
    private void removeLight() {
        BlockPos pos = this.blockPosition();
        for (int dy = -LIGHT_CLEAR_DEPTH; dy <= 1; ++dy) {
            BlockPos at = pos.above(dy);
            if (this.level().getBlockState(at).is(Blocks.LIGHT)) {
                this.level().setBlockAndUpdate(at, Blocks.AIR.defaultBlockState());
            }
        }
    }
}
