package com.saints.goetybossservants.mixin;

import com.saints.goetybossservants.entity.monstrosity.RisenMobs;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A risen mob (see RisenMobs) stays until its time is up, even on Peaceful, and doesn't burn in
 * daylight. Named twice (dev name and SRG name) since this mod ships no refmap.
 */
@Mixin(value = Mob.class, remap = false)
public abstract class RisenMobMixin {
    @Inject(method = {"checkDespawn", "m_6043_"}, at = @At("HEAD"), cancellable = true, require = 0)
    private void goetybossservants$risenStays(CallbackInfo ci) {
        if (RisenMobs.isRisen((Mob) (Object) this)) {
            RisenMobs.HOOKS.add("checkDespawn");
            ci.cancel();
        }
    }

    @Inject(method = {"isSunBurnTick", "m_21527_"}, at = @At("HEAD"), cancellable = true, require = 0)
    private void goetybossservants$risenUnburnt(CallbackInfoReturnable<Boolean> cir) {
        if (RisenMobs.isRisen((Mob) (Object) this)) {
            RisenMobs.HOOKS.add("isSunBurnTick");
            cir.setReturnValue(false);
        }
    }
}
