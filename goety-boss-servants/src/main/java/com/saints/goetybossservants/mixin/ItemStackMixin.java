package com.saints.goetybossservants.mixin;

import com.saints.goetybossservants.effect.LapisBuffEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

/** The Lapis Monstrosity's Unbreaking buff: some wear on the holder's items is ignored. */
@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
    @Inject(method = "hurtAndBreak", at = @At("HEAD"), cancellable = true)
    private <T extends LivingEntity> void goetybossservants$unbreakingBuff(int amount, T entity, Consumer<T> onBroken, CallbackInfo ci) {
        if (LapisBuffEffects.savesDurability(entity)) {
            ci.cancel();
        }
    }
}
