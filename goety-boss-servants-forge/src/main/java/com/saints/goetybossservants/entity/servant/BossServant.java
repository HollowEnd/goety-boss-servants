package com.saints.goetybossservants.entity.servant;

import com.Polarice3.Goety.api.entities.IOwned;
import com.Polarice3.Goety.api.entities.ally.IServant;
import com.Polarice3.Goety.common.effects.GoetyEffects;
import com.Polarice3.Goety.config.MobsConfig;
import com.Polarice3.Goety.utils.EntityFinder;
import com.Polarice3.Goety.utils.MathHelper;
import com.Polarice3.Goety.utils.MobUtil;
import com.Polarice3.Goety.utils.ModDamageSource;
import com.mojang.logging.LogUtils;
import com.saints.goetybossservants.config.ServantConfig;
import com.saints.goetybossservants.entity.monstrosity.RisenMobs;
import org.slf4j.Logger;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/**
 * Goety's servant interface, implemented on top of {@link ServantData}, for a boss servant that
 * extends the boss mod's own entity class (so it can't extend Goety's Summoned).
 *
 * <p>Every method Goety's Owned/Summoned classes back with a field is backed by ServantData here.
 * A few things must be overrides on the entity class itself, since an interface can't override a
 * class method; the entity forwards them to the static helpers below:
 * {@link #handleSetTarget}, {@link #isAlliedTo}, {@link #blocksHurt}, {@link #afterHurt}, plus calling
 * {@link #ownedTick()} and {@link #servantTick()} from tick() and the save/read helpers.
 */
public interface BossServant extends IServant, OwnableEntity {
    Logger LOGGER = LogUtils.getLogger();

    /** Save marker: written by versions with the checkHostility fix (see read). */
    String HOSTILITY_FIXED = "GoetyBossServantsHostilityFixed";

    ServantData servantData();

    /** Used when the target is overridden: calls the original Mob.setTarget. */
    void overrideSetTarget(@Nullable LivingEntity target);

    // ----- Ownership -----

    @Nullable
    @Override
    default UUID getOwnerId() {
        return this.servantData().getOwnerId();
    }

    @Override
    default void setOwnerId(@Nullable UUID uuid) {
        this.servantData().setOwnerId(uuid);
    }

    @Override
    default int getOwnerClientId() {
        return this.servantData().getOwnerClientId();
    }

    @Override
    default void setOwnerClientId(int id) {
        this.servantData().setOwnerClientId(id);
    }

    @Nullable
    @Override
    default UUID getOwnerUUID() {
        return this.getOwnerId();
    }

    @Nullable
    @Override
    default LivingEntity getOwner() {
        return this.getTrueOwner();
    }

    // Same lookup as Goety's Owned.
    @Nullable
    @Override
    default LivingEntity getTrueOwner() {
        Mob mob = this.servantData().mob;
        if (mob.hasEffect(GoetyEffects.WILD_RAGE.get())) {
            return null;
        }
        if (!mob.level().isClientSide) {
            UUID uuid = this.getOwnerId();
            return uuid == null ? null : EntityFinder.getLivingEntityByUuiD(mob.level(), uuid);
        }
        int id = this.getOwnerClientId();
        return id <= -1 ? null : mob.level().getEntity(id) instanceof LivingEntity living && living != mob ? living : null;
    }

    @Override
    default boolean isHostile() {
        return this.servantData().getFlag(ServantData.FLAG_HOSTILE);
    }

    /**
     * Goety's default (run when the servant is loaded with no saved "isHostile") also makes it hostile
     * if the servant itself is an Enemy. Boss servants that extend a boss's Monster class always
     * are, so every one stored in a Tesseract or reloaded with the world turned hostile, and
     * hostile servants only go after players. Goety's own servants aren't Monsters; here only a
     * hostile owner makes the servant hostile.
     */
    @Override
    default void checkHostility() {
        Mob mob = this.servantData().mob;
        if (mob.level().isClientSide) {
            return;
        }
        LivingEntity owner = this.getTrueOwner();
        if (owner instanceof Enemy || owner instanceof IOwned owned && owned.isHostile()) {
            this.setHostile(true);
        }
    }

    @Override
    default void setHostile(boolean hostile) {
        this.servantData().setFlag(ServantData.FLAG_HOSTILE, hostile);
    }

    @Override
    default boolean isNatural() {
        return this.servantData().getFlag(ServantData.FLAG_NATURAL);
    }

    @Override
    default void setNatural(boolean natural) {
        this.servantData().setFlag(ServantData.FLAG_NATURAL, natural);
    }

    @Override
    default boolean hasLifespan() {
        return this.servantData().limitedLifespan;
    }

    @Override
    default void setHasLifespan(boolean lifespan) {
        this.servantData().limitedLifespan = lifespan;
    }

    @Override
    default int getLifespan() {
        return this.servantData().limitedLifeTicks;
    }

    @Override
    default void setLifespan(int lifespan) {
        this.servantData().limitedLifeTicks = lifespan;
    }

    @Override
    default int getHasSummonCheck() {
        return this.servantData().hasSummonCheck;
    }

    @Override
    default void setHasSummonCheck(int check) {
        this.servantData().hasSummonCheck = check;
    }

    @Override
    default int getRevivingTime() {
        return this.servantData().revivingTime;
    }

    @Override
    default void setRevivingTime(int tick) {
        this.servantData().revivingTime = tick;
    }

    @Override
    default int getPacifyTick() {
        return this.servantData().pacifyTime;
    }

    @Override
    default void setPacifyTick(int tick) {
        this.servantData().pacifyTime = tick;
    }

    // ----- Servant modes and commands (as Goety's Summoned) -----

    @Override
    default boolean isWandering() {
        return this.servantData().getFlag(ServantData.FLAG_WANDERING);
    }

    @Override
    default void setWandering(boolean wandering) {
        this.servantData().setFlag(ServantData.FLAG_WANDERING, wandering);
    }

    @Override
    default boolean isStaying() {
        return this.servantData().getFlag(ServantData.FLAG_STAYING) && !this.isCommanded() && this.servantData().mob.getControllingPassenger() == null;
    }

    @Override
    default void setStaying(boolean staying) {
        this.servantData().setFlag(ServantData.FLAG_STAYING, staying);
    }

    @Override
    default boolean isUpgraded() {
        return this.servantData().getFlag(ServantData.FLAG_UPGRADED);
    }

    @Override
    default void setUpgraded(boolean upgraded) {
        this.servantData().setFlag(ServantData.FLAG_UPGRADED, upgraded);
    }

    @Override
    default boolean canUpdateMove() {
        return true;
    }

    @Override
    default boolean isCommanded() {
        return this.servantData().commandPos != null;
    }

    @Override
    default void setCommandPos(BlockPos blockPos, boolean removeEntity) {
        if (removeEntity) {
            this.servantData().commandPosEntity = null;
        }
        this.servantData().commandPos = blockPos;
        this.setCommandTick(MathHelper.secondsToTicks(10));
    }

    @Nullable
    @Override
    default BlockPos getCommandPos() {
        return this.servantData().commandPos;
    }

    @Override
    default void setCommandPosEntity(@Nullable LivingEntity living) {
        this.servantData().commandPosEntity = living;
        if (living != null) {
            this.setCommandPos(living.blockPosition(), false);
        }
    }

    @Nullable
    @Override
    default LivingEntity getCommandPosEntity() {
        return this.servantData().commandPosEntity;
    }

    @Override
    default int getCommandTick() {
        return this.servantData().commandTick;
    }

    @Override
    default void setCommandTick(int commandTick) {
        this.servantData().commandTick = commandTick;
    }

    @Nullable
    @Override
    default BlockPos getBoundPos() {
        return this.servantData().boundPos;
    }

    @Override
    default void setBoundPos(@Nullable BlockPos blockPos) {
        this.servantData().boundPos = blockPos;
        this.servantData().boundDim = this.servantData().mob.level().dimension().location().toString();
    }

    @Override
    default String getBoundDim() {
        return this.servantData().boundDim;
    }

    @Override
    default void setBoundDim(String dim) {
        this.servantData().boundDim = dim;
    }

    @Override
    default List<GlobalPos> getPatrolRoute() {
        return this.servantData().patrolList;
    }

    @Override
    default void setPatrolRoute(List<GlobalPos> list) {
        this.servantData().patrolList = list;
    }

    @Override
    default int getPatrolIndex() {
        return this.servantData().patrolIndex;
    }

    @Override
    default void setPatrolIndex(int index) {
        this.servantData().patrolIndex = index;
    }

    @Nullable
    @Override
    default LivingEntity getPriorityTarget() {
        return this.servantData().priorityTarget;
    }

    @Override
    default void setPriorityTarget(@Nullable LivingEntity target) {
        this.overrideSetTarget(target);
        this.servantData().priorityTarget = target;
        if (target != null) {
            this.setPriorityTime(100);
            this.setPriorityPos(target.blockPosition());
        }
    }

    @Override
    default int getPriorityTime() {
        return this.servantData().priorityTime;
    }

    @Override
    default void setPriorityTime(int time) {
        this.servantData().priorityTime = time;
    }

    @Nullable
    @Override
    default BlockPos getPriorityPos() {
        return this.servantData().priorityPos;
    }

    @Override
    default void setPriorityPos(@Nullable BlockPos pos) {
        this.servantData().priorityPos = pos;
    }

    @Override
    default int getNoHealTime() {
        return this.servantData().noHealTime;
    }

    @Override
    default void setNoHealTime(int time) {
        this.servantData().noHealTime = time;
    }

    @Override
    default int getKillChance() {
        return this.servantData().killChance;
    }

    @Override
    default void setKillChance(int killChance) {
        this.servantData().killChance = killChance;
    }

    @Override
    default long getTicketTime() {
        return this.servantData().ticketTime;
    }

    @Override
    default void setTicketTime(long ticketTime) {
        this.servantData().ticketTime = ticketTime;
    }

    // Goety's "are you sure?" warning before a servant can be dismissed with a wand.
    @Override
    default void tryKill(Player player) {
        Mob mob = this.servantData().mob;
        if (this.getKillChance() <= 0) {
            this.setKillChance(60);
            player.displayClientMessage(Component.translatable("info.goety.servant.tryKill", mob.getDisplayName()), true);
        } else {
            mob.hurt(ModDamageSource.getDamageSource(mob.level(), ModDamageSource.DISMISSED), Float.MAX_VALUE);
        }
    }

    // ----- Entity override helpers -----

    /** Body for the entity's setTarget override: Goety's Summoned targeting rules. */
    /** How long ago (ticks) an owner's attack or injury may be to still pull the servant in. */
    int OWNER_FIGHT_MEMORY = 100;

    /**
     * Every tick (server): switch to whatever the owner has just attacked or been attacked by. Goety's
     * OwnerHurtTargetGoal / OwnerHurtByTargetGoal only start when no target goal is running, so once a
     * servant had locked onto something it ignored the owner's next attack until that target died.
     */
    static void followOwnersFight(BossServant servant) {
        ServantData data = servant.servantData();
        if (data == null || data.mob.level().isClientSide) {
            return;
        }
        LivingEntity owner = servant.getTrueOwner();
        if (owner == null) {
            return;
        }
        int attacked = owner.getLastHurtMobTimestamp();
        if (attacked != data.ownerAttackSeen) {
            data.ownerAttackSeen = attacked;
            joinOwnersFight(servant, owner, owner.getLastHurtMob(), attacked);
        }
        int hurt = owner.getLastHurtByMobTimestamp();
        if (hurt != data.ownerHurtSeen) {
            data.ownerHurtSeen = hurt;
            joinOwnersFight(servant, owner, owner.getLastHurtByMob(), hurt);
        }
    }

    private static void joinOwnersFight(BossServant servant, LivingEntity owner, @Nullable LivingEntity enemy, int when) {
        Mob mob = servant.servantData().mob;
        if (enemy == null || enemy == mob.getTarget() || !enemy.isAlive() || owner.tickCount - when > OWNER_FIGHT_MEMORY
                || isFriendly(servant, enemy) || enemy instanceof IOwned owned && owned.getTrueOwner() == owner
                || !mob.canAttack(enemy) || mob.distanceToSqr(enemy) > Mth.square(mob.getAttributeValue(Attributes.FOLLOW_RANGE))) {
            return;
        }
        mob.setTarget(enemy);
    }

    static void handleSetTarget(BossServant servant, @Nullable LivingEntity target) {
        if (target != null && isFriendly(servant, target)) {
            // Boss code sometimes retargets whoever hurt it most; never the owner or an ally.
            return;
        }
        if (servant.getPacifyTick() > 0) {
            servant.overrideSetTarget(null);
        } else if (servant.isGuardingArea() && !servant.isPrioritizing()) {
            if (target == null || target.distanceToSqr(servant.vec3BoundPos()) <= Mth.square(GUARDING_RANGE)) {
                servant.overrideSetTarget(target);
            }
        } else {
            servant.overrideSetTarget(target);
        }
    }

    /** Extra allies on top of the entity's own isAlliedTo: Goety's Owned rules. */
    static boolean isAlliedTo(BossServant servant, Entity entity) {
        LivingEntity trueOwner = servant.getTrueOwner();
        if (trueOwner != null) {
            return trueOwner.isAlliedTo(entity) || entity.isAlliedTo(trueOwner) || entity == trueOwner
                    || entity instanceof IOwned owned && MobUtil.ownerStack(servant, owned)
                    || entity instanceof OwnableEntity ownable && ownable.getOwner() == trueOwner
                    || entity instanceof LivingEntity living && servant.isAllyWith(living);
        }
        return entity instanceof LivingEntity living && servant.isAllyWith(living);
    }

    /** True if the entity's hurt() should refuse this damage (servants of the same master). */
    static boolean blocksHurt(BossServant servant, DamageSource source) {
        return MobsConfig.ServantsMasterImmune.get() && source.getEntity() instanceof IOwned owned
                && !owned.isHostile() && !servant.isHostile() && servant.getTrueOwner() != null && owned.getTrueOwner() == servant.getTrueOwner();
    }

    /** Call after a successful hurt(): pauses Goety's servant healing. */
    static void afterHurt(BossServant servant) {
        servant.setNoHealTime(MathHelper.secondsToTicks(MobsConfig.ServantHealHalt.get()));
    }

    /** True for the owner and anything allied to the servant; for keeping area attacks off them. */
    static boolean isFriendly(BossServant servant, Entity entity) {
        return entity == servant.getTrueOwner() || servant.servantData().mob.isAlliedTo(entity);
    }

    /**
     * Who a servant's untargeted area attacks (orbiting projectiles, pulls) must leave alone: its
     * owner and allies, every player, every pet, and every tame servant (one with an owner and not
     * hostile, as a player's are), and every mob risen for a material monstrosity (always a pet). Wild
     * mobs and the minions of hostile summoners are still fair game.
     * (Goety's servants count as pets as well, so they go by their own rule.)
     */
    static boolean sparedByAreaAttacks(BossServant servant, Entity entity) {
        if (isFriendly(servant, entity) || entity instanceof Player || RisenMobs.isRisen(entity)) {
            return true;
        }
        if (entity instanceof IOwned owned) {
            // Its own and its owner's (by who owns them, so also while the owner is away), and any tame one.
            UUID owner = owned.getOwnerId();
            return owner != null && (owner.equals(servant.servantData().mob.getUUID()) || owner.equals(servant.getOwnerId()) || !owned.isHostile());
        }
        return entity instanceof OwnableEntity pet && pet.getOwnerUUID() != null;
    }

    /**
     * With the "logHealing" debug option on, logs where a health increase on a servant came from
     * (the first few non-Minecraft callers on the stack), at most once a second per servant.
     * Call from the entity's setHealth override before applying the new value.
     */
    static void logHealing(BossServant servant, float newHealth) {
        // setHealth runs inside LivingEntity's constructor, before the servant's fields exist.
        if (servant.servantData() == null || !ServantConfig.DebugHealing.get()) {
            return;
        }
        Mob mob = servant.servantData().mob;
        int last = servant.servantData().lastHealLogTick;
        if (mob.level().isClientSide || newHealth <= mob.getHealth() || last != 0 && mob.tickCount - last < 20) {
            return;
        }
        servant.servantData().lastHealLogTick = mob.tickCount;
        StringBuilder callers = new StringBuilder();
        int shown = 0;
        for (StackTraceElement frame : Thread.currentThread().getStackTrace()) {
            String cls = frame.getClassName();
            if (cls.startsWith("java.") || cls.startsWith("jdk.") || cls.endsWith("BossServant") || cls.endsWith("ServantData")) {
                continue;
            }
            callers.append("\n    at ").append(frame);
            if (!cls.startsWith("net.minecraft.") && ++shown >= 6) {
                break;
            }
        }
        LOGGER.info("[goetybossservants heal debug] {} healed {} -> {}{}", mob.getName().getString(), mob.getHealth(), newHealth, callers);
    }

    static void save(BossServant servant, CompoundTag tag) {
        servant.saveOwnedData(tag);
        servant.saveServantData(tag);
        tag.putBoolean(HOSTILITY_FIXED, true);
    }

    static void read(BossServant servant, CompoundTag tag) {
        servant.readOwnedData(tag);
        if (!tag.getBoolean(HOSTILITY_FIXED)) {
            // Saved before checkHostility was fixed: its "isHostile" may be that bug's doing (an owned
            // boss servant only turns hostile through a hostile owner, which checkHostility sets again).
            servant.setHostile(false);
        }
        servant.readServantData(tag);
    }
}
