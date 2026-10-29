package com.saints.goetybossservants.effect;

import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

/** The Lapis Monstrosity's Sharpness buff: attack damage like the enchantment, +0.5 per level plus 0.5 (V gives +3). */
public class SharpnessEffect extends BuffEffect {
    public SharpnessEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public double getAttributeModifierValue(int amplifier, AttributeModifier modifier) {
        return 0.5 * (amplifier + 1) + 0.5;
    }
}
