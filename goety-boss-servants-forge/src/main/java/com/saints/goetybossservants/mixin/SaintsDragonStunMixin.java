package com.saints.goetybossservants.mixin;

import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.component.DragonTamingStunComponent;
import com.saints.goetybossservants.entity.monstrosity.RisenMobs;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A wild Saints Dragons dragon hurt down to a third of its health falls into a stun to be tamed. A
 * risen one (see RisenMobs) never does: it fights on until it dies or crumbles.
 */
@Mixin(value = DragonTamingStunComponent.class, remap = false)
public abstract class SaintsDragonStunMixin {
    @Shadow
    @Final
    protected DragonEntity dragon;

    @Inject(method = "tryEnterHoldStateFromDamage", at = @At("HEAD"), cancellable = true)
    private void goetybossservants$risenNoHold(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (RisenMobs.isRisen(this.dragon)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = {"enterHoldState", "enterStun"}, at = @At("HEAD"), cancellable = true)
    private void goetybossservants$risenNoStun(CallbackInfo ci) {
        if (RisenMobs.isRisen(this.dragon)) {
            ci.cancel();
        }
    }
}
