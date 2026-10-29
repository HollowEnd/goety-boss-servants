package com.saints.goetybossservants.mixin;

import cn.sh1rocu.goety.util.transfer.ItemStackHandler;
import com.Polarice3.Goety.common.blocks.entities.DarkAltarBlockEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Works around a Goety Fabric-port bug: a ritual uses up its activation item by shrinking the altar
 * slot's stack directly, which skips the port's slot caches. The slot is then a "ghost" (its stack
 * is empty but the caches still list the old item), so the next ritual's activation item can't go
 * in and that ritual stops on its first tick. After every ritual, an emptied slot is set again
 * through the handler so its caches are rebuilt.
 */
@Mixin(DarkAltarBlockEntity.class)
public abstract class DarkAltarSlotFixMixin {
    @Inject(method = "stopRitual", at = @At("TAIL"))
    private void goetybossservants$resyncAltarSlot(boolean finished, CallbackInfo ci) {
        DarkAltarBlockEntity altar = (DarkAltarBlockEntity) (Object) this;
        if (altar.getLevel() == null || altar.getLevel().isClientSide) {
            return;
        }
        ItemStackHandler handler = altar.itemStackHandler;
        if (handler.getStackInSlot(0).isEmpty() && !handler.getVariantInSlot(0).isBlank()) {
            handler.setStackInSlot(0, ItemStack.EMPTY);
        }
    }
}
