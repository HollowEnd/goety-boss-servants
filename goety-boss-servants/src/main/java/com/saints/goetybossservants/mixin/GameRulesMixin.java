package com.saints.goetybossservants.mixin;

import com.saints.goetybossservants.event.ServantEvents;
import net.minecraft.world.level.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Mob griefing is off for risen mobs and what they shoot (see ServantEvents.startTicking). */
@Mixin(GameRules.class)
public abstract class GameRulesMixin {
    @Inject(method = "getBoolean", at = @At("HEAD"), cancellable = true)
    private void goetybossservants$risenGriefing(GameRules.Key<GameRules.BooleanValue> key, CallbackInfoReturnable<Boolean> cir) {
        if (key == GameRules.RULE_MOBGRIEFING && ServantEvents.griefingBlocked()) {
            cir.setReturnValue(false);
        }
    }
}
