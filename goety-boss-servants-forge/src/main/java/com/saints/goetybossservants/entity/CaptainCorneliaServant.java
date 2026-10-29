package com.saints.goetybossservants.entity;

import com.Polarice3.Goety.common.entities.ai.ServantHurtByTargetGoal;
import com.Polarice3.Goety.common.entities.ai.SummonTargetGoal;
import com.Polarice3.Goety.common.entities.ai.servant.ServantFollowOwnerGoal;
import com.Polarice3.Goety.common.entities.ai.servant.ServantPatrolGoal;
import com.Polarice3.Goety.common.entities.ai.servant.ServantWanderGoal;
import com.Polarice3.Goety.common.entities.neutral.Owned;
import com.saints.goetybossservants.config.ServantConfig;
import com.saints.goetybossservants.entity.servant.BossServant;
import com.saints.goetybossservants.entity.servant.ServantData;
import com.saints.goetybossservants.registry.ModRegistry;
import dev.obscuria.aquamirae.common.entity.CaptainCorneliaBoss;
import dev.obscuria.aquamirae.registry.AquamiraeItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Aquamirae's Captain Cornelia (Maze Mother) as a Goety servant. Her model, animations, sounds,
 * particles, attacks (pull, swing, thrust) and rage all come from Aquamirae's own
 * CaptainCorneliaBoss; this swaps her boss AI for Goety's servant AI.
 *
 * <p>Changed for a servant:
 * <ul>
 *     <li>She picks the best weapon for the fight ({@link #chooseWeapon}) instead of Aquamirae's
 *     weighted random pick, and switches quickly when a better one is needed.</li>
 *     <li>Her Remnant Saber heals her on hits that land.</li>
 *     <li>Her Shatterblade cuts 10% max health per hit (two Health Shatter stacks) for a minute per hit,
 *     up to 50% for 5 minutes (Aquamirae: 5% for 12 seconds).</li>
 *     <li>Her swing and rage spin pass over friendlies (owner, allies, players, pets, player-owned
 *     servants), and her rage brings no Shipbreaker or boss encounter (CaptainCorneliaMixin).</li>
 *     <li>Her low-health regeneration runs for one 10-second window per fight, and her rage and
 *     that window come back after time out of combat.</li>
 *     <li>No boss bar, spawn cinematic, weapon drop or experience.</li>
 * </ul>
 * On death she drops her soul, which revives her through a Dark Altar ritual.
 */
public class CaptainCorneliaServant extends CaptainCorneliaBoss implements BossServant {
    private static final EntityDataAccessor<Byte> SERVANT_FLAGS = SynchedEntityData.defineId(CaptainCorneliaServant.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Optional<UUID>> OWNER = SynchedEntityData.defineId(CaptainCorneliaServant.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Integer> OWNER_CLIENT = SynchedEntityData.defineId(CaptainCorneliaServant.class, EntityDataSerializers.INT);
    /** Aquamirae starts her slow regeneration at this much health. */
    private static final float LAST_STAND_HEALTH = 20.0f;
    private static final int LAST_STAND_TICKS = 200;
    /** Aquamirae equips the new weapon at tick 10 of her switch; a quick switch starts it at tick 5. */
    private static final int QUICK_SWITCH_START = 5;
    private static final int QUICK_SWITCH_LENGTH = 15;

    private final ServantData servantData = new ServantData(this, SERVANT_FLAGS, OWNER, OWNER_CLIENT);
    private boolean inBaseTick;
    private int lastStandTicks;
    private boolean lastStandUsed;
    private int outOfCombatTicks;
    /** Set below half health; she keeps the Remnant Saber until healed back up. */
    private boolean healingWithSaber;
    /** The weapon her running switch animation equips (null: Aquamirae's own periodic switch, so pick one then). */
    @Nullable
    private Item pendingWeapon;

    @SuppressWarnings({"unchecked", "rawtypes"})
    public CaptainCorneliaServant(EntityType<? extends CaptainCorneliaServant> type, Level level) {
        // Aquamirae's constructor asks for exactly EntityType<CaptainCorneliaBoss>.
        super((EntityType) type, level);
        this.xpReward = 0;
        this.bossInfo.setVisible(false);
        this.setFollowRange();
    }

    @Override
    public ServantData servantData() {
        return this.servantData;
    }

    private void setFollowRange() {
        AttributeInstance followRange = this.getAttribute(Attributes.FOLLOW_RANGE);
        if (followRange != null) {
            followRange.setBaseValue(ServantConfig.CaptainCorneliaFollowRange.get());
        }
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    protected void registerGoals() {
        // Not calling super: that adds her boss targeting (players, villagers, illagers).
        this.goalSelector.addGoal(1, new FloatGoal(this));
        // Chase only: her attacks are started from baseTick, as on the boss.
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, false) {
            @Override
            protected double getAttackReachSqr(@NotNull LivingEntity entity) {
                return 0.0;
            }
        });
        this.goalSelector.addGoal(3, new ServantPatrolGoal(this, 1.25));
        this.goalSelector.addGoal(5, new ServantFollowOwnerGoal(this, 1.0, 10.0f, 2.0f));
        this.goalSelector.addGoal(8, new ServantWanderGoal(this, 1.0));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));
        // Whatever the owner hits or is hit by outranks a target she picked by herself.
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
        tag.putBoolean("LastStandUsed", this.lastStandUsed);
        tag.putInt("LastStandTicks", this.lastStandTicks);
        tag.putBoolean("HealingWithSaber", this.healingWithSaber);
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        BossServant.read(this, tag);
        this.lastStandUsed = tag.getBoolean("LastStandUsed");
        this.lastStandTicks = tag.getInt("LastStandTicks");
        this.healingWithSaber = tag.getBoolean("HealingWithSaber");
        this.setFollowRange();
    }

    @Override
    public SpawnGroupData finalizeSpawn(@NotNull ServerLevelAccessor level, @NotNull DifficultyInstance difficulty, @NotNull MobSpawnType reason,
                                        @Nullable SpawnGroupData data, @Nullable CompoundTag tag) {
        // Aquamirae applies its Cornelia config here (health, armor, damage, ...); keep that, but not
        // its follow range, and skip the boss's rising-from-the-sea levitation.
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, data, tag);
        this.removeEffect(MobEffects.LEVITATION);
        this.setFollowRange();
        return result;
    }

    @Override
    public void tick() {
        // Before her own tick: see isBetweenAttacks.
        if (!this.level().isClientSide) {
            this.updateWeapon();
        }
        super.tick();
        this.ownedTick();
        this.servantTick();
    }

    // ----- Weapon choice -----------------------------------------------------------------------

    /**
     * Picks her weapon for the current target, in this order:
     * <ol>
     *     <li>below half health: the Remnant Saber (its hits heal her) until she's back to 90%,</li>
     *     <li>a target her main weapon (Tidepiercer, or Jawbreaker in winter) kills in two hits: that,</li>
     *     <li>a target with 150+ max health: the Shatterblade, until its Health Shatter is fully stacked,</li>
     *     <li>anything else: a random pick of her main weapon, the Shatterspike, and the Terrible Blade
     *     if the target can be poisoned. She sticks with that pick unless {@code reroll}.</li>
     * </ol>
     */
    public Item chooseWeapon(@Nullable LivingEntity target, boolean reroll) {
        this.updateSaberState();
        Item current = this.getMainHandItem().getItem();
        if (this.healingWithSaber) {
            return AquamiraeItems.REMNANT_SABER.get();
        }
        Item main = this.getSeasonalWeapon();
        if (target == null) {
            return current == AquamiraeItems.REMNANT_SABER.get() || current == Items.AIR ? main : current;
        }
        if (target.getHealth() <= 2.0f * CorneliaWeapons.weaponDamage(this, main, target)) {
            return main;
        }
        if (CorneliaWeapons.originalMaxHealth(target) >= ServantConfig.CaptainCorneliaShatterbladeMinHealth.get()
                && CorneliaWeapons.shatterStacks(target) < CorneliaWeapons.SHATTER_MAX_STACKS) {
            return AquamiraeItems.SHATTERBLADE.get();
        }
        List<Item> options = new ArrayList<>(List.of(main, AquamiraeItems.SHATTERSPIKE.get()));
        if (CorneliaWeapons.canBePoisoned(target)) {
            options.add(AquamiraeItems.TERRIBLE_BLADE.get());
        }
        if (!reroll && options.contains(current)) {
            return current;
        }
        return options.get(this.random.nextInt(options.size()));
    }

    /**
     * CaptainCorneliaMixin: Aquamirae is equipping a weapon loadout (when she spawns, and halfway
     * through her switch animation). Equip the chosen weapon instead: the one a quick switch was
     * started for, or a fresh pick for Aquamirae's own occasional switch.
     */
    public void equipChosenWeapon() {
        Item weapon = this.pendingWeapon != null ? this.pendingWeapon : this.chooseWeapon(this.getTarget(), true);
        this.pendingWeapon = null;
        this.equipWeapon(weapon);
    }

    public void equipWeapon(Item weapon) {
        this.setItemSlot(EquipmentSlot.MAINHAND, weapon.getDefaultInstance());
        this.setItemSlot(EquipmentSlot.OFFHAND, CorneliaWeapons.dualWielded(weapon) ? weapon.getDefaultInstance() : ItemStack.EMPTY);
    }

    /** Below half health she takes the Remnant Saber until she's back to 90% (config saberBelowHealth / saberUntilHealth). */
    private void updateSaberState() {
        float health = this.getHealth() / this.getMaxHealth();
        if (this.healingWithSaber && health >= ServantConfig.CaptainCorneliaSaberUntil.get()) {
            this.healingWithSaber = false;
        } else if (!this.healingWithSaber && health < ServantConfig.CaptainCorneliaSaberBelow.get()) {
            this.healingWithSaber = true;
        }
    }

    /** Starts a quick switch (half of Aquamirae's 20 ticks) when a different weapon suits the fight better. */
    private void updateWeapon() {
        // Every tick, so a quick drop below half health is noticed even mid-attack (her rage heals fast).
        this.updateSaberState();
        LivingEntity target = this.getTarget();
        if (target == null || !this.isBetweenAttacks()) {
            return;
        }
        Item wanted = this.chooseWeapon(target, false);
        if (wanted != this.getMainHandItem().getItem()) {
            this.pendingWeapon = wanted;
            this.weaponSwitchState.play(this, QUICK_SWITCH_START, QUICK_SWITCH_LENGTH);
        }
    }

    /**
     * Her pull chains into a swing and her swing into a thrust, and next to a target she starts the
     * next chain the moment a thrust ends. The only free moment is the last tick of a thrust (or no
     * attack at all); this runs before her tick, so a switch started here also stops baseTick from
     * starting the next attack.
     */
    private boolean isBetweenAttacks() {
        if (this.pullAttackState.isPlaying() || this.swingAttackState.isPlaying() || this.weaponSwitchState.isPlaying()) {
            return false;
        }
        return !this.thurstAttackState.isPlaying() || this.thurstAttackState.getTick() >= this.thurstAttackState.getDuration() - 1;
    }

    // ----- Rage and last stand -----------------------------------------------------------------

    @Override
    public void baseTick() {
        if (!this.level().isClientSide) {
            this.updatePhases();
        }
        this.inBaseTick = true;
        try {
            super.baseTick();
        } finally {
            this.inBaseTick = false;
        }
    }

    /** Runs the last-stand window, and gives back her rage and last stand after time out of combat. */
    private void updatePhases() {
        if (this.lastStandTicks > 0) {
            --this.lastStandTicks;
        } else if (!this.lastStandUsed && this.isAlive() && this.getHealth() <= LAST_STAND_HEALTH) {
            this.lastStandUsed = true;
            this.lastStandTicks = LAST_STAND_TICKS;
        }
        if (this.getTarget() != null || this.hurtTime > 0) {
            this.outOfCombatTicks = 0;
        } else if (++this.outOfCombatTicks >= ServantConfig.CaptainCorneliaPhaseResetSeconds.get() * 20) {
            this.setPhase(0);
            this.lastStandUsed = false;
        }
    }

    /**
     * Aquamirae heals her a little every tick at 20 health or less. Here that only works during her
     * last-stand window (see updatePhases). Heals on the client are dropped: the client's copy never
     * learns when her rage levitation ends and would show her healing forever.
     */
    @Override
    public void heal(float amount) {
        if (this.level().isClientSide) {
            return;
        }
        if (this.inBaseTick && !this.hasEffect(MobEffects.LEVITATION) && this.getHealth() <= LAST_STAND_HEALTH && this.lastStandTicks <= 0) {
            return;
        }
        super.heal(amount);
    }

    @Override
    public void setHealth(float health) {
        BossServant.logHealing(this, health);
        super.setHealth(health);
    }

    public boolean isLastStandActive() {
        return this.lastStandTicks > 0;
    }

    public boolean isLastStandUsed() {
        return this.lastStandUsed;
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
        return ServantConfig.CaptainCorneliaSummonLimit.get();
    }

    /**
     * Her swing hits everything in front of her; never let it land on the owner, an ally, or anyone
     * her area attacks spare unless it's what she's fighting. The Remnant Saber heals her on hits that
     * land (Aquamirae 7's boss doesn't heal from it; this servant does), and her Shatterblade's Health
     * Shatter is stronger and lasts longer than Aquamirae's (see CorneliaWeapons.applyShatter).
     */
    @Override
    public boolean doHurtTarget(@NotNull Entity entity) {
        if (BossServant.isFriendly(this, entity) || entity != this.getTarget() && BossServant.sparedByAreaAttacks(this, entity)) {
            return false;
        }
        int stacksBefore = entity instanceof LivingEntity living ? CorneliaWeapons.shatterStacks(living) : 0;
        boolean hurt = super.doHurtTarget(entity);
        if (hurt && entity instanceof LivingEntity living && this.getMainHandItem().is(AquamiraeItems.SHATTERBLADE.get())) {
            CorneliaWeapons.applyShatter(living, stacksBefore);
        }
        if (hurt && entity instanceof LivingEntity living && this.getMainHandItem().is(AquamiraeItems.REMNANT_SABER.get())) {
            this.heal(Math.min(30.0f, living.getMaxHealth() * 0.4f));
        }
        return hurt;
    }

    // ----- Boss-only behaviour turned off ------------------------------------------------------

    @Override
    protected void dropEquipment() {
        // No seasonal weapon; the soul is dropped in die().
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
    public void die(@NotNull DamageSource source) {
        super.die(source);
        if (!this.level().isClientSide && this.getOwnerId() != null) {
            this.spawnAtLocation(ModRegistry.CAPTAIN_CORNELIA_SOUL.get());
        }
    }

    /** Aquamirae 7 builds her attributes on Monster's and applies its Cornelia config in finalizeSpawn. */
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes();
    }
}
