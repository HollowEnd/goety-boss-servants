package com.saints.goetybossservants.mixin;

import com.Polarice3.Goety.utils.MobUtil;
import com.saints.goetybossservants.entity.monstrosity.RisenMobs;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Goety's servants (and its spells and area attacks) see a risen mob and its monstrosity's side as
 * allies, and never pick it as a target (see RisenMobs.sameSide).
 */
@Mixin(value = MobUtil.class, remap = false)
public abstract class GoetyMobUtilMixin {
    @Inject(method = "areAllies", at = @At("HEAD"), cancellable = true)
    private static void goetybossservants$risenAllies(Entity entity, Entity other, CallbackInfoReturnable<Boolean> cir) {
        if (RisenMobs.anyLoaded() && RisenMobs.sameSide(entity, other)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "isOwnedTargetable", at = @At("HEAD"), cancellable = true)
    private static void goetybossservants$risenTargetable(Entity attacker, LivingEntity target, CallbackInfoReturnable<Boolean> cir) {
        if (RisenMobs.anyLoaded() && RisenMobs.sameSide(attacker, target)) {
            cir.setReturnValue(false);
        }
    }
}
