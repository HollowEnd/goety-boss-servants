package com.saints.goetybossservants.mixin;

import com.saints.goetybossservants.entity.monstrosity.RisenMobs;
import net.minecraft.world.entity.monster.Creeper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A risen creeper (see RisenMobs) leaves no lingering cloud when it explodes: it would be one of its
 * risen effects. Named twice (dev name and SRG name) since this mod ships no refmap.
 */
@Mixin(value = Creeper.class, remap = false)
public abstract class RisenCreeperMixin {
    @Inject(method = {"spawnLingeringCloud", "m_32316_"}, at = @At("HEAD"), cancellable = true, require = 0)
    private void goetybossservants$risenNoCloud(CallbackInfo ci) {
        if (RisenMobs.isRisen((Creeper) (Object) this)) {
            ci.cancel();
        }
    }
}
