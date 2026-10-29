package com.saints.goetybossservants.mixin;

import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.component.DragonSleepComponent;
import com.saints.goetybossservants.entity.monstrosity.RisenMobs;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A risen Saints Dragons dragon (see RisenMobs) never goes to sleep: it has a few minutes to serve
 * its monstrosity. (A dragon sits down to fall asleep and waits, however long it takes, until it's
 * seated; one that rose kept being stood up, and stalled on the way, frozen in its bind pose.) One
 * already asleep, or on its way there, is woken at once.
 */
@Mixin(value = DragonSleepComponent.class, remap = false)
public abstract class SaintsDragonSleepMixin {
    @Shadow
    @Final
    private DragonEntity dragon;

    @Shadow
    public abstract boolean isSleeping();

    @Shadow
    public abstract boolean isSleepTransitioning();

    @Shadow
    public abstract void wakeUpImmediately();

    // Never sleepy, either: some dragons act on wanting to sleep alone (a Volitans dives to its sleeping depth).
    @Inject(method = "wantsToSleep", at = @At("HEAD"), cancellable = true)
    private void goetybossservants$risenNeverSleepy(CallbackInfoReturnable<Boolean> cir) {
        if (RisenMobs.isRisen(this.dragon)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "canSleepInCurrentEnvironment", at = @At("HEAD"), cancellable = true)
    private void goetybossservants$risenCannotSleep(CallbackInfoReturnable<Boolean> cir) {
        if (RisenMobs.isRisen(this.dragon)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "startSleepEnter", at = @At("HEAD"), cancellable = true)
    private void goetybossservants$risenNeverFallsAsleep(CallbackInfo ci) {
        if (RisenMobs.isRisen(this.dragon)) {
            ci.cancel();
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void goetybossservants$risenWakes(CallbackInfo ci) {
        if (!this.dragon.level().isClientSide && RisenMobs.isRisen(this.dragon) && (this.isSleeping() || this.isSleepTransitioning())) {
            this.wakeUpImmediately();
        }
    }
}
