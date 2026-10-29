package com.saints.goetybossservants.client;

import com.mojang.logging.LogUtils;
import com.saints.goetybossservants.GoetyBossServants;
import com.saints.goetybossservants.config.ServantConfig;
import com.saints.goetybossservants.entity.monstrosity.SculkZombie;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.WalkAnimationState;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.monster.hoglin.HoglinBase;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;

/**
 * A sculk zombie's copy of the mob it was (SculkZombie.puppet), on the client. It's never added to
 * the world (the zombie is what moves, fights and gets hit), but it's posed like the zombie every
 * tick and frame (where it is and faces, its walk, swing, hurt flash, health and death) and its own
 * client tick is run along with the zombie's, so whatever animates it there (idles, tails and necks,
 * tendrils, swings counting down) plays as on a live one. It can't move anything: it passes through
 * the local player, and makes no sound.
 */
final class SculkPuppets implements SculkZombie.PuppetHooks {
    static final SculkPuppets INSTANCE = new SculkPuppets();
    private static final Logger LOGGER = LogUtils.getLogger();
    // Mobs whose client tick isn't run on copies (say, one that drives the camera or the sky).
    private static final TagKey<EntityType<?>> NO_TICK = TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation(GoetyBossServants.MOD_ID, "puppet_no_tick"));
    private static final Set<EntityType<?>> FAILED = new HashSet<>();

    private SculkPuppets() {
    }

    /** Its copy of the mob it was, made the first time it's needed (a zombie if that mob can't be made). */
    @Nullable
    static LivingEntity puppet(SculkZombie zombie) {
        if (zombie.puppet == null) {
            zombie.puppet = zombie.puppetFailed ? null : create(zombie, zombie.sourceType(), zombie.look());
            if (zombie.puppet == null) {
                zombie.puppet = create(zombie, EntityType.ZOMBIE, new CompoundTag());
            }
        }
        return zombie.puppet;
    }

    @Nullable
    static LivingEntity create(SculkZombie zombie, EntityType<?> type, CompoundTag look) {
        Entity entity;
        try {
            entity = type.create(zombie.level());
        } catch (RuntimeException e) {
            return null;
        }
        if (!(entity instanceof LivingEntity living)) {
            return null;
        }
        try {
            // Its variant, size, age, equipment and so on.
            living.readAdditionalSaveData(look.copy());
        } catch (RuntimeException ignored) {
            // Whatever it could read is enough to look like it.
        }
        // A pet stands, whatever it was told before (its look never says it sits; this is its owner's order).
        if (living instanceof TamableAnimal pet) {
            pet.setOrderedToSit(false);
            pet.setInSittingPose(false);
        }
        // Always the same one (its animations and anything drawn from who it is stay the same when it is
        // built again), never mistaken for one in the world.
        if (look.hasUUID("SourceUUID")) {
            living.setUUID(look.getUUID("SourceUUID"));
        }
        living.setId(-1 - zombie.getId());
        living.setHealth(living.getMaxHealth());
        living.deathTime = 0;
        living.setSilent(true);
        return living;
    }

    /** Whether its copy's client tick is run. */
    private static boolean ticked(SculkZombie zombie, LivingEntity puppet) {
        return !zombie.puppetTickFailed && ServantConfig.SculkPuppetTicking.get() && !puppet.getType().is(NO_TICK);
    }

    @Override
    public void tick(SculkZombie zombie) {
        LivingEntity puppet = zombie.puppet;
        // (Only once it has been drawn, and has somewhere to be.)
        if (puppet == null || zombie.tickCount <= 2 || !ticked(zombie, puppet)) {
            return;
        }
        pose(zombie, puppet);
        puppet.noPhysics = true;
        puppet.setAirSupply(puppet.getMaxAirSupply());
        LocalPlayer player = Minecraft.getInstance().player;
        Vec3 motion = player == null ? null : player.getDeltaMovement();
        try {
            puppet.tick();
        } catch (RuntimeException | LinkageError e) {
            zombie.puppetTickFailed = true;
            if (FAILED.add(puppet.getType())) {
                LOGGER.warn("Sculk zombies looking like {} are drawn without its animations: its client tick failed on a copy", EntityType.getKey(puppet.getType()), e);
            }
        } finally {
            // (In case it pushed them anyway.)
            if (player != null) {
                player.setDeltaMovement(motion);
            }
        }
        if (puppet.isRemoved()) {
            // It removed itself: built afresh next time it's drawn, and never ticked again.
            zombie.puppet = null;
            zombie.puppetTickFailed = true;
            return;
        }
        pose(zombie, puppet);
    }

    @Override
    public boolean event(SculkZombie zombie, byte id) {
        LivingEntity puppet = zombie.puppet;
        if (puppet == null) {
            return false;
        }
        // An attack swing, but not to mobs that count theirs down as they tick when the copy doesn't
        // tick: it would hold the swing part way.
        if (id == 4 && !ticked(zombie, puppet) && (puppet instanceof IronGolem || puppet instanceof Ravager || puppet instanceof HoglinBase)) {
            return true;
        }
        try {
            puppet.handleEntityEvent(id);
        } catch (RuntimeException ignored) {
            // Shown as far as it could be.
        }
        return true;
    }

    /** Poses the copy as the zombie is posed this tick or frame. */
    static void pose(SculkZombie zombie, LivingEntity puppet) {
        puppet.setPos(zombie.getX(), zombie.getY(), zombie.getZ());
        puppet.xo = zombie.xo;
        puppet.yo = zombie.yo;
        puppet.zo = zombie.zo;
        puppet.xOld = zombie.xOld;
        puppet.yOld = zombie.yOld;
        puppet.zOld = zombie.zOld;
        // How it moved this tick (what animations that ask whether a mob is moving look at).
        puppet.setDeltaMovement(zombie.getX() - zombie.xo, zombie.getY() - zombie.yo, zombie.getZ() - zombie.zo);
        puppet.tickCount = zombie.tickCount;
        puppet.setYRot(zombie.getYRot());
        puppet.yRotO = zombie.yRotO;
        puppet.setXRot(zombie.getXRot());
        puppet.xRotO = zombie.xRotO;
        puppet.yBodyRot = zombie.yBodyRot;
        puppet.yBodyRotO = zombie.yBodyRotO;
        puppet.yHeadRot = zombie.yHeadRot;
        puppet.yHeadRotO = zombie.yHeadRotO;
        puppet.attackAnim = zombie.attackAnim;
        puppet.oAttackAnim = zombie.oAttackAnim;
        puppet.swinging = zombie.swinging;
        puppet.swingTime = zombie.swingTime;
        puppet.swingingArm = zombie.swingingArm;
        puppet.hurtTime = zombie.hurtTime;
        puppet.hurtDuration = zombie.hurtDuration;
        puppet.deathTime = zombie.deathTime;
        // As hurt as the zombie (some mobs look or move differently when low), and dying with it.
        float health = zombie.isDeadOrDying() ? 0.0f : Math.max(0.01f, zombie.getHealth() / Math.max(1.0f, zombie.getMaxHealth()) * puppet.getMaxHealth());
        if (puppet.getHealth() != health) {
            puppet.setHealth(health);
        }
        puppet.setOnGround(zombie.onGround());
        if (puppet.getPose() != zombie.getPose()) {
            puppet.setPose(zombie.getPose());
        }
        zombie.showStateOn(puppet);
        if (puppet instanceof Mob mob) {
            mob.setAggressive(zombie.isAggressive());
        }
        copyWalk(zombie.walkAnimation, puppet.walkAnimation);
    }

    // Makes {@code to} the same as {@code from}: its speed last tick and now, and how far it has walked.
    private static void copyWalk(WalkAnimationState from, WalkAnimationState to) {
        float speedOld = from.speed(0.0f), speed = from.speed(), position = from.position();
        to.update(position - to.position() - speed, 1.0f);
        to.setSpeed(speedOld);
        to.update(speed, 1.0f);
    }
}
