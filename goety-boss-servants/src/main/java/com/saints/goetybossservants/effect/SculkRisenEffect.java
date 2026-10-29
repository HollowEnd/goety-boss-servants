package com.saints.goetybossservants.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * A mob risen as itself by Sculk Decay (for its time), or climbing out of the ground (see RisenMobs):
 * shown on no one, and never taken away (LivingEntityMixin). Always given without particles or an icon.
 */
public class SculkRisenEffect extends MobEffect {
    public SculkRisenEffect() {
        super(MobEffectCategory.NEUTRAL, 0x0E6F7C);
    }
}
