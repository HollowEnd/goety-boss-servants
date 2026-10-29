package com.saints.goetybossservants.mixin;

import com.saints.goetybossservants.entity.monstrosity.RisenMobs;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

/**
 * Nothing picks a fight it mustn't over a risen mob (see RisenMobs.blocksTargeting): a risen mob only
 * finds its monstrosity's foes, and nothing on its side finds it, whatever goal or sensor is looking.
 * (Vetoing setTarget alone isn't enough: a goal that keeps picking one would never pick another.)
 * Named twice (dev name and SRG name) since this mod ships no refmap.
 */
@Mixin(value = TargetingConditions.class, remap = false)
public abstract class TargetingConditionsMixin {
    @Inject(method = {"test", "m_26885_"}, at = @At("HEAD"), cancellable = true, require = 0)
    private void goetybossservants$risen(@Nullable LivingEntity attacker, LivingEntity target, CallbackInfoReturnable<Boolean> cir) {
        if (attacker != null && !attacker.level().isClientSide && RisenMobs.anyLoaded() && RisenMobs.blocksTargeting(attacker, target)) {
            cir.setReturnValue(false);
        }
    }
}
