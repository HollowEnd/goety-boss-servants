package com.saints.goetybossservants.mixin;

import com.saints.goetybossservants.entity.servant.BossServant;
import dev.xylonity.knightquest.common.entity.boss.NethermanEntity;
import dev.xylonity.knightquest.common.entity.boss.ai.NethermanDarknessGoal;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(NethermanDarknessGoal.class)
public abstract class NethermanDarknessGoalMixin {
    @Shadow
    @Final
    private NethermanEntity netherman;

    /** The boss's darkness curse hits every player in the world; a servant's only hits its target. */
    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;addEffect(Lnet/minecraft/world/effect/MobEffectInstance;)Z"))
    private boolean goetybossservants$darknessOnTargetOnly(Player player, MobEffectInstance effect) {
        if (this.netherman instanceof BossServant && player != this.netherman.getTarget()) {
            return false;
        }
        return player.addEffect(effect);
    }
}
