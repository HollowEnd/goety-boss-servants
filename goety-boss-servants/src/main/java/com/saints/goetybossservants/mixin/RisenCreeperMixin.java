package com.saints.goetybossservants.mixin;

import com.saints.goetybossservants.entity.monstrosity.RisenMobs;
import net.minecraft.world.entity.monster.Creeper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A risen creeper (see RisenMobs) leaves no lingering cloud when it explodes: it would be one of its
 * risen effects.
 */
@Mixin(Creeper.class)
public abstract class RisenCreeperMixin {
    @Inject(method = "spawnLingeringCloud", at = @At("HEAD"), cancellable = true)
    private void goetybossservants$risenNoCloud(CallbackInfo ci) {
        if (RisenMobs.isRisen((Creeper) (Object) this)) {
            ci.cancel();
        }
    }
}
