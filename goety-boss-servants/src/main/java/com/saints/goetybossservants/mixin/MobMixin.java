package com.saints.goetybossservants.mixin;

import com.saints.goetybossservants.entity.servant.BossServant;
import com.saints.goetybossservants.event.ServantEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mob.class)
public abstract class MobMixin {
    /** Targeting rules for golems, minions and boss servants (ServantEvents.allowTarget). */
    @Inject(method = "setTarget", at = @At("HEAD"), cancellable = true)
    private void goetybossservants$checkTarget(LivingEntity target, CallbackInfo ci) {
        if ((Object) this instanceof BossServant servant) {
            BossServant.logTargetRequest(servant, target);
        }
        if (!ServantEvents.allowTarget((Mob) (Object) this, target)) {
            ci.cancel();
        }
    }
}
