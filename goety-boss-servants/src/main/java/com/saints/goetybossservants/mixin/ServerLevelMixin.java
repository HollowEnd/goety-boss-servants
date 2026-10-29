package com.saints.goetybossservants.mixin;

import com.saints.goetybossservants.event.ServantEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {
    /** Risen mobs and what they shoot don't grief (ServantEvents.startTicking). */
    @Inject(method = "tickNonPassenger", at = @At("HEAD"))
    private void goetybossservants$startTicking(Entity entity, CallbackInfo ci) {
        ServantEvents.startTicking(entity);
    }

    @Inject(method = "tickNonPassenger", at = @At("RETURN"))
    private void goetybossservants$stopTicking(Entity entity, CallbackInfo ci) {
        ServantEvents.stopTicking();
    }

    /** Lets boss compat code claim or stop entities a boss's own code spawns (ServantEvents.SPAWN_FILTERS). */
    @Inject(method = "addFreshEntity", at = @At("HEAD"), cancellable = true)
    private void goetybossservants$filterSpawn(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (!ServantEvents.allowSpawn(entity)) {
            cir.setReturnValue(false);
        }
    }
}
