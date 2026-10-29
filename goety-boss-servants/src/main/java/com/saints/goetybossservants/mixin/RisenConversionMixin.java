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
 * stray (stopping only the conversion itself, RisenMobMixin, would leave it shaking, trying every tick).
 */
@Mixin({Zombie.class, Skeleton.class})
public abstract class RisenConversionMixin {
    @Inject(method = "startUnderWaterConversion", at = @At("HEAD"), cancellable = true, require = 0)
    private void goetybossservants$risenStaysZombie(int time, CallbackInfo ci) {
        if (RisenMobs.isRisen((Mob) (Object) this)) {
            ci.cancel();
        }
    }

    @Inject(method = "startFreezeConversion", at = @At("HEAD"), cancellable = true, require = 0)
    private void goetybossservants$risenStaysSkeleton(int time, CallbackInfo ci) {
        if (RisenMobs.isRisen((Mob) (Object) this)) {
            ci.cancel();
        }
    }
}
