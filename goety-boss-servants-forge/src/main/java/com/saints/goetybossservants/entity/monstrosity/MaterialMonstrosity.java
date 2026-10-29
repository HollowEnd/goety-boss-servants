package com.saints.goetybossservants.entity.monstrosity;

import com.Polarice3.Goety.client.particles.ModParticleTypes;
import com.Polarice3.Goety.common.entities.ally.golem.RedstoneMonstrosity;
import com.Polarice3.Goety.common.entities.neutral.Owned;
import com.Polarice3.Goety.common.entities.util.CameraShake;
import com.Polarice3.Goety.init.ModSounds;
import com.Polarice3.Goety.utils.MathHelper;
import com.Polarice3.Goety.utils.MobUtil;
import com.saints.goetybossservants.config.ServantConfig;
import com.saints.goetybossservants.item.MaterialMonstrosityHeadItem;
import com.saints.goetybossservants.mixin.RedstoneMonstrosityAccessor;
import com.saints.goetybossservants.registry.ModRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.Tags;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

/**
 * Goety's Redstone Monstrosity in another material. Everything (animations, melee smash, scatter
 * bomb belch, riding, sitting, wand commands, repairs, head on death) is the Redstone Monstrosity's;
 * only the summon attack changes: at the moment it slams the ground, instead of raising Redstone
 * Cubes it does its material's special (see MonstrositySpecials).
 *
 * <p>The Lapis one can also be handed an enchanted book by its owner; while it holds one, its buff
 * is that book's enchantment instead of a random one (see LapisBuffs).
 *
 * <p>The Sculk one has a second melee attack, taking turns with the smash: it raises only its
 * tentacle arm and slams it down, leaving a line of sculk mire. Its belch coughs up sculk globs
 * (SculkGlob) instead of scatter bombs, and everything it hurts decays (see SculkDecay).
 */
public class MaterialMonstrosity extends RedstoneMonstrosity {
    private static final EntityDataAccessor<ItemStack> HELD_BOOK = SynchedEntityData.defineId(MaterialMonstrosity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Boolean> MINION = SynchedEntityData.defineId(MaterialMonstrosity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> TENTACLE_SLAM = SynchedEntityData.defineId(MaterialMonstrosity.class, EntityDataSerializers.BOOLEAN);
    /** summonTick when the summon attack's slam lands (Goety spawns its slam particles then). */
    public static final int SLAM_TICK = MathHelper.secondsToTicks(SUMMON_SECONDS_TIME - 1.0f);
    /** When the Sculk one's tentacle slam lands, and when it is over (ticks after it starts; see SculkAnimations). */
    public static final int TENTACLE_IMPACT = 22;
    public static final int TENTACLE_END = 45;
    /** How far ahead of its tentacle arm's shoulder the slam reaches, and how wide it is, in blocks. */
    private static final double TENTACLE_REACH = 7.0;
    private static final double TENTACLE_HALF_WIDTH = 1.2;
    /** How far to its right its tentacle arm is. */
    private static final double TENTACLE_SIDE = 2.4;

    @Nullable
    private MonstrosityMaterial material;
    /** Ticks of diamond rain left (Diamond only). */
    public int rainTicks;
    /** The Netherite one's special while it plays out. */
    @Nullable
    NetheriteCataclysm cataclysm;
    /** The Sculk one's tentacle slam (client), and how far into it it is (server, 0 when not slamming). */
    public final AnimationState tentacleSlamState = new AnimationState();
    private int tentacleSlamTick;
    private boolean tentacleNext;

    public MaterialMonstrosity(EntityType<? extends Owned> type, Level level) {
        super(type, level);
        this.setConfigurableAttributes();
        this.setHealth(this.getMaxHealth());
    }

    /** Goety's attributes with this material's default stats; the config is applied per entity. */
    public static AttributeSupplier.Builder createAttributes(MonstrosityMaterial material) {
        return RedstoneMonstrosity.setCustomAttributes()
                .add(Attributes.MAX_HEALTH, material.defaultHealth)
                .add(Attributes.ATTACK_DAMAGE, material.defaultDamage)
                .add(Attributes.ARMOR, material.defaultArmor)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    /** Which material this is, from its entity type ("copper_monstrosity" and so on). */
    public MonstrosityMaterial material() {
        if (this.material == null) {
            String path = EntityType.getKey(this.getType()).getPath();
            this.material = MonstrosityMaterial.byId(path.substring(0, path.length() - "_monstrosity".length()));
        }
        return this.material;
    }

    @Override
    public void setConfigurableAttributes() {
        ServantConfig.MaterialStats stats = ServantConfig.MaterialMonstrosityStats.get(this.material());
        MobUtil.setBaseAttributes(this.getAttribute(Attributes.MAX_HEALTH), stats.health().get() / (this.isMinion() ? 2.0 : 1.0));
        MobUtil.setBaseAttributes(this.getAttribute(Attributes.ATTACK_DAMAGE), stats.damage().get());
        MobUtil.setBaseAttributes(this.getAttribute(Attributes.ARMOR), stats.armor().get());
        MobUtil.setBaseAttributes(this.getAttribute(Attributes.FOLLOW_RANGE), ServantConfig.MaterialMonstrosityFollowRange.get());
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(HELD_BOOK, ItemStack.EMPTY);
        this.entityData.define(MINION, false);
        this.entityData.define(TENTACLE_SLAM, false);
    }

    @Override
    @Nullable
    public SpawnGroupData finalizeSpawn(@NotNull ServerLevelAccessor level, @NotNull DifficultyInstance difficulty, @NotNull MobSpawnType reason, @Nullable SpawnGroupData spawnData, @Nullable CompoundTag tag) {
        this.setConfigurableAttributes();
        this.setHealth(this.getMaxHealth());
        return super.finalizeSpawn(level, difficulty, reason, spawnData, tag);
    }

    /**
     * Only ever a pet. Goety's Redstone Monstrosity turns into its hostile version when hostile;
     * these have no hostile version.
     */
    @Override
    public boolean isHostile() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide && this.isAlive()) {
            RedstoneMonstrosityAccessor accessor = (RedstoneMonstrosityAccessor) this;
            // Goety raises its Redstone Cubes a second after this while summonCount is set; clearing
            // it here means none ever come, and the material's own special takes their place.
            if (this.summonTick == SLAM_TICK && accessor.goetybossservants$getSummonCount() != 0) {
                accessor.goetybossservants$setSummonCount(0);
                MonstrositySpecials.perform(this);
            }
            if (this.rainTicks > 0) {
                --this.rainTicks;
                MonstrositySpecials.diamondRain(this);
            }
            if (this.cataclysm != null && !this.cataclysm.tick((ServerLevel) this.level())) {
                this.cataclysm = null;
            }
            if (this.tentacleSlamTick > 0) {
                this.tickTentacleSlam((ServerLevel) this.level());
            }
        }
    }

    // Goety's summon attack, only started when the material's special would hit something; the
    // Sculk one belches sculk globs.
    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.removeAllGoals(goal -> goal.getClass() == SummonGoal.class);
        this.goalSelector.addGoal(1, new SummonGoal(this) {
            @Override
            public boolean canUse() {
                return super.canUse() && MonstrositySpecials.worthUsing(MaterialMonstrosity.this);
            }
        });
        if (this.material() == MonstrosityMaterial.SCULK) {
            this.goalSelector.removeAllGoals(goal -> goal.getClass() == BelchGoal.class);
            this.goalSelector.addGoal(3, new SculkBelchGoal());
        }
    }

    // The Netherite one's leap replays the summon attack from the start, so its slam lands with it.
    @Override
    public void handleEntityEvent(byte id) {
        if (id == NetheriteCataclysm.REPLAY_SLAM) {
            this.summonAnimationState.start(this.tickCount);
            this.stopMostAnimation(this.summonAnimationState);
        } else {
            super.handleEntityEvent(id);
        }
    }

    // ----- Sculk tentacle slam and belch ---------------------------------------------------------

    /** Whether the Sculk one is slamming its tentacle down. */
    public boolean isTentacleSlamming() {
        return this.entityData.get(TENTACLE_SLAM);
    }

    // Counts as busy (Goety's "belching") while it slams its tentacle, so no other attack starts, it
    // stands still, and Goety leaves its animation alone.
    @Override
    public boolean isBelching() {
        return super.isBelching() || this.isTentacleSlamming();
    }

    // The Sculk one takes turns: its tentacle slam, then Goety's smash. (Goety's attack goal keeps
    // asking all through the slam; it would queue up a smash for straight after.)
    @Override
    public boolean doHurtTarget(@NotNull Entity target) {
        if (this.isTentacleSlamming()) {
            return false;
        }
        if (this.material() == MonstrosityMaterial.SCULK && !this.level().isClientSide && !this.isMeleeAttacking()) {
            this.tentacleNext = !this.tentacleNext;
            if (this.tentacleNext) {
                this.startTentacleSlam();
                return true;
            }
        }
        return super.doHurtTarget(target);
    }

    /** Starts the tentacle slam (also used by the self-test). */
    public void startTentacleSlam() {
        this.tentacleSlamTick = 1;
        this.entityData.set(TENTACLE_SLAM, true);
        // An animation of Goety's it doesn't know, so when the slam ends its idle starts over (and stops the slam's).
        this.setAnimationState(0);
        this.getNavigation().stop();
        this.playSound(ModSounds.REDSTONE_MONSTROSITY_GROWL.get(), 2.0f, 0.6f);
        this.playSound(SoundEvents.SCULK_SHRIEKER_SHRIEK, 2.0f, 0.7f);
    }

    private void tickTentacleSlam(ServerLevel level) {
        LivingEntity target = this.getTarget();
        if (target != null && this.tentacleSlamTick < TENTACLE_IMPACT - 6) {
            this.turnTentacleTo(target);
        } else {
            // Then it holds still where its tentacle comes down (Goety's attack goal would turn its head,
            // and its body after, to face its target).
            this.face(this.getYRot());
        }
        this.getNavigation().stop();
        if (this.tentacleSlamTick == TENTACLE_IMPACT) {
            this.tentacleImpact(level);
        }
        if (++this.tentacleSlamTick > TENTACLE_END) {
            this.tentacleSlamTick = 0;
            this.entityData.set(TENTACLE_SLAM, false);
            ((RedstoneMonstrosityAccessor) this).goetybossservants$setPostAttackCool(MathHelper.secondsToTicks(0.5f));
        }
    }

    /** Where its tentacle lands: from beside its shoulder (on its right), on the ground ahead. */
    public Vec3 tentacleStart() {
        return this.position().add(MobUtil.getHorizontalRightLookAngle(this).scale(TENTACLE_SIDE)).add(this.getHorizontalLookAngle().scale(1.5));
    }

    // Turns it (a little at a time) so its tentacle, off to its right, comes down on {@code target}:
    // facing a little to the left of it, more the closer it is (the target then lies TENTACLE_SIDE to
    // its right, as the sine of the angle between them times the distance).
    private void turnTentacleTo(LivingEntity target) {
        double dx = target.getX() - this.getX(), dz = target.getZ() - this.getZ();
        double distance = Math.sqrt(dx * dx + dz * dz);
        float toTarget = (float) (Mth.atan2(-dx, dz) * Mth.RAD_TO_DEG);
        float aside = distance > TENTACLE_SIDE ? (float) (Math.asin(TENTACLE_SIDE / distance) * Mth.RAD_TO_DEG) : 60.0f;
        this.face(Mth.approachDegrees(this.getYRot(), toTarget - aside, 12.0f));
    }

    // Faces {@code yaw}, head and body together.
    private void face(float yaw) {
        this.setYRot(yaw);
        this.setYBodyRot(yaw);
        this.setYHeadRot(yaw);
    }

    // The tentacle lands along the ground ahead of its shoulder (turned a little toward its target):
    // everything along it is hurt and thrown up, and sculk mire is left there.
    private void tentacleImpact(ServerLevel level) {
        Vec3 start = this.tentacleStart();
        Vec3 dir = this.getHorizontalLookAngle();
        LivingEntity target = this.getTarget();
        if (target != null) {
            Vec3 to = target.position().subtract(start).multiply(1.0, 0.0, 1.0);
            if (to.lengthSqr() > 1.0 && to.normalize().dot(dir) > 0.8) {
                dir = to.normalize();
            }
        }
        float yaw = (float) (Mth.atan2(-dir.x, dir.z) * Mth.RAD_TO_DEG);
        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE);
        AABB area = new AABB(start, start.add(dir.scale(TENTACLE_REACH))).inflate(TENTACLE_HALF_WIDTH + 1.0, 3.0, TENTACLE_HALF_WIDTH + 1.0);
        for (LivingEntity hit : level.getEntitiesOfClass(LivingEntity.class, area, this::canHitWithArea)) {
            double dx = hit.getX() - start.x, dz = hit.getZ() - start.z;
            double along = dx * dir.x + dz * dir.z;
            double across = Math.abs(dx * dir.z - dz * dir.x);
            if (along >= -1.0 && along <= TENTACLE_REACH + 0.5 && across <= TENTACLE_HALF_WIDTH + hit.getBbWidth() / 2.0
                    && hit.hurt(this.getServantAttack(), damage)) {
                MonstrositySpecials.launch(hit, start, 0.2, 0.5);
            }
        }
        SculkMire.line(level, this, start, yaw, TENTACLE_REACH);
        BlockParticleOption sculk = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SCULK.defaultBlockState());
        for (int i = 0; i <= TENTACLE_REACH; ++i) {
            Vec3 at = start.add(dir.scale(i));
            level.sendParticles(sculk, at.x, at.y + 0.2, at.z, 10, 0.6, 0.1, 0.6, 0.15);
            level.sendParticles(ParticleTypes.SCULK_CHARGE_POP, at.x, at.y + 0.3, at.z, 3, 0.5, 0.1, 0.5, 0.02);
        }
        CameraShake.cameraShake(level, start, 15.0f, 0.2f, 0, 10);
        this.playSound(ModSounds.REDSTONE_MONSTROSITY_SMASH.get(), 2.0f, 0.5f);
        this.playSound(SoundEvents.WARDEN_ATTACK_IMPACT, 2.0f, 0.5f);
    }

    @Override
    public @NotNull List<AnimationState> getAllAnimations() {
        List<AnimationState> states = super.getAllAnimations();
        // (Null while Goety's constructor runs.)
        if (this.tentacleSlamState != null) {
            states.add(this.tentacleSlamState);
        }
        return states;
    }

    /**
     * Goety's belch (its goal's timing, aim and cooldown, which Goety keeps private), coughing up
     * sculk globs instead of scatter bombs.
     */
    private class SculkBelchGoal extends Goal {
        private int attackTime;
        private int seeTime;

        SculkBelchGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        private MaterialMonstrosity mob() {
            return MaterialMonstrosity.this;
        }

        @Override
        public boolean canUse() {
            LivingEntity target = this.mob().getTarget();
            return this.mob().belchCool <= 0 && target != null && target.isAlive() && !this.mob().targetClose(target) && this.mob().distanceTo(target) <= 13.0f
                    && !this.mob().isMeleeAttacking() && !this.mob().isSummoning() && !this.mob().isTentacleSlamming();
        }

        @Override
        public boolean canContinueToUse() {
            return this.canUse() || !this.mob().isMeleeAttacking() && !this.mob().isSummoning() && !this.mob().isTentacleSlamming()
                    && this.attackTime > 0 && this.mob().belchCool <= 0;
        }

        @Override
        public void stop() {
            this.mob().setBelching(false);
            this.seeTime = 0;
            this.attackTime = 0;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            MaterialMonstrosity mob = this.mob();
            if (mob.isMeleeAttacking() || mob.isSummoning() || mob.isTentacleSlamming()) {
                return;
            }
            LivingEntity target = mob.getTarget();
            if (target != null) {
                this.seeTime = mob.getSensing().hasLineOfSight(target) ? this.seeTime + 1 : 0;
                if (mob.distanceToSqr(target) <= 32.0 * 32.0 && this.seeTime >= 5) {
                    mob.getNavigation().stop();
                } else {
                    mob.getNavigation().moveTo(target, 1.0);
                }
                MobUtil.instaLook(mob, target);
            }
            ++this.attackTime;
            if (this.attackTime == 1) {
                mob.playSound(ModSounds.REDSTONE_MONSTROSITY_ARM.get(), mob.getSoundVolume() * 0.7f, mob.getVoicePitch());
                mob.setBelching(true);
                mob.setAnimationState(BELCH);
            } else if (this.attackTime > 5 && this.attackTime < 17) {
                mob.makeBigGlow();
            } else if (this.attackTime == 17) {
                mob.belch();
            } else if (this.attackTime >= 37) {
                mob.setBelching(false);
                ((RedstoneMonstrosityAccessor) mob).goetybossservants$setPostAttackCool(MathHelper.secondsToTicks(0.5f));
                this.attackTime = 0;
                mob.belchCool = MathHelper.secondsToTicks(8);
            }
        }
    }

    /** The Sculk one's belch lands: eight sculk globs, lobbed as Goety's scatter bombs are (also used by the self-test). */
    public void belch() {
        Vec3 aim = this.position().add(this.getLookAngle().scale(10.0));
        for (int i = 0; i < 8; ++i) {
            this.level().addFreshEntity(this.glob(aim));
        }
        this.playSound(ModSounds.REDSTONE_MONSTROSITY_BELCH.get(), this.getSoundVolume(), 0.7f);
        this.playSound(ModSounds.REDSTONE_MONSTROSITY_GROWL.get(), this.getSoundVolume() - 1.5f, 0.9f);
        this.playSound(SoundEvents.SCULK_SHRIEKER_SHRIEK, 2.0f, 0.5f);
    }

    // Goety's aim: lobbed toward a point ahead of it, slower for a closer target, scattered.
    private SculkGlob glob(Vec3 aim) {
        SculkGlob glob = SculkGlob.create(this);
        double dx = aim.x - this.getX(), dz = aim.z - this.getZ();
        double dy = aim.y - (this.getEyeY() - 0.1);
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        float velocity = 1.0f;
        LivingEntity target = this.getTarget();
        if (target != null && target.distanceTo(this) <= 13.0f) {
            velocity = Math.max(0.25f, target.distanceTo(this) / 13.0f);
        }
        Vec3 motion = new Vec3(dx, dy + horizontal, dz).normalize()
                .add(this.random.triangle(0.0, 0.0172275 * 30.0), this.random.triangle(0.0, 0.0861375), this.random.triangle(0.0, 0.0172275 * 30.0))
                .scale(velocity);
        glob.setDeltaMovement(motion);
        glob.setYRot((float) (Mth.atan2(motion.x, motion.z) * Mth.RAD_TO_DEG));
        glob.setXRot((float) (Mth.atan2(motion.y, motion.horizontalDistance()) * Mth.RAD_TO_DEG));
        glob.yRotO = glob.getYRot();
        glob.xRotO = glob.getXRot();
        return glob;
    }

    // ----- Netherite minions ---------------------------------------------------------------------

    /** Summoned by the Netherite Monstrosity: half the size and health, gone when its time is up, and drops nothing. */
    public boolean isMinion() {
        return this.entityData.get(MINION);
    }

    public void setMinion(boolean minion) {
        this.entityData.set(MINION, minion);
        this.refreshDimensions();
        this.setConfigurableAttributes();
        this.setHealth(this.getMaxHealth());
    }

    @Override
    public @NotNull EntityDimensions getDimensions(@NotNull Pose pose) {
        return this.isMinion() ? super.getDimensions(pose).scale(0.5f) : super.getDimensions(pose);
    }

    @Override
    public void onSyncedDataUpdated(@NotNull EntityDataAccessor<?> accessor) {
        super.onSyncedDataUpdated(accessor);
        if (MINION.equals(accessor)) {
            this.refreshDimensions();
        }
        if (TENTACLE_SLAM.equals(accessor) && this.level().isClientSide && this.isTentacleSlamming()) {
            this.tentacleSlamState.start(this.tickCount);
            this.stopMostAnimation(this.tentacleSlamState);
        }
    }

    // When its time is up it dies outright (Goety takes 1 health a second until it does).
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
        if (!this.isMinion()) {
            super.dropAllDeathLoot(source);
        }
    }

    @Override
    public boolean shouldDropExperience() {
        return !this.isMinion() && super.shouldDropExperience();
    }

    /** Starts the summon attack right away (cooldown cleared), as if it had picked it itself. Used by the self-test. */
    public void forceSpecial() {
        ((RedstoneMonstrosityAccessor) this).goetybossservants$setSummonCool(0);
        this.summonTick = MathHelper.secondsToTicks(SUMMON_SECONDS_TIME);
        ((RedstoneMonstrosityAccessor) this).goetybossservants$setSummonCount(1);
        this.setAnimationState(SUMMON);
    }

    // ----- Lapis book ----------------------------------------------------------------------------

    public ItemStack getHeldBook() {
        return this.entityData.get(HELD_BOOK);
    }

    public void setHeldBook(ItemStack book) {
        this.entityData.set(HELD_BOOK, book);
    }

    @Override
    public @NotNull InteractionResult mobInteract(@NotNull Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (this.getTrueOwner() != player) {
            return super.mobInteract(player, hand);
        }
        if (this.material() == MonstrosityMaterial.LAPIS) {
            InteractionResult result = this.handleBook(player, hand, stack);
            if (result != InteractionResult.PASS) {
                return result;
            }
        }
        // Repaired with its own material's block (the Reinforced Redstone Block still works, through Goety).
        if (stack.getItem() instanceof net.minecraft.world.item.BlockItem blockItem && this.material().isFill(blockItem.getBlock().defaultBlockState())) {
            if (this.getHealth() >= this.getMaxHealth()) {
                return InteractionResult.PASS;
            }
            if (!this.level().isClientSide) {
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                this.heal(this.getMaxHealth() / 16.0f);
                this.playSound(SoundEvents.IRON_GOLEM_REPAIR, 1.0f, 1.0f);
                if (this.level() instanceof ServerLevel serverLevel) {
                    for (int i = 0; i < 7; ++i) {
                        serverLevel.sendParticles(ModParticleTypes.HEAL_EFFECT.get(), this.getRandomX(1.0), this.getRandomY(), this.getRandomZ(1.0), 0, 0.0, 0.0, 0.0, 0.5);
                    }
                }
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        // Plain redstone blocks are for the Redstone Monstrosity.
        if (stack.is(Tags.Items.STORAGE_BLOCKS_REDSTONE)) {
            return InteractionResult.PASS;
        }
        return super.mobInteract(player, hand);
    }

    private InteractionResult handleBook(Player player, InteractionHand hand, ItemStack stack) {
        if (player.isShiftKeyDown() && stack.isEmpty() && !this.getHeldBook().isEmpty()) {
            if (!this.level().isClientSide) {
                player.setItemInHand(hand, this.getHeldBook().copy());
                this.setHeldBook(ItemStack.EMPTY);
                this.playSound(SoundEvents.BOOK_PAGE_TURN, 1.0f, 1.0f);
                player.displayClientMessage(Component.translatable("info.goetybossservants.lapis_book.taken"), true);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        if (!stack.is(net.minecraft.world.item.Items.ENCHANTED_BOOK)) {
            return InteractionResult.PASS;
        }
        if (!this.level().isClientSide) {
            Map<Enchantment, Integer> usable = LapisBuffs.usableEnchantments(stack);
            if (usable.isEmpty()) {
                player.displayClientMessage(Component.translatable("info.goetybossservants.lapis_book.refused").withStyle(ChatFormatting.RED), true);
                return InteractionResult.CONSUME;
            }
            ItemStack old = this.getHeldBook();
            this.setHeldBook(stack.copyWithCount(1));
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            if (!old.isEmpty() && !player.addItem(old)) {
                player.drop(old, false);
            }
            this.playSound(SoundEvents.ENCHANTMENT_TABLE_USE, 1.0f, 1.0f);
            Component names = usable.entrySet().stream().map(entry -> entry.getKey().getFullname(entry.getValue()).copy().withStyle(ChatFormatting.AQUA))
                    .reduce((a, b) -> a.append(Component.literal(", ")).append(b)).orElse(Component.empty());
            player.displayClientMessage(Component.translatable("info.goetybossservants.lapis_book.held", names), true);
        }
        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("DiamondRain", this.rainTicks);
        tag.putBoolean("Minion", this.isMinion());
        if (!this.getHeldBook().isEmpty()) {
            tag.put("HeldBook", this.getHeldBook().save(new CompoundTag()));
        }
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.rainTicks = tag.getInt("DiamondRain");
        if (tag.getBoolean("Minion")) {
            this.entityData.set(MINION, true);
            this.refreshDimensions();
        }
        this.setHeldBook(tag.contains("HeldBook") ? ItemStack.of(tag.getCompound("HeldBook")) : ItemStack.EMPTY);
    }

    // ----- Death ---------------------------------------------------------------------------------

    /** Goety's death (5 seconds of animation), but dropping this material's head and any held book (minions drop nothing). */
    @Override
    protected void tickDeath() {
        ++this.deathTime;
        if (this.deathTime >= MathHelper.secondsToTicks(5)) {
            this.spawnAnim();
            if (!this.level().isClientSide) {
                if (this.getOwnerId() != null && !this.isMinion()) {
                    ItemStack head = MaterialMonstrosityHeadItem.create(this.material());
                    MaterialMonstrosityHeadItem.setOwner(head, this.getOwnerId(), this.getTrueOwner());
                    if (this.getCustomName() != null) {
                        MaterialMonstrosityHeadItem.setCustomName(head, this.getCustomName().getString());
                    }
                    ItemEntity item = this.spawnAtLocation(head);
                    if (item != null) {
                        item.setExtendedLifetime();
                    }
                }
                if (!this.getHeldBook().isEmpty()) {
                    this.spawnAtLocation(this.getHeldBook().copy());
                    this.setHeldBook(ItemStack.EMPTY);
                }
            }
            this.remove(Entity.RemovalReason.KILLED);
        }
        this.hurtTime = 1;
        this.setYRot(this.deathRotation);
        this.setYBodyRot(this.deathRotation);
    }

    /**
     * Whether {@code entity} is on this monstrosity's side: itself, its owner, and its owner's (or
     * its own) servants and pets, and the mobs risen for it or a monstrosity on its side (see
     * RisenMobs); for a Netherite minion, also everything on its master's side.
     */
    public boolean isAlly(Entity entity) {
        if (entity == this || this.getOwnerId() != null && this.getOwnerId().equals(entity.getUUID())) {
            return true;
        }
        if (RisenMobs.isRisen(entity)) {
            MaterialMonstrosity master = RisenMobs.master(entity);
            if (this.getUUID().equals(RisenMobs.masterId(entity)) || master != null && master != this && this.isAlly(master)) {
                return true;
            }
        }
        if (entity instanceof com.Polarice3.Goety.api.entities.IOwned owned && owned.getOwnerId() != null
                && (owned.getOwnerId().equals(this.getOwnerId()) || owned.getOwnerId().equals(this.getUUID()))) {
            return true;
        }
        // (And servants of servants, by whom they all serve in the end.)
        LivingEntity top = entity instanceof com.Polarice3.Goety.api.entities.IOwned owned ? owned.getMasterOwner() : null;
        if (top != null && top != entity && (top == this || top.getUUID().equals(this.getOwnerId()))) {
            return true;
        }
        // (And boss servants' minions, by their boss.)
        net.minecraft.world.entity.Mob boss = com.saints.goetybossservants.entity.servant.ServantMinions.isMinion(entity)
                ? com.saints.goetybossservants.entity.servant.ServantMinions.summoner(entity) : null;
        if (boss != null && boss != entity && this.isAlly(boss)) {
            return true;
        }
        return entity instanceof net.minecraft.world.entity.OwnableEntity pet && pet.getOwnerUUID() != null && pet.getOwnerUUID().equals(this.getOwnerId())
                || this.getTrueOwner() instanceof MaterialMonstrosity master && master != this && master.isAlly(entity);
    }

    /**
     * Whether an area attack of this monstrosity may hit {@code entity}: anything hostile to it, but
     * never its side, nor players, pets or (tame) servants unless that is what it's fighting. A hostile
     * summoner's minions are fair game.
     */
    public boolean canHitWithArea(LivingEntity entity) {
        if (!entity.isAlive() || !entity.isAttackable() || entity instanceof net.minecraft.world.entity.decoration.ArmorStand
                || this.isAlly(entity) || MobUtil.areAllies(this, entity)) {
            return false;
        }
        if (entity == this.getTarget()) {
            return true;
        }
        if (entity instanceof com.Polarice3.Goety.api.entities.IOwned owned) {
            return owned.getOwnerId() == null || owned.isHostile();
        }
        return !(entity instanceof Player) && !(entity instanceof net.minecraft.world.entity.OwnableEntity pet && pet.getOwnerUUID() != null);
    }

    /**
     * Whether {@code entity} is one of this monstrosity's foes, not a bystander its area attacks may
     * also hit (villagers, animals, calm neutral mobs): what it's fighting, what Goety's servants would
     * pick a fight with on their own (MobUtil.isOwnedTargetable), and whatever is fighting it or its
     * side. The Sculk one's tentacles go after these, and only these decay (and so rise as its
     * sculk zombies).
     */
    public boolean isFoe(LivingEntity entity) {
        if (!this.canHitWithArea(entity)) {
            return false;
        }
        if (entity == this.getTarget() || entity == this.getLastHurtByMob() || MobUtil.isOwnedTargetable(this, entity)) {
            return true;
        }
        // (A piglin keeps what it's fighting in its brain.)
        LivingEntity fighting = entity instanceof net.minecraft.world.entity.Mob mob ? mob.getTarget() : null;
        if (fighting == null && entity.getBrain().hasMemoryValue(net.minecraft.world.entity.ai.memory.MemoryModuleType.ATTACK_TARGET)) {
            fighting = entity.getBrain().getMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.ATTACK_TARGET).orElse(null);
        }
        return fighting != null && (this.isAlly(fighting) || MobUtil.areAllies(this, fighting));
    }

    /** Spawns this material's monstrosity entity type (used by molds, heads and the self-test). */
    @Nullable
    public static MaterialMonstrosity create(Level level, MonstrosityMaterial material) {
        return ModRegistry.MONSTROSITIES.get(material).get().create(level);
    }
}
