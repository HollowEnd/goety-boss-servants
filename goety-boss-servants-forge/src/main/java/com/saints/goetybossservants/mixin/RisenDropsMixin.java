package com.saints.goetybossservants.mixin;

import com.saints.goetybossservants.entity.monstrosity.RisenMobs;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A risen mob (see RisenMobs) drops no items at all: not only its death loot (cancelled with the
 * drops event) but what some mobs drop around it (a dragon's chest and saddle, a fox's mouth item),
 * which would otherwise come out a second time, its corpse having dropped them already. Every
 * spawnAtLocation goes through this one. Named twice (dev name and SRG name) since this mod ships no
 * refmap.
 */
@Mixin(value = Entity.class, remap = false)
public abstract class RisenDropsMixin {
    @Inject(method = {"spawnAtLocation(Lnet/minecraft/world/item/ItemStack;F)Lnet/minecraft/world/entity/item/ItemEntity;",
            "m_5552_(Lnet/minecraft/world/item/ItemStack;F)Lnet/minecraft/world/entity/item/ItemEntity;"}, at = @At("HEAD"), cancellable = true, require = 0)
    private void goetybossservants$risenDropsNothing(ItemStack stack, float offset, CallbackInfoReturnable<ItemEntity> cir) {
        Entity self = (Entity) (Object) this;
        if (!self.level().isClientSide && RisenMobs.isRisen(self)) {
            RisenMobs.HOOKS.add("spawnAtLocation");
            cir.setReturnValue(null);
        }
    }
}
