package com.saints.goetybossservants.mixin;

import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import com.leon.saintsdragons.server.entity.dragons.stegonaut.Stegonaut;
import com.saints.goetybossservants.entity.monstrosity.RisenMobs;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Saints Dragons' dragons check whom they may fight with their own canTarget (their roars, bites and
 * target checks), not only Minecraft's targeting: a risen dragon (see RisenMobs) may only fight its
 * monstrosity's foes there too. Raevyx and Stegonaut decide some cases without asking DragonEntity's.
 */
@Mixin(value = {DragonEntity.class, Raevyx.class, Stegonaut.class}, remap = false)
public abstract class SaintsDragonTargetMixin {
    @Inject(method = "canTarget", at = @At("HEAD"), cancellable = true)
    private void goetybossservants$risenTarget(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof Mob dragon && entity instanceof LivingEntity target && !dragon.level().isClientSide
                && RisenMobs.isRisen(dragon) && !RisenMobs.mayAttack(dragon, target)) {
            cir.setReturnValue(false);
        }
    }
}
