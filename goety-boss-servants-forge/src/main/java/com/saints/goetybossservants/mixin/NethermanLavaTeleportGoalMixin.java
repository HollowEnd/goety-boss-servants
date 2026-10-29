package com.saints.goetybossservants.mixin;

import com.saints.goetybossservants.config.ServantConfig;
import com.saints.goetybossservants.entity.servant.BossServant;
import dev.xylonity.knightquest.common.entity.boss.NethermanEntity;
import dev.xylonity.knightquest.common.entity.boss.ai.NethermanLavaTeleportGoal;
import net.minecraft.world.level.GameRules;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** teleportAroundTarget is the goal's own method; GameRules.getBoolean is listed by its dev and SRG ("m_46207_") names. */
@Mixin(value = NethermanLavaTeleportGoal.class, remap = false)
public abstract class NethermanLavaTeleportGoalMixin {
    @Shadow
    @Final
    private NethermanEntity netherman;

    /**
     * In its fire form the boss leaves lava under every spot it blinks to (when mob griefing is on).
     * A servant only does that when boss servants may change terrain (bossesBreakBlocks).
     */
    @Redirect(method = "teleportAroundTarget", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/GameRules;getBoolean(Lnet/minecraft/world/level/GameRules$Key;)Z"), require = 0)
    private boolean goetybossservants$lavaOnlyIfAllowedDev(GameRules rules, GameRules.Key<GameRules.BooleanValue> key) {
        return this.goetybossservants$lava(rules, key);
    }

    @Redirect(method = "teleportAroundTarget", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/GameRules;m_46207_(Lnet/minecraft/world/level/GameRules$Key;)Z"), require = 0)
    private boolean goetybossservants$lavaOnlyIfAllowedSrg(GameRules rules, GameRules.Key<GameRules.BooleanValue> key) {
        return this.goetybossservants$lava(rules, key);
    }

    private boolean goetybossservants$lava(GameRules rules, GameRules.Key<GameRules.BooleanValue> key) {
        return rules.getBoolean(key) && (!(this.netherman instanceof BossServant) || ServantConfig.BossesBreakBlocks.get());
    }
}
