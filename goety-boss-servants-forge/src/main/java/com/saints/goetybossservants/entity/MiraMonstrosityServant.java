package com.saints.goetybossservants.entity;

import com.Polarice3.Goety.api.entities.IAutoRideable;
import com.Polarice3.Goety.api.entities.IOwned;
import com.Polarice3.Goety.api.entities.ally.IServant;
import com.Polarice3.Goety.api.items.magic.IWand;
import com.Polarice3.Goety.client.particles.ModParticleTypes;
import com.Polarice3.Goety.common.blocks.ModBlocks;
import com.Polarice3.Goety.common.effects.GoetyEffects;
import com.Polarice3.Goety.common.entities.ai.ServantHurtByTargetGoal;
import com.Polarice3.Goety.common.entities.ai.SummonTargetGoal;
import com.Polarice3.Goety.common.entities.ai.servant.ServantFollowOwnerGoal;
import com.Polarice3.Goety.common.entities.ai.servant.ServantPatrolGoal;
import com.Polarice3.Goety.common.entities.ai.servant.ServantWanderGoal;
import com.Polarice3.Goety.common.entities.neutral.Owned;
import com.Polarice3.Goety.config.MobsConfig;
import com.Polarice3.Goety.utils.EntityFinder;
import com.Polarice3.Goety.utils.MathHelper;
import com.Polarice3.Goety.utils.MobUtil;
import com.Polarice3.Goety.utils.ModDamageSource;
import com.jerotes.meowhem.entity.Boss.MiraMonstrosityEntity;
import com.jerotes.meowhem.goal.MiraMonstrosityMeleeAttackGoal;
import com.saints.goetybossservants.config.ServantConfig;
import com.saints.goetybossservants.item.MiraMonstrosityHeadItem;
import com.saints.goetybossservants.mixin.MiraMonstrosityAccessor;
import com.saints.goetybossservants.registry.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.PlayerRideable;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Meowhem's Mira Monstrosity, made into a Goety servant that behaves like Goety's Redstone Monstrosity.
 *
 * <p>All of the boss's combat, animation and variant logic is inherited untouched; this class swaps the
 * boss AI for Goety's servant AI and adds the Redstone Monstrosity's servant features (commands,
 * repairing, riding, head drop on death).
 *
 * <p>Goety's {@link IServant} is attached by {@code MiraMonstrosityServantMixin}, not declared here:
 * {@code IOwned.getOwnerId()} returns a UUID while meowhem's {@code MiraMachineEntity.getOwnerId()}
 * returns an int, which Java won't allow in one source file but the JVM is fine with. Every other
 * IServant/IOwned method is implemented below under its real name; {@link #servant()} reaches the
 * interface's default methods.
 */
public class MiraMonstrosityServant extends MiraMonstrosityEntity implements IAutoRideable, PlayerRideable {
    private static final EntityDataAccessor<Byte> SERVANT_FLAGS = SynchedEntityData.defineId(MiraMonstrosityServant.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> VARIANT = SynchedEntityData.defineId(MiraMonstrosityServant.class, EntityDataSerializers.INT);
    private static final int FLAG_WANDERING = 1;
    private static final int FLAG_STAYING = 2;
    private static final int FLAG_UPGRADED = 4;
    private static final int FLAG_HOSTILE = 8;
    private static final int FLAG_NATURAL = 16;
    private static final int FLAG_AUTONOMOUS = 32;

    @Nullable
    private LivingEntity priorityTarget;
    @Nullable
    private LivingEntity commandPosEntity;
    @Nullable
    private BlockPos commandPos;
    @Nullable
    private BlockPos priorityPos;
    @Nullable
    private BlockPos boundPos;
    private String boundDim = Level.OVERWORLD.location().toString();
    private List<GlobalPos> patrolList = new ArrayList<>();
    private int patrolIndex;
    private int priorityTime;
    private int commandTick;
    private int killChance;
    private int noHealTime;
    private long ticketTime;
    private boolean limitedLifespan;
    private int limitedLifeTicks;
    private int hasSummonCheck;
    private int revivingTime;
    private int pacifyTime;

    public MiraMonstrosityServant(EntityType<? extends MiraMonstrosityServant> type, Level level) {
        super(type, level);
        this.xpReward = 0;
        // Meowhem's constructor may have applied its own boss config; the servant config wins.
        this.setConfigurableAttributes();
        this.setHealth(this.getMaxHealth());
        // Meowhem freezes a tamed Monstrosity's rotation unless it's in "wander" mode, so keep that on.
        // Goety's own wander/stay/follow modes are tracked separately below.
        this.setWander(true);
    }

    /** Applied on creation and again on load (saved attributes would otherwise win), like Goety's servants. */
    public void setConfigurableAttributes() {
        setBase(Attributes.MAX_HEALTH, ServantConfig.MiraMonstrosityHealth.get());
        setBase(Attributes.ATTACK_DAMAGE, ServantConfig.MiraMonstrosityDamage.get());
        setBase(Attributes.ARMOR, ServantConfig.MiraMonstrosityArmor.get());
        // The boss's 128-block range made it chase mobs far away from the owner.
        setBase(Attributes.FOLLOW_RANGE, ServantConfig.MiraMonstrosityFollowRange.get());
    }

    private void setBase(Attribute attribute, double value) {
        AttributeInstance instance = this.getAttribute(attribute);
        if (instance != null) {
            instance.setBaseValue(value);
        }
    }

    /** This entity as Goety's servant interface (added by mixin), for calling its default methods. */
    public IServant servant() {
        return (IServant) (Object) this;
    }

    // ----- Goals -------------------------------------------------------------------------------

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    protected void registerGoals() {
        // Deliberately not calling super: that would add meowhem's tame/faction/boss target goals.
        // Goety's goals are bounded by "Mob & IServant"; IServant only appears at runtime (see the
        // class comment), so they're created raw.
        this.goalSelector.addGoal(1, new MiraMonstrosityMeleeAttackGoal(this, true));
        this.goalSelector.addGoal(2, new ServantPatrolGoal(this, 1.25));
        this.goalSelector.addGoal(5, new ServantFollowOwnerGoal(this, 1.0, 10.0f, 2.0f));
        this.goalSelector.addGoal(8, new ServantWanderGoal(this, 1.0));
        this.goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 3.0f, 1.0f));
        this.goalSelector.addGoal(10, new LookAtPlayerGoal(this, Mob.class, 8.0f));
        // Unlike Goety's own order (all at 1-2), whatever the owner hits or is hit by outranks a target
        // the servant picked by itself, so it can switch off a self-chosen target mid-fight.
        this.targetSelector.addGoal(1, new Owned.OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(1, new Owned.OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(2, new ServantHurtByTargetGoal(this));
        this.targetSelector.addGoal(3, new SummonTargetGoal(this));
    }

    // ----- Synced data / saving ----------------------------------------------------------------

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(SERVANT_FLAGS, (byte) 0);
        this.entityData.define(VARIANT, 0);
    }

    private boolean getFlag(int mask) {
        return (this.entityData.get(SERVANT_FLAGS) & mask) != 0;
    }

    private void setFlag(int mask, boolean value) {
        int flags = this.entityData.get(SERVANT_FLAGS);
        flags = value ? flags | mask : flags & ~mask;
        this.entityData.set(SERVANT_FLAGS, (byte) flags);
    }

    public MiraVariant getVariant() {
        return MiraVariant.byOrdinal(this.entityData.get(VARIANT));
    }

    public void setVariant(MiraVariant variant) {
        this.entityData.set(VARIANT, variant.ordinal());
    }

    @Override
    protected Component getTypeName() {
        return this.getVariant().getTypeName();
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("MiraVariant", this.getVariant().getId());
        tag.putBoolean("Autonomous", this.isAutonomous());
        this.servant().saveOwnedData(tag);
        this.servant().saveServantData(tag);
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.setVariant(MiraVariant.byId(tag.getString("MiraVariant")));
        this.setAutonomous(tag.getBoolean("Autonomous"));
        this.servant().readOwnedData(tag);
        this.servant().readServantData(tag);
        this.setConfigurableAttributes();
        this.setWander(true);
    }

    // ----- Ownership (IOwned) ------------------------------------------------------------------
    // The owner lives in meowhem's own owner fields so its attack code (which asks OwnableEntity for
    // the owner) also treats the owner and the owner's allies as friends.

    /** IOwned#getOwnerId(): exposed under that name by the mixin. */
    @Nullable
    public UUID getGoetyOwnerId() {
        return this.getOwnerUUID();
    }

    public void setOwnerId(@Nullable UUID uuid) {
        this.setOwnerUUID(uuid);
        this.setTame(uuid != null);
    }

    public int getOwnerClientId() {
        return this.getOwnerId();
    }

    public void setOwnerClientId(int id) {
        this.setOwnerId(id);
    }

    @Nullable
    public LivingEntity getTrueOwner() {
        if (this.hasEffect(GoetyEffects.WILD_RAGE.get())) {
            return null;
        }
        if (!this.level().isClientSide) {
            UUID uuid = this.getOwnerUUID();
            return uuid == null ? null : EntityFinder.getLivingEntityByUuiD(this.level(), uuid);
        }
        int id = this.getOwnerId();
        return id <= -1 ? null : this.level().getEntity(id) instanceof LivingEntity living && living != this ? living : null;
    }

    @Nullable
    @Override
    public LivingEntity getOwner() {
        return this.getTrueOwner();
    }

    public boolean isHostile() {
        return this.getFlag(FLAG_HOSTILE);
    }

    public void setHostile(boolean hostile) {
        this.setFlag(FLAG_HOSTILE, hostile);
    }

    public boolean isNatural() {
        return this.getFlag(FLAG_NATURAL);
    }

    public void setNatural(boolean natural) {
        this.setFlag(FLAG_NATURAL, natural);
    }

    public boolean hasLifespan() {
        return this.limitedLifespan;
    }

    public void setHasLifespan(boolean lifespan) {
        this.limitedLifespan = lifespan;
    }

    public int getLifespan() {
        return this.limitedLifeTicks;
    }

    public void setLifespan(int lifespan) {
        this.limitedLifeTicks = lifespan;
    }

    public int getHasSummonCheck() {
        return this.hasSummonCheck;
    }

    public void setHasSummonCheck(int check) {
        this.hasSummonCheck = check;
    }

    public int getRevivingTime() {
        return this.revivingTime;
    }

    public void setRevivingTime(int tick) {
        this.revivingTime = tick;
    }

    public int getPacifyTick() {
        return this.pacifyTime;
    }

    public void setPacifyTick(int tick) {
        this.pacifyTime = tick;
    }

    // Same ally rules as Goety's Owned.
    @Override
    public boolean isAlliedTo(@NotNull Entity entity) {
        LivingEntity trueOwner = this.getTrueOwner();
        if (trueOwner != null) {
            return trueOwner.isAlliedTo(entity) || entity.isAlliedTo(trueOwner) || entity == trueOwner
                    || entity instanceof IOwned owned && MobUtil.ownerStack(this.servant(), owned)
                    || entity instanceof OwnableEntity ownable && ownable.getOwner() == trueOwner
                    || entity instanceof LivingEntity living && this.servant().isAllyWith(living);
        }
        return super.isAlliedTo(entity) || entity instanceof LivingEntity living && this.servant().isAllyWith(living);
    }

    @Override
    public boolean canBeLeashed(@NotNull Player player) {
        return false;
    }

    // ----- Servant state (IServant) ------------------------------------------------------------

    public boolean isWandering() {
        return this.getFlag(FLAG_WANDERING);
    }

    public void setWandering(boolean wandering) {
        this.setFlag(FLAG_WANDERING, wandering);
    }

    public boolean isStaying() {
        return this.getFlag(FLAG_STAYING) && !this.isCommanded() && this.getControllingPassenger() == null;
    }

    public void setStaying(boolean staying) {
        this.setFlag(FLAG_STAYING, staying);
    }

    public boolean isUpgraded() {
        return this.getFlag(FLAG_UPGRADED);
    }

    public void setUpgraded(boolean upgraded) {
        this.setFlag(FLAG_UPGRADED, upgraded);
    }

    public boolean canUpdateMove() {
        return true;
    }

    public boolean isCommanded() {
        return this.commandPos != null;
    }

    public void setCommandPos(BlockPos blockPos, boolean removeEntity) {
        if (removeEntity) {
            this.commandPosEntity = null;
        }
        this.commandPos = blockPos;
        this.setCommandTick(MathHelper.secondsToTicks(10));
    }

    @Nullable
    public BlockPos getCommandPos() {
        return this.commandPos;
    }

    public void setCommandPosEntity(@Nullable LivingEntity living) {
        this.commandPosEntity = living;
        if (living != null) {
            this.setCommandPos(living.blockPosition(), false);
        }
    }

    @Nullable
    public LivingEntity getCommandPosEntity() {
        return this.commandPosEntity;
    }

    public int getCommandTick() {
        return this.commandTick;
    }

    public void setCommandTick(int commandTick) {
        this.commandTick = commandTick;
    }

    @Nullable
    public BlockPos getBoundPos() {
        return this.boundPos;
    }

    public void setBoundPos(@Nullable BlockPos blockPos) {
        this.boundPos = blockPos;
        this.boundDim = this.level().dimension().location().toString();
    }

    public String getBoundDim() {
        return this.boundDim;
    }

    public void setBoundDim(String dim) {
        this.boundDim = dim;
    }

    public List<GlobalPos> getPatrolRoute() {
        return this.patrolList;
    }

    public void setPatrolRoute(List<GlobalPos> list) {
        this.patrolList = list;
    }

    public int getPatrolIndex() {
        return this.patrolIndex;
    }

    public void setPatrolIndex(int index) {
        this.patrolIndex = index;
    }

    @Nullable
    public LivingEntity getPriorityTarget() {
        return this.priorityTarget;
    }

    public void setPriorityTarget(@Nullable LivingEntity target) {
        this.overrideSetTarget(target);
        this.priorityTarget = target;
        if (target != null) {
            this.setPriorityTime(100);
            this.setPriorityPos(target.blockPosition());
        }
    }

    public int getPriorityTime() {
        return this.priorityTime;
    }

    public void setPriorityTime(int time) {
        this.priorityTime = time;
    }

    @Nullable
    public BlockPos getPriorityPos() {
        return this.priorityPos;
    }

    public void setPriorityPos(@Nullable BlockPos pos) {
        this.priorityPos = pos;
    }

    public int getNoHealTime() {
        return this.noHealTime;
    }

    public void setNoHealTime(int time) {
        this.noHealTime = time;
    }

    public int getKillChance() {
        return this.killChance;
    }

    public void setKillChance(int killChance) {
        this.killChance = killChance;
    }

    public long getTicketTime() {
        return this.ticketTime;
    }

    public void setTicketTime(long ticketTime) {
        this.ticketTime = ticketTime;
    }

    public long decreaseTicketTime() {
        return --this.ticketTime;
    }

    // Same targeting rules as Goety's Summoned.
    @Override
    public void setTarget(@Nullable LivingEntity target) {
        if (this.getPacifyTick() > 0) {
            this.overrideSetTarget(null);
        } else if (this.servant().isGuardingArea() && !this.servant().isPrioritizing()) {
            if (target == null || target.distanceToSqr(this.servant().vec3BoundPos()) <= Mth.square(IServant.GUARDING_RANGE)) {
                this.overrideSetTarget(target);
            }
        } else {
            this.overrideSetTarget(target);
        }
    }

    public void overrideSetTarget(@Nullable LivingEntity target) {
        super.setTarget(target);
    }

    // Goety's "are you sure?" warning before a servant can be dismissed with a wand.
    public void tryKill(Player player) {
        if (this.killChance <= 0) {
            this.killChance = 60;
            player.displayClientMessage(Component.translatable("info.goety.servant.tryKill", this.getDisplayName()), true);
        } else {
            this.hurt(ModDamageSource.getDamageSource(this.level(), ModDamageSource.DISMISSED), Float.MAX_VALUE);
        }
    }

    // ----- Ticking -----------------------------------------------------------------------------

    @Override
    public void tick() {
        super.tick();
        this.servant().ownedTick();
        this.servant().servantTick();
        if (!this.level().isClientSide) {
            // Reuse meowhem's tamed sit animation for Goety's "stay" mode.
            boolean staying = this.isStaying();
            if (this.isInSittingPose() != staying) {
                this.setOrderedToSit(staying);
                this.setInSittingPose(staying);
            }
            if (!this.isWander()) {
                this.setWander(true);
            }
        }
    }

    // ----- Boss-only behaviour turned off ------------------------------------------------------

    @Override
    public void startSeenByPlayer(@NotNull ServerPlayer player) {
        super.startSeenByPlayer(player);
        ((MiraMonstrosityAccessor) this).goetybossservants$getBossEvent().removePlayer(player);
    }

    @Override
    public SoundEvent getBossMusic() {
        return null;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, @NotNull DamageSource source) {
        return false;
    }

    // ----- Damage and death --------------------------------------------------------------------

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        if (MobsConfig.ServantsMasterImmune.get() && source.getEntity() instanceof IOwned owned
                && !owned.isHostile() && !this.isHostile() && this.getTrueOwner() != null && owned.getTrueOwner() == this.getTrueOwner()) {
            return false;
        }
        boolean flag = super.hurt(source, amount);
        if (flag) {
            this.setNoHealTime(MathHelper.secondsToTicks(MobsConfig.ServantHealHalt.get()));
        }
        return flag;
    }

    // No die() override: meowhem's MiraMachineEntity already sends the death message to the owner.

    @Override
    public void tickDeath() {
        // Meowhem removes the body when deathTime reaches 40; drop the head on that last tick, the
        // same moment Goety's Redstone Monstrosity drops its head.
        if (!this.level().isClientSide && this.deathTime == 39 && this.getOwnerUUID() != null) {
            ItemStack head = new ItemStack(ModRegistry.MIRA_MONSTROSITY_HEAD_ITEM.get());
            MiraMonstrosityHeadItem.setOwner(head, this.getOwnerUUID(), this.getTrueOwner());
            MiraMonstrosityHeadItem.setVariant(head, this.getVariant());
            if (this.getCustomName() != null) {
                MiraMonstrosityHeadItem.setCustomName(head, this.getCustomName().getString());
            }
            ItemEntity itemEntity = this.spawnAtLocation(head);
            if (itemEntity != null) {
                itemEntity.setExtendedLifetime();
            }
        }
        super.tickDeath();
    }

    // ----- Interaction: repair and riding ------------------------------------------------------

    @Override
    protected @NotNull InteractionResult mobInteract(@NotNull Player player, @NotNull InteractionHand hand) {
        if (this.level().isClientSide) {
            return this.getTrueOwner() == player ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }
        ItemStack stack = player.getItemInHand(hand);
        if (this.getTrueOwner() == null || player != this.getTrueOwner()) {
            return InteractionResult.PASS;
        }
        boolean reinforced = stack.is(ModBlocks.REINFORCED_REDSTONE_BLOCK.get().asItem());
        if ((reinforced || stack.is(Items.ORANGE_TERRACOTTA)) && this.getHealth() < this.getMaxHealth()) {
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            if (reinforced) {
                this.heal(this.getMaxHealth() / 4.0f);
                this.playSound(SoundEvents.IRON_GOLEM_REPAIR, 1.0f, 1.25f);
            } else {
                this.heal(this.getMaxHealth() / 4.0f / 8.0f);
                this.playSound(SoundEvents.IRON_GOLEM_REPAIR, 0.25f, 0.75f);
            }
            if (this.level() instanceof ServerLevel serverLevel) {
                for (int i = 0; i < 7; ++i) {
                    double d0 = serverLevel.random.nextGaussian() * 0.02;
                    double d1 = serverLevel.random.nextGaussian() * 0.02;
                    double d2 = serverLevel.random.nextGaussian() * 0.02;
                    serverLevel.sendParticles(ModParticleTypes.HEAL_EFFECT.get(), this.getRandomX(1.0), this.getRandomY(), this.getRandomZ(1.0), 0, d0, d1, d2, 0.5);
                }
            }
            return InteractionResult.SUCCESS;
        }
        if (this.getFirstPassenger() != null && this.getFirstPassenger() != player) {
            this.getFirstPassenger().stopRiding();
            return InteractionResult.SUCCESS;
        }
        if (!(stack.getItem() instanceof IWand)) {
            player.setYRot(this.getYRot());
            player.setXRot(this.getXRot());
            player.startRiding(this);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public void setAutonomous(boolean autonomous) {
        this.setFlag(FLAG_AUTONOMOUS, autonomous);
        if (autonomous) {
            this.playSound(SoundEvents.ARROW_HIT_PLAYER);
            if (!this.isWandering()) {
                this.setWandering(true);
                this.setStaying(false);
            }
        }
    }

    @Override
    public boolean isAutonomous() {
        return this.getFlag(FLAG_AUTONOMOUS);
    }

    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        if (!this.isNoAi()) {
            Entity entity = this.getFirstPassenger();
            if (entity instanceof Mob mob) {
                return MobsConfig.ServantRideAutonomous.get() ? null : mob;
            }
            if (entity instanceof LivingEntity living && !this.isAutonomous()) {
                return living;
            }
        }
        return null;
    }

    @Override
    public boolean isVehicle() {
        return !this.isAutonomous() && super.isVehicle();
    }

    @Override
    public boolean isControlledByLocalInstance() {
        return this.isEffectiveAi();
    }

    @Override
    public double getPassengersRidingOffset() {
        return this.getBbHeight();
    }

    private boolean isBusyAttacking() {
        return this.specialAction() && this.getRushUseTick() <= 10 || this.getAttackTick() > 0;
    }

    @Override
    public void travel(@NotNull Vec3 travelVector) {
        if (!this.isAlive()) {
            return;
        }
        LivingEntity rider = this.getControllingPassenger();
        if (rider instanceof Player player && !this.isAutonomous() && !this.isBusyAttacking()) {
            this.setYRot(rider.getYRot());
            this.yRotO = this.getYRot();
            this.setXRot(rider.getXRot() * 0.5f);
            this.setRot(this.getYRot(), this.getXRot());
            this.yBodyRot = this.getYRot();
            this.yHeadRot = this.yBodyRot;
            float speed = this.getRiddenSpeed(player);
            float strafe = rider.xxa * speed;
            float forward = rider.zza * speed;
            if (forward <= 0.0f) {
                forward *= 0.25f;
            }
            this.setSpeed((float) this.getAttributeValue(Attributes.MOVEMENT_SPEED));
            super.travel(new Vec3(strafe, travelVector.y, forward));
            this.lerpSteps = 0;
            this.calculateEntityAnimation(false);
            return;
        }
        super.travel(travelVector);
    }
}
