package com.saints.goetybossservants.entity.knightquest;

import com.Polarice3.Goety.common.entities.ai.ServantHurtByTargetGoal;
import com.Polarice3.Goety.common.entities.ai.SummonTargetGoal;
import com.Polarice3.Goety.common.entities.ally.Summoned;
import com.Polarice3.Goety.common.entities.neutral.Owned;
import com.mojang.logging.LogUtils;
import com.saints.goetybossservants.config.ServantConfig;
import com.saints.goetybossservants.entity.servant.BossServant;
import com.saints.goetybossservants.entity.servant.ServantData;
import com.saints.goetybossservants.entity.servant.ServantMinions;
import com.saints.goetybossservants.mixin.NethermanAccessor;
import com.saints.goetybossservants.registry.ModItems;
import dev.xylonity.knightquest.common.entity.boss.NethermanEntity;
import dev.xylonity.knightquest.common.entity.boss.ai.NethermanAttackGoal;
import dev.xylonity.knightquest.common.entity.boss.ai.NethermanClonesGoal;
import dev.xylonity.knightquest.common.entity.boss.ai.NethermanDarknessGoal;
import dev.xylonity.knightquest.common.entity.boss.ai.NethermanFlameGoal;
import dev.xylonity.knightquest.common.entity.boss.ai.NethermanIceGoal;
import dev.xylonity.knightquest.common.entity.boss.ai.NethermanLavaTeleportGoal;
import dev.xylonity.knightquest.common.entity.boss.ai.NethermanProjectileChargesGoal;
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
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.Optional;
import java.util.UUID;

/**
 * Knights Quest's Netherman ("The Architect of Chaos") as a Goety servant. Its model, animations,
 * three forms (Fire above 66% health, Ice to 33%, Magic below, each switch a brief invulnerable
 * transformation), melee, teleport barrage, curses, clones and explosive orbs all come from Knights
 * Quest's own NethermanEntity and its goals; this swaps the boss's player targeting for Goety's.
 *
 * <p>Changed for a servant:
 * <ul>
 *     <li>Its clones and orbs are its minions (ServantMinions): they only go for its target, never
 *     the owner or allies, their bursts and blasts spare friendlies, and they go when it does.</li>
 *     <li>The darkness curse only hits its target (the boss's hit every player), and the fire-form
 *     barrage leaves lava only when bossesBreakBlocks is on (NethermanDarknessGoalMixin,
 *     NethermanLavaTeleportGoalMixin).</li>
 *     <li>No "teleport to the nearest player" goal (it follows its owner instead), and its teleport
 *     when hit is a short hop that keeps it in the fight.</li>
 *     <li>No boss bar, boss music, summoning cinematic, experience or boss loot.</li>
 * </ul>
 */
public class NethermanServant extends NethermanEntity implements BossServant {
    private static final EntityDataAccessor<Byte> SERVANT_FLAGS = SynchedEntityData.defineId(NethermanServant.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Optional<UUID>> OWNER = SynchedEntityData.defineId(NethermanServant.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Integer> OWNER_CLIENT = SynchedEntityData.defineId(NethermanServant.class, EntityDataSerializers.INT);
    /** How far its teleport when hit may take it (the boss's could go 20 blocks out and 32 up or down). */
    private static final double HIT_TELEPORT_RANGE = 5.0;
    private static final int HIT_TELEPORT_TRIES = 8;
    /** A hop that would land it more than this far down (into a pit or a cave) is undone. */
    private static final double HIT_TELEPORT_MAX_DROP = 3.0;
    /**
     * Countdown values its attacks open with (see primeAttack). Flame, freeze and darkness fire at about
     * 1.5 (teleport) and 2.5 seconds (attack) from 60; the barrage at about 3 and clones at about 3.5 seconds from 80.
     */
    private static final int ATTACK_OPENS_AT = 60;
    private static final int BARRAGE_OPENS_AT = 80;
    private static final Logger LOGGER = LogUtils.getLogger();

    /** The servant whose clone or orb goal is running right now; what it spawns becomes its minions (KnightQuestCompat). */
    @Nullable
    public static NethermanServant summoning;

    private final ServantData servantData = new ServantData(this, SERVANT_FLAGS, OWNER, OWNER_CLIENT);

    public NethermanServant(EntityType<? extends NethermanServant> type, Level level) {
        super(type, level);
        this.xpReward = 0;
        ((NethermanAccessor) this).goetybossservants$getBossInfo().setVisible(false);
    }

    @Override
    public ServantData servantData() {
        return this.servantData;
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    protected void registerGoals() {
        // Not calling super: that adds the boss's player targeting and its teleport-to-player goal.
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new NethermanAttackGoal(this, 0.5, true));
        // Each attack opens its form (and each new fight) early instead of after its full cycle: see primeAttack.
        this.goalSelector.addGoal(2, new NethermanLavaTeleportGoal(this) {
            @Override
            public void start() {
                super.start();
                primeAttack(this, BARRAGE_OPENS_AT);
            }
        });
        this.goalSelector.addGoal(3, new NethermanFlameGoal(this) {
            @Override
            public void start() {
                super.start();
                primeAttack(this, ATTACK_OPENS_AT);
            }
        });
        this.goalSelector.addGoal(2, new NethermanClonesGoal(this) {
            @Override
            public void start() {
                super.start();
                primeAttack(this, BARRAGE_OPENS_AT);
            }

            @Override
            public void tick() {
                NethermanServant.this.whileSummoning(super::tick);
            }
        });
        this.goalSelector.addGoal(3, new NethermanIceGoal(this) {
            @Override
            public void start() {
                super.start();
                primeAttack(this, ATTACK_OPENS_AT);
            }
        });
        this.goalSelector.addGoal(2, new NethermanProjectileChargesGoal(this) {
            @Override
            public void start() {
                super.start();
                primeAttack(this, BARRAGE_OPENS_AT);
            }

            @Override
            public void tick() {
                NethermanServant.this.whileSummoning(super::tick);
            }
        });
        this.goalSelector.addGoal(3, new NethermanDarknessGoal(this) {
            @Override
            public void start() {
                super.start();
                primeAttack(this, ATTACK_OPENS_AT);
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

    private void whileSummoning(Runnable goalTick) {
        summoning = this;
        try {
            goalTick.run();
        } finally {
            summoning = null;
        }
    }

    /** Called by KnightQuestCompat when its clone or orb goal spawns something: claim it, or stop it past the cap. */
    public boolean claimMinion(Entity minion) {
        if (minion instanceof net.minecraft.world.entity.Mob && ServantMinions.count(this) >= ServantConfig.NethermanMaxClones.get()) {
            return false;
        }
        ServantMinions.tag(minion, this, false);
        // The orbs' blasts are then this servant's (friendly fire, terrain: ExplosionMixin).
        if (minion instanceof net.minecraft.world.entity.projectile.Projectile orb) {
            orb.setOwner(this);
        }
        return true;
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
        // No five-second summoning cinematic: it arrives ready to fight.
        this.setIsSummoning(false);
        this.setNoMovement(false);
        this.setInvulnerability(false);
        this.setShouldSearchTarget(true);
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
        return super.isAlliedTo(entity) || BossServant.isAlliedTo(this, entity) || ServantMinions.isMinionOf(entity, this);
    }

    @Override
    public int getSummonLimit(LivingEntity owner) {
        return ServantConfig.NethermanSummonLimit.get();
    }

    /** The boss blinks up to 20 blocks away (and 32 up or down) when hit; the servant only hops a few blocks, and not while told to stay. */
    @Override
    protected boolean teleport() {
        if (this.level().isClientSide || !this.isAlive() || this.isStaying() || this.isGuardingArea()) {
            return false;
        }
        // randomTeleport searches downward from the spot for ground, so start at or above its feet (a
        // spot below them is inside the floor and searches down into whatever cave is under it), try a
        // few spots, and undo a hop that dropped it into a pit.
        double startY = this.getY();
        for (int i = 0; i < HIT_TELEPORT_TRIES; ++i) {
            double x = this.getX() + (this.random.nextDouble() - 0.5) * 2.0 * HIT_TELEPORT_RANGE;
            double y = startY + this.random.nextInt(3);
            double z = this.getZ() + (this.random.nextDouble() - 0.5) * 2.0 * HIT_TELEPORT_RANGE;
            double fromX = this.getX();
            double fromZ = this.getZ();
            if (this.randomTeleport(x, y, z, true)) {
                if (this.getY() >= startY - HIT_TELEPORT_MAX_DROP) {
                    return true;
                }
                this.teleportTo(fromX, startY, fromZ);
            }
        }
        return false;
    }

    /**
     * Knights Quest's form attacks run on a countdown that starts full (10 or 15 seconds, and only
     * counting while the target is in sight) each time the attack becomes available: on a new target
     * and after each form change. A servant's fights are often over before that, so it only ever
     * meleed. Instead each attack opens early, then repeats on Knights Quest's normal cycle.
     */
    private static void primeAttack(Goal goal, int ticks) {
        try {
            for (Class<?> type = goal.getClass(); type != null; type = type.getSuperclass()) {
                try {
                    Field charge = type.getDeclaredField("chargeTime");
                    charge.setAccessible(true);
                    charge.setInt(goal, ticks);
                    return;
                } catch (NoSuchFieldException ignored) {
                    // Declared on Knights Quest's goal class, above our anonymous subclass.
                }
            }
        } catch (IllegalAccessException e) {
            LOGGER.warn("Couldn't speed up the Netherman servant's {}", goal.getClass().getSuperclass().getSimpleName(), e);
        }
    }

    // ----- Boss-only behaviour turned off ------------------------------------------------------

    /** KnightLib plays the boss theme near any Netherman; not near a servant. */
    @Override
    public boolean shouldPlayBossMusic(Player listener) {
        return false;
    }

    @Override
    protected void dropCustomDeathLoot(@NotNull DamageSource source, int looting, boolean recentlyHit) {
        // No Chaotic Essence or music disc; the soul is dropped in die().
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean isPreventingPlayerRest(@NotNull Player player) {
        return this.isHostile();
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
            this.spawnAtLocation(ModItems.NETHERMAN_SOUL);
        }
    }
}
