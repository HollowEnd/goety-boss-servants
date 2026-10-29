package com.saints.goetybossservants.mixin;

import com.saints.goetybossservants.entity.monstrosity.RisenMobs;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A risen mob (see RisenMobs) stays until its time is up, even on Peaceful, doesn't burn in daylight,
 * and stays what it rose as (no drowned, stray or zombified piglin).
 */
@Mixin(Mob.class)
public abstract class RisenMobMixin {
    @Inject(method = "checkDespawn", at = @At("HEAD"), cancellable = true)
    private void goetybossservants$risenStays(CallbackInfo ci) {
        if (RisenMobs.isRisen((Mob) (Object) this)) {
            RisenMobs.HOOKS.add("checkDespawn");
            ci.cancel();
        }
    }

    @Inject(method = "isSunBurnTick", at = @At("HEAD"), cancellable = true)
    private void goetybossservants$risenUnburnt(CallbackInfoReturnable<Boolean> cir) {
        if (RisenMobs.isRisen((Mob) (Object) this)) {
            RisenMobs.HOOKS.add("isSunBurnTick");
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "convertTo", at = @At("HEAD"), cancellable = true)
    private <T extends Mob> void goetybossservants$risenUnconverted(EntityType<T> type, boolean keepEquipment, CallbackInfoReturnable<T> cir) {
        if (RisenMobs.isRisen((Mob) (Object) this)) {
            cir.setReturnValue(null);
        }
    }
}
