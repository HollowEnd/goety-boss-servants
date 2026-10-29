package com.saints.goetybossservants.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/** An effect whose behaviour lives in event handlers (see MonstrosityEvents) rather than in the effect itself. */
public class BuffEffect extends MobEffect {
    public BuffEffect(MobEffectCategory category, int color) {
        super(category, color);
    }
}
