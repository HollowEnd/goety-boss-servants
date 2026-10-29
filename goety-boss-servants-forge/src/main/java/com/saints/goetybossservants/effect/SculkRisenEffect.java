package com.saints.goetybossservants.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * A mob risen as itself by Sculk Decay (for its time), or climbing out of the ground (see RisenMobs):
 * shown on no one, cured by nothing. Always given without particles or an icon.
 */
public class SculkRisenEffect extends MobEffect {
    public SculkRisenEffect() {
        super(MobEffectCategory.NEUTRAL, 0x0E6F7C);
    }

    @Override
    public List<ItemStack> getCurativeItems() {
        return new ArrayList<>();
    }
}
