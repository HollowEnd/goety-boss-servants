package com.saints.goetybossservants.entity.soulslike;

import com.Polarice3.Goety.common.entities.ai.ServantHurtByTargetGoal;
import com.Polarice3.Goety.common.entities.ai.SummonTargetGoal;
import com.Polarice3.Goety.common.entities.ally.Summoned;
import com.Polarice3.Goety.common.entities.neutral.Owned;
import com.saints.goetybossservants.config.ServantConfig;
import com.saints.goetybossservants.entity.servant.BossServant;
import com.saints.goetybossservants.entity.servant.ServantData;
import com.saints.goetybossservants.registry.ModEntities;
import com.saints.goetybossservants.registry.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.soulsweaponry.entity.ai.goal.DraugrBossGoal;
import net.soulsweaponry.entity.mobs.DraugrBoss;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

/**
 * Soulslike Weaponry's Old Champion's Remains (draugr boss) as a Goety servant. Its model,
 * animations, shield stance and whole attack set (DraugrBossGoal: counters, parries, shield bash and
 * vault, swipes, heavy blow, ground slam, battle cry, leap, running thrust) come from the boss; the
 * goal already fights whatever it targets, so only targeting is swapped for Goety's. Its second phase
 * is the boss's too: at half health a posture break, then Speed and Strength II for the rest of the
 * fight. The boss's growing resistance to repeated hits from the same weapon is removed
 * (OldChampionMixin): mobs always "hold" the same thing, so it ended up immune to them.
 *
 * <p>Boss-only parts turned off: the boss bar and music, experience, the Night Shade boss that rises
 * when it dies (ModEvents), and the Slowness/Weakness it gets under open sky (OldChampionMixin).
 * Its ground slam and battle cry pass over friendlies (SoulslikeGoalMixin).
 */
public class OldChampionServant extends DraugrBoss implements BossServant {
    private static final EntityDataAccessor<Byte> SERVANT_FLAGS = SynchedEntityData.defineId(OldChampionServant.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Optional<UUID>> OWNER = SynchedEntityData.defineId(OldChampionServant.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Integer> OWNER_CLIENT = SynchedEntityData.defineId(OldChampionServant.class, EntityDataSerializers.INT);

    /** The servant currently inside the boss's die(), whose Night Shade ModEvents stops from spawning. */
    @Nullable
    public static OldChampionServant dying;

    private final ServantData servantData = new ServantData(this, SERVANT_FLAGS, OWNER, OWNER_CLIENT);

    public OldChampionServant(EntityType<? extends OldChampionServant> type, Level level) {
        super(type, level);
        this.xpReward = 0;
        this.bossBar.setVisible(false);
        // 3 blocks tall: steps up a full block like the boss model suggests, instead of hopping.
        this.setMaxUpStep(1.25f);
    }

    @Override
    public ServantData servantData() {
        return this.servantData;
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    protected void registerGoals() {
        // Not calling super: that adds the boss's player targeting.
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new DraugrBossGoal(this));
        this.goalSelector.addGoal(5, new Summoned.FollowOwnerGoal(this, 1.0, 10.0f, 2.0f));
        this.goalSelector.addGoal(8, new Summoned.WanderGoal(this, 1.0));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new Owned.OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(1, new Owned.OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(2, new ServantHurtByTargetGoal(this));
        this.targetSelector.addGoal(3, new SummonTargetGoal(this));
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
        // No rising-from-the-ground cinematic: it arrives ready, shield up.
        this.setState(States.IDLE);
        this.setShielding(true);
        return result;
    }

    @Override
    public void tick() {
        super.tick();
        this.ownedTick();
        this.servantTick();
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
        return ServantConfig.OldChampionSummonLimit.get();
    }

    // ----- Boss-only behaviour turned off ------------------------------------------------------

    @Override
    public int getXp() {
        return 0;
    }

    @Override
    public boolean hasBossMusic() {
        return false;
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return false;
    }

    @Override
    public boolean isPreventingPlayerRest(@NotNull Player player) {
        return this.isHostile();
    }

    @Override
    public boolean canChangeDimensions() {
        return true;
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
        boolean hurt = super.hurt(source, amount);
        if (hurt) {
            BossServant.afterHurt(this);
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
        dying = this;
        try {
            super.die(source);
        } finally {
            dying = null;
        }
        if (this.getOwnerId() != null) {
            this.spawnAtLocation(ModItems.OLD_CHAMPION_SOUL);
        }
    }
}
