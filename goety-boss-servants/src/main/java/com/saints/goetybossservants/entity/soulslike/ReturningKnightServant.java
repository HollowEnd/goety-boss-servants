package com.saints.goetybossservants.entity.soulslike;

import com.Polarice3.Goety.common.entities.ai.ServantHurtByTargetGoal;
import com.Polarice3.Goety.common.entities.ai.SummonTargetGoal;
import com.Polarice3.Goety.common.entities.ally.Summoned;
import com.Polarice3.Goety.common.entities.neutral.Owned;
import com.saints.goetybossservants.config.ServantConfig;
import com.saints.goetybossservants.entity.servant.BossServant;
import com.saints.goetybossservants.entity.servant.ServantData;
import com.saints.goetybossservants.entity.servant.ServantMinions;
import com.saints.goetybossservants.mixin.MobAccessor;
import com.saints.goetybossservants.registry.ModEntities;
import com.saints.goetybossservants.registry.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.soulsweaponry.entity.ai.goal.ReturningKnightGoal;
import net.soulsweaponry.entity.mobs.DarkSorcerer;
import net.soulsweaponry.entity.mobs.Remnant;
import net.soulsweaponry.entity.mobs.ReturningKnight;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

/**
 * Soulslike Weaponry's Returning Knight as a Goety servant. Its model, animations and whole attack
 * set (ReturningKnightGoal: Obliterate, Blinding Reflection, Rupture, Mace of Spades, summoning)
 * come from the boss; the goal already fights whatever it targets, so only targeting is swapped for
 * Goety's. Its second phase is the boss's too: at half health the one-time Unbreakable stance, then
 * Speed and Resistance for the rest of the fight, and its summons bring only Remnants.
 *
 * <p>Its Remnants and Dark Sorcerers are its minions (see {@link #summoning} and ServantMinions):
 * capped, Remnants tamed to its owner, Sorcerers beaming heals into it, and all of them gone when it
 * dies or is stored away. Obliterate still raises Remnants from what it kills.
 *
 * <p>Boss-only parts turned off: the boss bar, experience, and its aura that turns arrows around
 * (it would send its owner's arrows back). Like the boss it ignores projectiles. Its area attacks
 * pass over friendlies (SoulslikeGoalMixin); block breaking follows the bossesBreakBlocks option.
 */
public class ReturningKnightServant extends ReturningKnight implements BossServant {
    private static final EntityDataAccessor<Byte> SERVANT_FLAGS = SynchedEntityData.defineId(ReturningKnightServant.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Optional<UUID>> OWNER = SynchedEntityData.defineId(ReturningKnightServant.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Integer> OWNER_CLIENT = SynchedEntityData.defineId(ReturningKnightServant.class, EntityDataSerializers.INT);
    /** Blocks between its hitbox and its target's at which it stops walking in (its Blinding Reflection reaches 5 from its centre). */
    private static final double HOLD_GAP = 1.5;

    /** The servant whose attack goal is running right now; Remnants and Sorcerers its goal spawns become its minions (ModEvents). */
    @Nullable
    public static ReturningKnightServant summoning;

    private final ServantData servantData = new ServantData(this, SERVANT_FLAGS, OWNER, OWNER_CLIENT);

    public ReturningKnightServant(EntityType<? extends ReturningKnightServant> type, Level level) {
        super(type, level);
        this.xpReward = 0;
        this.bossBar.setVisible(false);
        // 8 blocks tall: walks up two-block steps instead of getting stuck on them.
        this.setMaxUpStep(2.0f);
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
        this.goalSelector.addGoal(2, new ReturningKnightGoal(this) {
            @Override
            public void tick() {
                summoning = ReturningKnightServant.this;
                try {
                    super.tick();
                } finally {
                    summoning = null;
                }
                ReturningKnightServant.this.holdPositionWhenClose();
            }
        });
        this.goalSelector.addGoal(5, new Summoned.FollowOwnerGoal(this, 1.0, 10.0f, 2.0f));
        this.goalSelector.addGoal(8, new Summoned.WanderGoal(this, 1.0));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new Owned.OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(1, new Owned.OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(2, new ServantHurtByTargetGoal(this));
        this.targetSelector.addGoal(3, new SummonTargetGoal(this));
    }

    /**
     * The boss's goal walks straight at its target every tick with no stopping distance, so once
     * there it shoves the target around between attacks. All its attacks reach from a few blocks
     * away, so stop short once the gap between them is small. Attacks that move it on their own
     * (pathing to where Obliterate or Mace of Spades lands) are left alone.
     */
    private void holdPositionWhenClose() {
        LivingEntity target = this.getTarget();
        if (target == null || !this.getNavigation().isDone()) {
            return;
        }
        double gap = Math.sqrt(Math.pow(target.getX() - this.getX(), 2) + Math.pow(target.getZ() - this.getZ(), 2))
                - (this.getBbWidth() + target.getBbWidth()) / 2.0;
        if (gap < HOLD_GAP) {
            this.getMoveControl().setWantedPosition(this.getX(), this.getY(), this.getZ(), 0.0);
        }
    }

    /**
     * The boss's animation plays "walk" while it's aggressive and "idle" otherwise, since the boss
     * never stops advancing in a fight. The servant walks out of combat (following its owner) and
     * stands still in combat (holdPositionWhenClose), so on the client "aggressive" means moving,
     * by GeckoLib's own isMoving threshold. The server keeps the real flag.
     */
    @Override
    public boolean isAggressive() {
        if (this.level().isClientSide) {
            return this.walkAnimation.speed() >= 0.15f;
        }
        return super.isAggressive();
    }

    // ----- Minions -----------------------------------------------------------------------------

    /**
     * Called by ModEvents when its goal spawns a Remnant or Dark Sorcerer: claim it as a minion, or
     * stop it from spawning once the cap is reached.
     */
    public boolean claimMinion(Mob minion) {
        if (ServantMinions.count(this) >= ServantConfig.ReturningKnightMaxMinions.get()) {
            return false;
        }
        ServantMinions.tag(minion, this, minion instanceof DarkSorcerer);
        // Like Goety's Redstone Monstrosity cubes: the knight's, and through it the player's (tamed to them).
        if (minion instanceof TamableAnimal remnant && this.getOwnerId() != null) {
            remnant.setTame(true);
            remnant.setOwnerUUID(this.getOwnerId());
        }
        return true;
    }

    /**
     * A Remnant minion keeps to its knight rather than wandering off or following the player around
     * (ServantMinions moves it), and can't be told to sit mid-fight.
     */
    public static void adjustMinionGoals(Mob minion) {
        if (minion instanceof Remnant remnant) {
            ((MobAccessor) remnant).goetybossservants$getGoalSelector().removeAllGoals(goal -> goal instanceof FollowOwnerGoal || goal instanceof WaterAvoidingRandomStrollGoal || goal instanceof SitWhenOrderedToGoal);
            remnant.setOrderedToSit(false);
            remnant.setInSittingPose(false);
        }
    }

    @Override
    protected void customServerAiStep() {
        // Not calling super: the boss turns every arrow within 3 blocks around, the owner's included.
        // Block breaking is the only other thing it did here.
        if (ServantConfig.BossesBreakBlocks.get()) {
            this.breakSurroundingBlocks();
        }
    }

    // ----- Synced data, saving -----------------------------------------------------------------

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
        return super.isAlliedTo(entity) || BossServant.isAlliedTo(this, entity) || ServantMinions.isMinionOf(entity, this);
    }

    @Override
    public int getSummonLimit(LivingEntity owner) {
        return ServantConfig.ReturningKnightSummonLimit.get();
    }

    // ----- Boss-only behaviour turned off ------------------------------------------------------

    @Override
    public int getXp() {
        return 0;
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
        super.die(source);
        if (!this.level().isClientSide && this.getOwnerId() != null) {
            this.spawnAtLocation(ModItems.RETURNING_KNIGHT_SOUL);
        }
    }
}
