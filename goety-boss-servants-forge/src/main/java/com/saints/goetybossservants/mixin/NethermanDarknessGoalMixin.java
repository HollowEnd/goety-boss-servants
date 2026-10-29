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

/**
 * tick overrides Goal.tick, so it's listed by its dev and SRG ("m_8037_") names, like Player.addEffect
 * ("m_7292_"): in the real game Knights Quest's jar only has the SRG ones.
 */
@Mixin(value = NethermanDarknessGoal.class, remap = false)
public abstract class NethermanDarknessGoalMixin {
    @Shadow
    @Final
    private NethermanEntity netherman;

    /** The boss's darkness curse hits every player in the world; a servant's only hits its target. */
    @Redirect(method = {"tick", "m_8037_"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;addEffect(Lnet/minecraft/world/effect/MobEffectInstance;)Z"), require = 0)
    private boolean goetybossservants$darknessOnTargetOnlyDev(Player player, MobEffectInstance effect) {
        return this.goetybossservants$curse(player, effect);
    }

    @Redirect(method = {"tick", "m_8037_"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;m_7292_(Lnet/minecraft/world/effect/MobEffectInstance;)Z"), require = 0)
    private boolean goetybossservants$darknessOnTargetOnlySrg(Player player, MobEffectInstance effect) {
        return this.goetybossservants$curse(player, effect);
    }

    private boolean goetybossservants$curse(Player player, MobEffectInstance effect) {
        if (this.netherman instanceof BossServant && player != this.netherman.getTarget()) {
            return false;
        }
        return player.addEffect(effect);
    }
}
