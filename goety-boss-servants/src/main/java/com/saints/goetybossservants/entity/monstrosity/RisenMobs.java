package com.saints.goetybossservants.entity.monstrosity;

import com.Polarice3.Goety.api.entities.IOwned;
import com.Polarice3.Goety.utils.MathHelper;
import com.mojang.logging.LogUtils;
import com.saints.goetybossservants.GoetyBossServants;
import com.saints.goetybossservants.config.ServantConfig;
import com.saints.goetybossservants.mixin.MobAccessor;
import com.saints.goetybossservants.registry.ModEffects;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.RangedBowAttackGoal;
import net.minecraft.world.entity.ai.goal.RangedCrossbowAttackGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.animal.AbstractGolem;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.entity.animal.horse.TraderLlama;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.regex.Pattern;

/**
 * Mobs that died with Sculk Decay and rose as themselves (see SculkDecay.onDeath): the same mob, with
 * its own AI, attacks and animations, but overgrown with sculk and fighting for the Sculk Monstrosity
 * that decayed it, for as long as a sculk zombie would walk, dropping nothing when it dies. A mob that
 * has no attack of its own, or whose AI is a brain that can't be turned to the monstrosity's side
 * (unless its type is in the rises_as_itself tag), rises as a sculk zombie that looks like it instead.
 *
 * <p>A risen mob carries two entity tags: that it's risen, and its monstrosity's UUID. Who it may fight
 * (mayAttack) and who may fight it (blocksTargeting) are decided here for everything: its own target
 * goals and anything else that picks targets (TargetingConditionsMixin, ModEvents), Goety's ally
 * checks (GoetyMobUtilMixin), the monstrosities' area attacks (MaterialMonstrosity.isAlly) and its
 * damage (MonstrosityEvents). Its time is the SCULK_RISEN effect; when that's gone, it crumbles.
 * Server side, except isRisen and rising, which work on both.
 */
public final class RisenMobs {
    /** The entity tag every risen mob has. */
    public static final String TAG = GoetyBossServants.MOD_ID + ".risen";
    // ...and the one naming its monstrosity.
    private static final String MASTER_TAG = GoetyBossServants.MOD_ID + ".risen_by.";
    // ...and the one on what a risen mob shot (which may outlive it).
    private static final String SHOT_TAG = GoetyBossServants.MOD_ID + ".risen_shot";
    // Each corpse that rose as itself this tick, and what rose (to check against what the corpse dropped).
    private static final Map<Entity, Mob> RISING_FROM = new WeakHashMap<>();
    // While this mod gives a mob its risen effects (nothing else may give them).
    private static boolean giving;
    /** Mobs that always rise as themselves (as long as sculkRiseMode lets any), and mobs that never do. */
    public static final TagKey<EntityType<?>> AS_ITSELF = tag("rises_as_itself");
    public static final TagKey<EntityType<?>> AS_PUPPET = tag("rises_as_puppet");
    // Names of goals that attack (any mod's), besides vanilla's melee and ranged goals.
    private static final Pattern ATTACK_GOAL = Pattern.compile("(?i)attack|melee|bite|charge|strike|slam|stomp|swing|dash|pounce|shoot|spell");
    private static final UUID HEALTH_CAP = UUID.fromString("4b3f2c1d-8e7a-4f60-b5d9-2c1e0a9f8b71");
    // The loaded risen mobs (server), with their monstrosity's UUID.
    private static final Map<Entity, UUID> LOADED = Collections.synchronizedMap(new WeakHashMap<>());
    // When each one's monstrosity was found missing, and when it started dying (game time).
    private static final Map<Entity, Long> MISSING = new WeakHashMap<>();
    private static final Map<Entity, Long> DYING = new WeakHashMap<>();
    // How long one waits for its monstrosity (an unloaded chunk, a teleport), and lies dead at most.
    private static final long MASTER_GRACE = 100L;
    private static final long DEATH_TICKS = 100L;
    private static final double FOLLOW_DISTANCE = 12.0;
    private static final double TELEPORT_DISTANCE = 32.0;
    // Recent: what its monstrosity hit, or was hit by, this long ago (ticks).
    private static final int RECENT = 100;
    private static final Set<EntityType<?>> FAILED = new HashSet<>();
    private static final Logger LOGGER = LogUtils.getLogger();
    /** Mixin hooks that have run on a risen mob (checked by the self-test). */
    public static final Set<String> HOOKS = Collections.synchronizedSet(new HashSet<>());

    private RisenMobs() {
    }

    private static TagKey<EntityType<?>> tag(String name) {
        return TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation(GoetyBossServants.MOD_ID, name));
    }

    static MobEffect risenEffect() {
        return ModEffects.SCULK_RISEN;
    }

    static MobEffect risingEffect() {
        return ModEffects.SCULK_RISING;
    }

    // ----- Who is risen -----------------------------------------------------------------------------

    /** Whether {@code entity} is a risen mob (on a client, by the effect the server shows it). */
    public static boolean isRisen(@Nullable Entity entity) {
        if (entity == null) {
            return false;
        }
        return entity.level().isClientSide ? entity instanceof LivingEntity living && living.hasEffect(risenEffect()) : entity.getTags().contains(TAG);
    }

    /** Whether {@code entity} is climbing out of the ground: it can't move, hurt or be hurt. */
    public static boolean rising(Entity entity) {
        return entity instanceof LivingEntity living && living.hasEffect(risingEffect());
    }

    /** Whether any risen mob is loaded (so the hot hooks can skip everything else). */
    public static boolean anyLoaded() {
        return !LOADED.isEmpty();
    }

    /** The UUID of the monstrosity {@code entity} rose for (null if it isn't risen). */
    @Nullable
    public static UUID masterId(Entity entity) {
        UUID id = LOADED.get(entity);
        if (id != null) {
            return id;
        }
        for (String name : entity.getTags()) {
            if (name.startsWith(MASTER_TAG)) {
                try {
                    return UUID.fromString(name.substring(MASTER_TAG.length()));
                } catch (IllegalArgumentException ignored) {
                    return null;
                }
            }
        }
        return null;
    }

    /** The monstrosity {@code entity} rose for, if it's loaded and alive. */
    @Nullable
    public static MaterialMonstrosity master(Entity entity) {
        UUID id = masterId(entity);
        return id != null && entity.level() instanceof ServerLevel level && level.getEntity(id) instanceof MaterialMonstrosity master && master.isAlive() ? master : null;
    }

    /** A mob joined a level (risen or loaded): if it's risen, it is counted, and fights for its monstrosity. */
    public static void joined(Entity entity) {
        // A risen evoker's vexes fight for its monstrosity too (they'd go for the player otherwise), for as long as it.
        if (entity instanceof Vex vex && !vex.level().isClientSide && vex.getOwner() != null && isRisen(vex.getOwner()) && !isRisen(vex)) {
            UUID master = masterId(vex.getOwner());
            vex.addTag(TAG);
            if (master != null) {
                vex.addTag(MASTER_TAG + master);
            }
            MobEffectInstance life = vex.getOwner().getEffect(risenEffect());
            give(vex, risenEffect(), life == null ? 600 : life.getDuration());
        }
        // What a risen mob shoots is marked, so it breaks no blocks even after the mob is gone.
        if (entity instanceof Projectile projectile && !projectile.level().isClientSide && isRisen(projectile.getOwner())) {
            projectile.addTag(SHOT_TAG);
        }
        if (entity instanceof Mob mob && !mob.level().isClientSide && isRisen(mob)) {
            // (Some mobs turn picking things up back on as they load: pillagers, Goety's illagers.)
            mob.setCanPickUpLoot(false);
            LOADED.put(mob, masterId(mob));
            RisenGoals.install(mob);
        }
    }

    public static void left(Entity entity) {
        // (Only the server's: the client's copy of a mob leaving mustn't take the server's along, as the
        // two share this in singleplayer.)
        if (!entity.level().isClientSide) {
            LOADED.remove(entity);
        }
    }

    /** How many mobs have risen for {@code master} and are still about (its sculk zombies too). */
    public static int count(ServerLevel level, MaterialMonstrosity master) {
        int count = SculkZombie.count(level, master);
        synchronized (LOADED) {
            for (Map.Entry<Entity, UUID> entry : LOADED.entrySet()) {
                // (Not counting what risen mobs summon, a risen evoker's vexes.)
                if (entry.getKey().isAlive() && !(entry.getKey() instanceof Vex) && master.getUUID().equals(entry.getValue())) {
                    ++count;
                }
            }
        }
        return count;
    }

    // ----- Rising -----------------------------------------------------------------------------------

    /**
     * Whether {@code dead} rises as itself rather than as a sculk zombie: when the config allows it,
     * a mob in the rises_as_itself tag, or one not run by a brain that fights by itself (see
     * fightsByItself), unless it's in the rises_as_puppet tag.
     */
    public static boolean risesAsItself(LivingEntity dead) {
        if (!(dead instanceof Mob mob) || ServantConfig.SculkRiseMode.get() == ServantConfig.RiseMode.PUPPET || dead.getType().is(AS_PUPPET)) {
            return false;
        }
        if (dead.getType().is(AS_ITSELF)) {
            return true;
        }
        // (A brain decides whom it fights by itself: piglins, wardens, goats...)
        return mob.getBrain().getMemories().isEmpty() && fightsByItself(mob);
    }

    /**
     * Whether {@code mob} has fighting of its own: a brain, an attack goal, goals that pick targets
     * (some mobs attack from their own code once they have one), or it's a ranged or hostile mob.
     */
    static boolean fightsByItself(Mob mob) {
        return !mob.getBrain().getMemories().isEmpty() || mob instanceof RangedAttackMob || mob instanceof Enemy
                || ((MobAccessor) mob).goetybossservants$getGoalSelector().getAvailableGoals().stream().anyMatch(wrapped -> attacks(wrapped.getGoal()))
                || ((MobAccessor) mob).goetybossservants$getTargetSelector().getAvailableGoals().stream().anyMatch(wrapped -> wrapped.getGoal() instanceof TargetGoal);
    }

    private static boolean attacks(Goal goal) {
        for (Class<?> type = goal.getClass(); type != null && type != Goal.class && type != Object.class; type = type.getSuperclass()) {
            if (type == MeleeAttackGoal.class || type == RangedAttackGoal.class || type == RangedBowAttackGoal.class || type == RangedCrossbowAttackGoal.class
                    || ATTACK_GOAL.matcher(type.getSimpleName()).find()) {
                return true;
            }
        }
        return false;
    }

    /**
     * {@code dead} rises again as itself, where it fell, fighting for {@code master}: a new mob loaded
     * from its saved data (what it is, its stats and gear; see RiseData.revive), at full health. Whether
     * it rose (if anything went wrong, nothing is left of it, and it can rise as a sculk zombie instead).
     */
    public static boolean riseReal(ServerLevel level, MaterialMonstrosity master, Mob dead) {
        Mob mob = null;
        try {
            CompoundTag saved = RiseData.revive(dead.saveWithoutId(new CompoundTag()));
            if (!(dead.getType().create(level) instanceof Mob created)) {
                return false;
            }
            mob = created;
            mob.load(saved);
            // None of the corpse's effects stay on it (what they did to its stats is saved with them).
            for (MobEffectInstance effect : dead.getActiveEffects()) {
                effect.getEffect().removeAttributeModifiers(mob, mob.getAttributes(), effect.getAmplifier());
            }
            // (The dead one keeps its UUID a while yet.)
            mob.setUUID(UUID.randomUUID());
            SculkZombie.placeWhereFell(level, mob, dead);
            mob.setYBodyRot(dead.yBodyRot);
            mob.setYHeadRot(dead.yHeadRot);
            reset(mob, master);
            mob.addTag(TAG);
            mob.addTag(MASTER_TAG + master.getUUID());
            give(mob, risenEffect(), MathHelper.secondsToTicks(ServantConfig.SculkZombieSeconds.get()));
            give(mob, risingEffect(), SculkZombie.RISE_TICKS);
            if (!level.addFreshEntity(mob)) {
                return false;
            }
            RISING_FROM.put(dead, mob);
        } catch (RuntimeException | LinkageError e) {
            if (mob != null) {
                mob.discard();
            }
            if (FAILED.add(dead.getType())) {
                LOGGER.warn("{} couldn't rise as itself from Sculk Decay, so it rises as a sculk zombie", EntityType.getKey(dead.getType()), e);
            }
            return false;
        }
        mob.playSound(SoundEvents.SCULK_CATALYST_BLOOM, 2.0f, 0.8f);
        level.sendParticles(ParticleTypes.SCULK_SOUL, mob.getX(), mob.getY() + 0.3, mob.getZ(), 12, mob.getBbWidth() * 0.5, 0.2, mob.getBbWidth() * 0.5, 0.03);
        return true;
    }

    // Fresh: unhurt (with no more health than the cap), standing, not burning or drowning, its AI on,
    // never picking up or dropping anything, and its monstrosity's if it's one of Goety's.
    private static void reset(Mob mob, MaterialMonstrosity master) {
        AttributeInstance maxHealth = mob.getAttribute(Attributes.MAX_HEALTH);
        double cap = ServantConfig.SculkRisenMaxHealth.get();
        if (maxHealth != null) {
            maxHealth.removeModifier(HEALTH_CAP);
            if (maxHealth.getValue() > cap) {
                maxHealth.addPermanentModifier(new AttributeModifier(HEALTH_CAP, "Sculk risen health cap", cap / maxHealth.getValue() - 1.0, AttributeModifier.Operation.MULTIPLY_TOTAL));
            }
        }
        mob.setHealth(mob.getMaxHealth());
        mob.deathTime = 0;
        mob.hurtTime = 0;
        mob.setNoAi(false);
        mob.setPose(Pose.STANDING);
        mob.setTarget(null);
        mob.clearFire();
        mob.setAirSupply(mob.getMaxAirSupply());
        mob.setTicksFrozen(0);
        mob.setCanPickUpLoot(false);
        mob.setPersistenceRequired();
        // (A zombie calls wild zombies to help it.)
        AttributeInstance reinforcements = mob.getAttribute(Attributes.SPAWN_REINFORCEMENTS_CHANCE);
        if (reinforcements != null) {
            reinforcements.removeModifiers();
            reinforcements.setBaseValue(0.0);
        }
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            mob.setDropChance(slot, 0.0f);
        }
        if (mob instanceof TamableAnimal pet) {
            pet.setOrderedToSit(false);
            pet.setInSittingPose(false);
        }
        if (mob instanceof AbstractPiglin piglin) {
            piglin.setImmuneToZombification(true);
        }
        if (mob instanceof Hoglin hoglin) {
            hoglin.setImmuneToZombification(true);
        }
        // A fox drops what's in its mouth however it dies (the corpse did); a raider stays out of raids; a
        // trader llama stays as long as it lives.
        if (mob instanceof Fox) {
            mob.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        }
        if (mob instanceof Raider raider) {
            raider.setCanJoinRaid(false);
            raider.setCurrentRaid(null);
        }
        if (mob instanceof TraderLlama llama) {
            llama.setDespawnDelay(Integer.MAX_VALUE);
        }
        if (mob instanceof IOwned owned) {
            owned.setTrueOwner(master);
            owned.setHostile(master.isHostile());
        }
    }

    /** Whether {@code entity} is something a risen mob shot. */
    public static boolean isRisenShot(@Nullable Entity entity) {
        return entity != null && entity.getTags().contains(SHOT_TAG);
    }

    /**
     * {@code corpse} has dropped its loot (it died after rising as itself): gear it dropped isn't kept on
     * what rose from it either (it rose with a copy of everything it wore, before knowing what would drop).
     */
    public static void afterCorpseDrops(Entity corpse) {
        Mob risen = RISING_FROM.remove(corpse);
        if (risen != null && corpse instanceof LivingEntity dead) {
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                if (dead.getItemBySlot(slot).isEmpty()) {
                    risen.setItemSlot(slot, ItemStack.EMPTY);
                }
            }
        }
    }

    // Gives it the effect even if it takes no effects (some modded mobs don't).
    private static void give(Mob mob, MobEffect effect, int ticks) {
        MobEffectInstance instance = new MobEffectInstance(effect, ticks, 0, false, false, false);
        giving = true;
        try {
            if (!mob.addEffect(instance) || !mob.hasEffect(effect)) {
                mob.getActiveEffectsMap().put(effect, instance);
            }
        } finally {
            giving = false;
        }
    }

    /** Whether this mod is giving a mob its risen effects now: nothing else may (a risen creeper's cloud would pass them on). */
    public static boolean giving() {
        return giving;
    }

    // ----- Living -----------------------------------------------------------------------------------

    /**
     * Every server tick of a risen mob: it crumbles when its time is up (or something took it away) or
     * its monstrosity is gone; it stays still while it rises; and otherwise it keeps to its monstrosity
     * between fights (and a brain mob is told whom to fight).
     */
    public static void tick(Mob mob) {
        long now = mob.level().getGameTime();
        if (mob.isDeadOrDying()) {
            unsplit(mob);
            // (In case its death never ends by itself.)
            Long since = DYING.putIfAbsent(mob, now);
            if (since != null && now - since > DEATH_TICKS) {
                mob.discard();
            }
            return;
        }
        if (!mob.hasEffect(risenEffect())) {
            crumble(mob);
            return;
        }
        MaterialMonstrosity master = master(mob);
        if (master == null) {
            Long since = MISSING.putIfAbsent(mob, now);
            if (since != null && now - since > MASTER_GRACE) {
                crumble(mob);
            }
            return;
        }
        MISSING.remove(mob);
        boolean brain = !mob.getBrain().getMemories().isEmpty();
        if (rising(mob)) {
            mob.setTarget(null);
            mob.getNavigation().stop();
            mob.setDeltaMovement(0.0, Math.min(0.0, mob.getDeltaMovement().y), 0.0);
            if (brain) {
                forget(mob.getBrain(), MemoryModuleType.ATTACK_TARGET);
            }
            return;
        }
        LivingEntity fighting = mob.getTarget();
        if (brain) {
            fighting = steerBrain(mob, master);
        } else if (fighting != null && !mayAttack(mob, fighting)) {
            mob.setTarget(null);
            fighting = null;
        }
        // Off to its monstrosity's fight when that's further than it can see (it takes it up once
        // close enough, see RisenGoals), and otherwise back to its monstrosity.
        LivingEntity far = fighting == null ? wanted(mob, master, Double.MAX_VALUE) : null;
        // A sitting risen pet gets up when there's something to do: a fight, or its monstrosity to reach.
        // Its own sitting is otherwise left alone: some mobs sit on the way into another state (a Saints
        // Dragons dragon sits down to fall asleep), and made to stand halfway, they stall in between.
        if (mob instanceof TamableAnimal pet && (pet.isOrderedToSit() || pet.isInSittingPose())
                && (fighting != null || far != null || mob.distanceToSqr(master) > FOLLOW_DISTANCE * FOLLOW_DISTANCE)) {
            pet.setOrderedToSit(false);
            pet.setInSittingPose(false);
        }
        if (fighting == null) {
            if (far == null) {
                followMaster(mob, master);
            } else if (mob.tickCount % 10 == 0) {
                mob.getNavigation().moveTo(far, 1.0);
            }
        }
    }

    // A brain mob forgets targets it may not fight, and takes its monstrosity's when it has none.
    // What it's fighting now.
    @Nullable
    private static LivingEntity steerBrain(Mob mob, MaterialMonstrosity master) {
        Brain<?> brain = mob.getBrain();
        LivingEntity target = brain.hasMemoryValue(MemoryModuleType.ATTACK_TARGET) ? brain.getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null) : null;
        if (target != null && !mayAttack(mob, target)) {
            forget(brain, MemoryModuleType.ATTACK_TARGET);
            target = null;
        }
        if (brain.hasMemoryValue(MemoryModuleType.ANGRY_AT) && mob.level() instanceof ServerLevel level
                && !(brain.getMemory(MemoryModuleType.ANGRY_AT).map(level::getEntity).orElse(null) instanceof LivingEntity angry && mayAttack(mob, angry))) {
            forget(brain, MemoryModuleType.ANGRY_AT);
        }
        if (target == null && brain.checkMemory(MemoryModuleType.ATTACK_TARGET, MemoryStatus.REGISTERED)) {
            target = wanted(mob, master, Double.MAX_VALUE);
            if (target != null) {
                brain.setMemory(MemoryModuleType.ATTACK_TARGET, target);
            }
        }
        return target;
    }

    private static void forget(Brain<?> brain, MemoryModuleType<?> memory) {
        if (brain.checkMemory(memory, MemoryStatus.REGISTERED)) {
            brain.eraseMemory(memory);
        }
    }

    // Between fights it keeps near its monstrosity, catching up by teleport if left far behind.
    private static void followMaster(Mob mob, MaterialMonstrosity master) {
        double distance = mob.distanceToSqr(master);
        if (distance > TELEPORT_DISTANCE * TELEPORT_DISTANCE) {
            if (mob.level().noCollision(mob, mob.getBoundingBox().move(master.position().subtract(mob.position())))) {
                mob.teleportTo(master.getX(), master.getY(), master.getZ());
                mob.getNavigation().stop();
            }
        } else if (distance > FOLLOW_DISTANCE * FOLLOW_DISTANCE && mob.tickCount % 10 == 0) {
            mob.getNavigation().moveTo(master, 1.0);
        }
    }

    /**
     * What {@code mob} should fight for {@code master} now, within {@code range}: what its
     * monstrosity is fighting, or recently hit or was hit by, if it may fight that.
     */
    @Nullable
    static LivingEntity wanted(Mob mob, MaterialMonstrosity master, double range) {
        List<LivingEntity> candidates = new ArrayList<>(3);
        candidates.add(master.getTarget());
        if (master.tickCount - master.getLastHurtByMobTimestamp() < RECENT) {
            candidates.add(master.getLastHurtByMob());
        }
        if (master.tickCount - master.getLastHurtMobTimestamp() < RECENT) {
            candidates.add(master.getLastHurtMob());
        }
        for (LivingEntity candidate : candidates) {
            if (candidate != null && candidate.isAlive() && candidate.level() == mob.level() && mob.distanceToSqr(candidate) <= range * range && mayAttack(mob, candidate)) {
                return candidate;
            }
        }
        return null;
    }

    // A slime splits into new (wild) slimes as it goes, unless it's the smallest.
    private static void unsplit(Mob mob) {
        if (mob instanceof Slime slime && slime.getSize() > 1) {
            slime.setSize(1, false);
        }
    }

    /** Its time is up: it dies (its own death, so its death animation plays), dropping nothing. */
    public static void crumble(Mob mob) {
        if (mob.isDeadOrDying()) {
            return;
        }
        unsplit(mob);
        if (mob.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.SCULK_SOUL, mob.getX(), mob.getY() + mob.getBbHeight() * 0.5, mob.getZ(), 12,
                    mob.getBbWidth() * 0.5, mob.getBbHeight() * 0.3, mob.getBbWidth() * 0.5, 0.03);
        }
        mob.setHealth(0.0f);
        mob.die(mob.damageSources().starve());
    }

    // ----- Sides ------------------------------------------------------------------------------------

    /**
     * Whether risen {@code mob} may fight {@code target}: never its monstrosity's side, nor anything
     * while it rises; otherwise what its monstrosity is fighting, what hurt it, or one of its
     * monstrosity's foes (see MaterialMonstrosity.isFoe: never bystanders, players or pets).
     */
    public static boolean mayAttack(Mob mob, LivingEntity target) {
        MaterialMonstrosity master = master(mob);
        if (master == null || target == mob || rising(mob) || master.isAlly(target)) {
            return false;
        }
        return target == master.getTarget() || target == mob.getLastHurtByMob() || master.isFoe(target);
    }

    /**
     * Whether {@code a} and {@code b} are on the same side because one is a risen mob: the other is
     * its monstrosity, or on its monstrosity's side (MaterialMonstrosity.isAlly, which counts the
     * mobs risen for it and its allies). False when neither is risen.
     */
    public static boolean sameSide(@Nullable Entity a, @Nullable Entity b) {
        if (a == null || b == null || a.level().isClientSide) {
            return false;
        }
        Entity risen = isRisen(a) ? a : isRisen(b) ? b : null;
        if (risen == null) {
            return false;
        }
        Entity other = risen == a ? b : a;
        MaterialMonstrosity master = master(risen);
        return master != null && (other == master || master.isAlly(other));
    }

    /**
     * Whether {@code attacker} may not pick {@code target} to fight: a risen mob anything it may not
     * fight, and anything on a risen mob's side (golems included, as the monstrosity is never hostile),
     * or anything at all, that mob while it rises.
     */
    public static boolean blocksTargeting(LivingEntity attacker, @Nullable LivingEntity target) {
        if (target == null) {
            return false;
        }
        if (attacker instanceof Mob mob && isRisen(mob)) {
            return !mayAttack(mob, target);
        }
        if (isRisen(target)) {
            MaterialMonstrosity master = master(target);
            return rising(target) || master != null && (master.isAlly(attacker) || attacker instanceof AbstractGolem && !master.isHostile());
        }
        return false;
    }

    /**
     * Whether damage from {@code source} to {@code victim} is stopped: nothing hurts a rising mob, and a
     * risen mob (or what it shot or set off) only hurts what it may fight, sparing its side, players, pets
     * and bystanders unless that's what it's fighting.
     */
    public static boolean blocksDamage(DamageSource source, LivingEntity victim) {
        if (victim.level().isClientSide || !anyLoaded()) {
            return false;
        }
        if (isRisen(victim) && rising(victim) && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return true;
        }
        return source.getEntity() instanceof Mob attacker && attacker != victim && isRisen(attacker) && spares(attacker, victim);
    }

    /** Whether risen {@code mob}'s attacks (its explosions too) must leave {@code victim} alone. */
    public static boolean spares(Mob mob, Entity victim) {
        MaterialMonstrosity master = master(mob);
        if (master == null || rising(mob) || victim == master || master.isAlly(victim)) {
            return true;
        }
        return victim instanceof LivingEntity living && living != mob.getTarget() && living != master.getTarget() && !master.canHitWithArea(living);
    }
}
