package com.saints.goetybossservants.effect;

import com.saints.goetybossservants.entity.monstrosity.SculkDecay;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * Sculk Decay, from the Sculk Monstrosity's attacks: damage once a second for each stack (its
 * amplifier is the stacks less one). It keeps its own second (SculkDecay.tick), since every new
 * stack restarts its duration. What applies and stacks it, and what dies with it, is in SculkDecay.
 */
public class SculkDecayEffect extends MobEffect {
    public SculkDecayEffect() {
        super(MobEffectCategory.HARMFUL, 0x0E6F7C);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level() instanceof ServerLevel level) {
            SculkDecay.tick(level, entity, amplifier);
        }
    }
}
