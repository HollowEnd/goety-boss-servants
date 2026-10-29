package com.saints.goetybossservants.mixin;

import com.saints.goetybossservants.effect.LapisBuffEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The Lapis Monstrosity's movement, tool and looting buffs add their level to the enchantment's
 * level on the entity's gear (see LapisBuffEffects.enchantmentBonus).
 */
@Mixin(EnchantmentHelper.class)
public abstract class EnchantmentHelperMixin {
    @Inject(method = "getEnchantmentLevel(Lnet/minecraft/world/item/enchantment/Enchantment;Lnet/minecraft/world/entity/LivingEntity;)I", at = @At("RETURN"), cancellable = true)
    private static void goetybossservants$buffLevels(Enchantment enchantment, LivingEntity entity, CallbackInfoReturnable<Integer> cir) {
        int bonus = LapisBuffEffects.enchantmentBonus(enchantment, entity);
        if (bonus > 0) {
            cir.setReturnValue(cir.getReturnValueI() + bonus);
        }
    }
}
