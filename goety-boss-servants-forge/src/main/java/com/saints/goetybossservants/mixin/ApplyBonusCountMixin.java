package com.saints.goetybossservants.mixin;

import com.saints.goetybossservants.effect.LapisBuffEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.ApplyBonusCount;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.lang.reflect.Field;

/**
 * The Lapis Monstrosity's Fortune buff: block drops count extra Fortune levels for whoever broke the
 * block. Named twice (dev and SRG). The function's enchantment is read by reflection: mixin can't
 * alias a non-private field by its SRG name, and Forge's helper finds it under either name.
 */
@Mixin(value = ApplyBonusCount.class, remap = false)
public abstract class ApplyBonusCountMixin {
    @Unique
    private static final Field goetybossservants$ENCHANTMENT = ObfuscationReflectionHelper.findField(ApplyBonusCount.class, "f_79899_");

    @ModifyVariable(method = {"run", "m_7372_"}, at = @At("STORE"), ordinal = 0)
    private int goetybossservants$fortuneBuff(int level, ItemStack stack, LootContext context) {
        try {
            return level + LapisBuffEffects.fortuneBonus((Enchantment) goetybossservants$ENCHANTMENT.get(this), context);
        } catch (IllegalAccessException e) {
            return level;
        }
    }
}
