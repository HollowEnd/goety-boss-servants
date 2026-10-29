package com.saints.goetybossservants.mixin;

import com.saints.goetybossservants.effect.LapisBuffEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.ApplyBonusCount;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** The Lapis Monstrosity's Fortune buff: block drops count extra Fortune levels for whoever broke the block. */
@Mixin(ApplyBonusCount.class)
public abstract class ApplyBonusCountMixin {
    @Shadow
    @Final
    Enchantment enchantment;

    @ModifyVariable(method = "run", at = @At("STORE"), ordinal = 0)
    private int goetybossservants$fortuneBuff(int level, ItemStack stack, LootContext context) {
        return level + LapisBuffEffects.fortuneBonus(this.enchantment, context);
    }
}
