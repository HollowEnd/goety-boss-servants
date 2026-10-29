package com.saints.goetybossservants.mixin;

import com.saints.goetybossservants.entity.monstrosity.RisenMobs;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Zombie;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A risen zombie under water or skeleton in powder snow never starts turning into a drowned or a
 * stray (stopping only the conversion itself would leave it shaking and trying every tick). Named
 * twice (dev name and SRG name) since this mod ships no refmap.
 */
@Mixin(value = {Zombie.class, Skeleton.class}, remap = false)
public abstract class RisenConversionMixin {
    @Inject(method = {"startUnderWaterConversion", "m_34278_"}, at = @At("HEAD"), cancellable = true, require = 0)
    private void goetybossservants$risenStaysZombie(int time, CallbackInfo ci) {
        if (RisenMobs.isRisen((Mob) (Object) this)) {
            ci.cancel();
        }
    }

    @Inject(method = {"startFreezeConversion", "m_149830_"}, at = @At("HEAD"), cancellable = true, require = 0)
    private void goetybossservants$risenStaysSkeleton(int time, CallbackInfo ci) {
        if (RisenMobs.isRisen((Mob) (Object) this)) {
            ci.cancel();
        }
    }
}
