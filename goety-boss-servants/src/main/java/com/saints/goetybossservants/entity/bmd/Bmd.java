package com.saints.goetybossservants.entity.bmd;

import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import me.shedaniel.autoconfig.AutoConfig;
import net.barribob.boss.Mod;
import net.barribob.boss.config.LichConfig;
import net.barribob.boss.config.ModConfig;
import net.barribob.boss.config.VoidBlossomConfig;
import net.barribob.boss.mob.ai.action.IAction;
import net.barribob.boss.mob.ai.action.IActionStop;
import net.barribob.boss.mob.ai.goals.ActionGoal;
import net.barribob.boss.sound.ModSounds;
import net.barribob.boss.utils.ModUtils;
import net.barribob.maelstrom.general.event.TimedEvent;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

import java.util.function.BooleanSupplier;

/**
 * Small bridge from Java to Bosses of Mass Destruction's Kotlin API (Fabric): Kotlin lambdas, its
 * config, its sounds, and its sound and particle helpers. Only used when BMD is installed.
 */
public final class Bmd {
    private Bmd() {
    }

    /** A Java Runnable as the Kotlin () -> Unit that BMD's events take. */
    public static Function0<Unit> run(Runnable action) {
        return () -> {
            action.run();
            return Unit.INSTANCE;
        };
    }

    public static Function0<Boolean> test(BooleanSupplier condition) {
        return condition::getAsBoolean;
    }

    /** BMD's ActionGoal: runs tick while canUse holds, and end when it stops (no start action). */
    public static ActionGoal actionGoal(BooleanSupplier canUse, IAction tick, IActionStop end) {
        Function0<Boolean> condition = test(canUse);
        return new ActionGoal(condition, condition, tick, () -> {
        }, end != null ? end : () -> {
        });
    }

    /** One callback after a delay (Maelstrom's TimedEvent), dropped once shouldCancel is true. */
    public static TimedEvent after(int delay, BooleanSupplier shouldCancel, Runnable action) {
        return new TimedEvent(run(action), delay, 1, test(shouldCancel));
    }

    public static TimedEvent after(int delay, Runnable action) {
        return after(delay, () -> false, action);
    }

    public static ModConfig config() {
        return AutoConfig.getConfigHolder(ModConfig.class).getConfig();
    }

    public static LichConfig lichConfig() {
        return config().getLichConfig();
    }

    public static VoidBlossomConfig voidBlossomConfig() {
        return config().getVoidBlossomConfig();
    }

    public static ModSounds sounds() {
        return Mod.INSTANCE.getSounds();
    }

    /** BMD's own sound helper; pitch is randomized like BMD's default. */
    public static void playSound(ServerLevel level, Vec3 pos, SoundEvent sound, float volume, double range) {
        playSound(level, pos, sound, volume, ModUtils.INSTANCE.randomPitch(level.random), range);
    }

    public static void playSound(ServerLevel level, Vec3 pos, SoundEvent sound, float volume, float pitch, double range) {
        ModUtils.INSTANCE.playSound(level, pos, sound, SoundSource.HOSTILE, volume, pitch, range, null);
    }

    public static void spawnParticle(ServerLevel level, ParticleOptions particle, Vec3 pos, Vec3 velocity, int count, double speed) {
        ModUtils.INSTANCE.spawnParticle(level, particle, pos, velocity, count, speed);
    }
}
