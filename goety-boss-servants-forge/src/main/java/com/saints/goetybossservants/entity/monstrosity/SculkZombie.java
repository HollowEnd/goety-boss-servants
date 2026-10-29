package com.saints.goetybossservants.entity.monstrosity;

import com.Polarice3.Goety.common.entities.ally.Summoned;
import com.Polarice3.Goety.common.entities.neutral.Owned;
import com.Polarice3.Goety.utils.BlockFinder;
import com.Polarice3.Goety.utils.MathHelper;
import com.Polarice3.Goety.utils.MobUtil;
import com.saints.goetybossservants.config.ServantConfig;
import com.saints.goetybossservants.registry.ModRegistry;
import net.minecraft.Util;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

/**
 * A mob that died with Sculk Decay, walking again overgrown with sculk: it fights as a zombie
 * servant of the Sculk Monstrosity that decayed it, and dies when its time is up (3 minutes by
 * default), dropping nothing. It looks like the mob it was (its own model, drawn by
 * SculkZombieRenderer with sculk grown over parts of it), keeps its size, and rises out of the
 * ground where it fell.
 */
public class SculkZombie extends Summoned {
    private static final EntityDataAccessor<String> SOURCE = SynchedEntityData.defineId(SculkZombie.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<CompoundTag> LOOK = SynchedEntityData.defineId(SculkZombie.class, EntityDataSerializers.COMPOUND_TAG);
    private static final EntityDataAccessor<Float> WIDTH = SynchedEntityData.defineId(SculkZombie.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> HEIGHT = SynchedEntityData.defineId(SculkZombie.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> RISEN_AT = SynchedEntityData.defineId(SculkZombie.class, EntityDataSerializers.INT);
    /** How long it takes to climb out of the ground. */
    public static final int RISE_TICKS = 30;
    /** The copy of the mob it was, built by its renderer and ticked by SculkPuppets (client only). */
    @Nullable
    public LivingEntity puppet;
    /** Whether its renderer couldn't draw the mob it was, so it looks like a plain zombie (client only). */
    public boolean puppetFailed;
    /** Whether running its copy's client tick failed, so it's drawn unticked (client only). */
    public boolean puppetTickFailed;
    /** What the client does with its copy (see SculkPuppets); nothing on a server. */
    public static PuppetHooks CLIENT = new PuppetHooks() {
    };

    public SculkZombie(EntityType<? extends Owned> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.23)
                .add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.FOLLOW_RANGE, 35.0)
                .add(Attributes.ARMOR, 2.0);
    }

    /** {@code dead} rises as a sculk zombie fighting for {@code monstrosity}. */
    public static void rise(ServerLevel level, MaterialMonstrosity monstrosity, LivingEntity dead) {
        SculkZombie zombie = ModRegistry.SCULK_ZOMBIE.get().create(level);
        if (zombie == null) {
            return;
        }
        CompoundTag look = new CompoundTag();
        try {
            dead.addAdditionalSaveData(look);
        } catch (RuntimeException e) {
            look = new CompoundTag();
        }
        // Its look keeps who it was (the same look each time it is built, see SculkPuppets).
        look.putUUID("SourceUUID", dead.getUUID());
        // A tamed pet still looks tamed (collar and all), though it's the monstrosity's now (see lookOf).
        if (dead instanceof TamableAnimal pet && pet.isTame()) {
            look.putUUID("Owner", Util.NIL_UUID);
        }
        zombie.entityData.set(SOURCE, EntityType.getKey(dead.getType()).toString());
        zombie.entityData.set(LOOK, lookOf(look));
        zombie.entityData.set(WIDTH, dead.getBbWidth());
        zombie.entityData.set(HEIGHT, dead.getBbHeight());
        zombie.entityData.set(RISEN_AT, (int) level.getGameTime() + RISE_TICKS);
        zombie.refreshDimensions();
        placeWhereFell(level, zombie, dead);
        zombie.yBodyRot = dead.yBodyRot;
        zombie.yHeadRot = dead.yHeadRot;
        zombie.setTrueOwner(monstrosity);
        // As tough and as strong as it was, at least a zombie's walk.
        double health = Math.max(6.0, Math.min(200.0, dead.getMaxHealth()));
        AttributeInstance attack = dead.getAttribute(Attributes.ATTACK_DAMAGE);
        double damage = attack != null ? Math.max(2.0, attack.getBaseValue()) : Math.max(2.0, Math.min(12.0, health * 0.15));
        AttributeInstance speed = dead.getAttribute(Attributes.MOVEMENT_SPEED);
        MobUtil.setBaseAttributes(zombie.getAttribute(Attributes.MAX_HEALTH), health);
        MobUtil.setBaseAttributes(zombie.getAttribute(Attributes.ATTACK_DAMAGE), damage);
        MobUtil.setBaseAttributes(zombie.getAttribute(Attributes.MOVEMENT_SPEED), Math.max(0.23, Math.min(0.35, speed != null ? speed.getBaseValue() : 0.23)));
        MobUtil.setBaseAttributes(zombie.getAttribute(Attributes.ARMOR), 2.0 + dead.getArmorValue());
        zombie.setHealth(zombie.getMaxHealth());
        zombie.finalizeSpawn(level, level.getCurrentDifficultyAt(zombie.blockPosition()), MobSpawnType.MOB_SUMMONED, null, null);
        zombie.setLimitedLife(MathHelper.secondsToTicks(ServantConfig.SculkZombieSeconds.get()));
        level.addFreshEntity(zombie);
        zombie.playSound(SoundEvents.SCULK_CATALYST_BLOOM, 2.0f, 0.8f);
        level.sendParticles(ParticleTypes.SCULK_SOUL, zombie.getX(), zombie.getY() + 0.3, zombie.getZ(), 12, dead.getBbWidth() * 0.5, 0.2, dead.getBbWidth() * 0.5, 0.03);
    }

    /** The part of a mob's saved data that its look needs (see RiseData.look). */
    public static CompoundTag lookOf(CompoundTag saved) {
        return RiseData.look(saved);
    }

    /**
     * Puts {@code riser} where {@code dead} fell, facing the same way: a flying mob's from the ground
     * below it, if there's room for it there.
     */
    static void placeWhereFell(ServerLevel level, Entity riser, LivingEntity dead) {
        riser.moveTo(dead.getX(), dead.getY(), dead.getZ(), dead.getYRot(), 0.0f);
        if (!dead.onGround() && !dead.isInWater()) {
            double ground = Ground.floorAt(level, dead.getX(), BlockFinder.moveDownToGround(dead), dead.getZ());
            if (level.noCollision(riser, riser.getBoundingBox().move(0.0, ground - dead.getY(), 0.0))) {
                riser.moveTo(dead.getX(), ground, dead.getZ(), dead.getYRot(), 0.0f);
            }
        }
    }

    /** How many sculk zombies {@code monstrosity} has. */
    public static int count(ServerLevel level, MaterialMonstrosity monstrosity) {
        return level.getEntitiesOfClass(SculkZombie.class, monstrosity.getBoundingBox().inflate(96.0),
                zombie -> zombie.isAlive() && monstrosity.getUUID().equals(zombie.getOwnerId())).size();
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(SOURCE, "minecraft:zombie");
        this.entityData.define(LOOK, new CompoundTag());
        this.entityData.define(WIDTH, 0.0f);
        this.entityData.define(HEIGHT, 0.0f);
        this.entityData.define(RISEN_AT, 0);
    }

    /** What the client does with a sculk zombie's copy of the mob it was. */
    public interface PuppetHooks {
        /** Every client tick. */
        default void tick(SculkZombie zombie) {
        }

        /** Shows entity event {@code id} on its copy; whether that's all it shows (not on the zombie itself too). */
        default boolean event(SculkZombie zombie, byte id) {
            return false;
        }
    }

    /** The type of the mob it was. */
    public EntityType<?> sourceType() {
        ResourceLocation id = ResourceLocation.tryParse(this.entityData.get(SOURCE));
        return id == null ? EntityType.ZOMBIE : BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElse(EntityType.ZOMBIE);
    }

    /** The saved data of the mob it was that its look needs (variant, size, equipment, ...). */
    public CompoundTag look() {
        return this.entityData.get(LOOK);
    }

    /** How far it has risen out of the ground, 0 to 1. */
    public float risen(float partialTicks) {
        float left = this.entityData.get(RISEN_AT) - ((int) this.level().getGameTime() + partialTicks);
        return Math.max(0.0f, Math.min(1.0f, 1.0f - left / RISE_TICKS));
    }

    private boolean rising() {
        return this.risen(0.0f) < 1.0f;
    }

    /** Gives {@code look} (the mob it looks like) the state it shows: crouching, invisible, glowing and so on. */
    public void showStateOn(Entity look) {
        look.getEntityData().set(DATA_SHARED_FLAGS_ID, this.entityData.get(DATA_SHARED_FLAGS_ID));
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.0, false));
        this.goalSelector.addGoal(8, new Summoned.WanderGoal<>(this, 1.0, 10.0f));
        this.goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 3.0f, 1.0f));
        this.goalSelector.addGoal(10, new LookAtPlayerGoal(this, Mob.class, 8.0f));
    }

    @Override
    public @NotNull EntityDimensions getDimensions(@NotNull Pose pose) {
        float width = this.entityData.get(WIDTH), height = this.entityData.get(HEIGHT);
        return width > 0.0f ? EntityDimensions.scalable(width, height) : super.getDimensions(pose);
    }

    @Override
    public void onSyncedDataUpdated(@NotNull EntityDataAccessor<?> accessor) {
        super.onSyncedDataUpdated(accessor);
        if (WIDTH.equals(accessor) || HEIGHT.equals(accessor)) {
            this.refreshDimensions();
        }
        if (SOURCE.equals(accessor) || LOOK.equals(accessor)) {
            this.dropPuppet();
            this.puppetFailed = false;
        }
    }

    // Still while it climbs out of the ground, and nothing hurts it until it is out.
    @Override
    protected boolean isImmobile() {
        return super.isImmobile() || this.rising();
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        return !this.rising() && super.hurt(source, amount);
    }

    @Override
    public boolean doHurtTarget(@NotNull Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit) {
            // The attack swing of mobs that have one (iron golems, ravagers, hoglins, ...).
            this.level().broadcastEntityEvent(this, (byte) 4);
        }
        return hit;
    }

    // Its attack swing is shown on its copy of the mob it was (a mob's own swing: iron golems, ravagers,
    // hoglins...), and its death on both.
    @Override
    public void handleEntityEvent(byte id) {
        if (id != 4 || !CLIENT.event(this, id)) {
            super.handleEntityEvent(id);
        }
        if (id == 3) {
            CLIENT.event(this, id);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            CLIENT.tick(this);
        }
    }

    @Override
    public void onClientRemoval() {
        super.onClientRemoval();
        this.dropPuppet();
    }

    private void dropPuppet() {
        if (this.puppet != null) {
            this.puppet.discard();
            this.puppet = null;
        }
        this.puppetTickFailed = false;
    }

    // It doesn't breathe (one risen under water would drown long before its time is up).
    @Override
    protected int decreaseAirSupply(int air) {
        return air;
    }

    @Override
    public boolean canAttack(@NotNull LivingEntity target) {
        return !(this.getTrueOwner() instanceof MaterialMonstrosity monstrosity && monstrosity.isAlly(target)) && super.canAttack(target);
    }

    // When its time is up it dies outright, and it drops nothing.
    @Override
    public void lifeSpanDamage() {
        // (Goety keeps calling this every tick while it lies dead.)
        if (this.isDeadOrDying()) {
            return;
        }
        this.setHealth(0.0f);
        this.die(this.damageSources().starve());
    }

    @Override
    protected void dropAllDeathLoot(@NotNull DamageSource source) {
    }

    @Override
    public boolean shouldDropExperience() {
        return false;
    }

    // "Sculk Pig", "Sculk Skeleton", ...
    @Override
    protected @NotNull Component getTypeName() {
        return Component.translatable("entity.goetybossservants.sculk_zombie.of", this.sourceType().getDescription());
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.SCULK_BLOCK_SPREAD;
    }

    @Override
    protected SoundEvent getHurtSound(@NotNull DamageSource source) {
        return SoundEvents.SCULK_BLOCK_BREAK;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SCULK_CATALYST_BLOOM;
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("SourceType", this.entityData.get(SOURCE));
        tag.put("SourceLook", this.look());
        tag.putFloat("SourceWidth", this.entityData.get(WIDTH));
        tag.putFloat("SourceHeight", this.entityData.get(HEIGHT));
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("SourceType")) {
            this.entityData.set(SOURCE, tag.getString("SourceType"));
            this.entityData.set(LOOK, lookOf(tag.getCompound("SourceLook")));
            this.entityData.set(WIDTH, tag.getFloat("SourceWidth"));
            this.entityData.set(HEIGHT, tag.getFloat("SourceHeight"));
        }
    }
}
