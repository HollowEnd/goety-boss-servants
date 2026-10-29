package com.saints.goetybossservants.mixin;

import com.saints.goetybossservants.entity.servant.BossServant;
import com.saints.goetybossservants.event.ServantEvents;
import dev.xylonity.knightquest.common.entity.boss.NethermanEntity;
import dev.xylonity.knightquest.common.entity.boss.ai.NethermanLavaTeleportGoal;
import net.minecraft.world.level.GameRules;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(NethermanLavaTeleportGoal.class)
public abstract class NethermanLavaTeleportGoalMixin {
    @Shadow
    @Final
    private NethermanEntity netherman;

    /**
     * In its fire form the boss leaves lava under every spot it blinks to (when mob griefing is on).
     * A servant only does that when boss servants may change terrain (bossesBreakBlocks).
     */
    @Redirect(method = "teleportAroundTarget", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/GameRules;getBoolean(Lnet/minecraft/world/level/GameRules$Key;)Z"))
    private boolean goetybossservants$lavaOnlyIfAllowed(GameRules rules, GameRules.Key<GameRules.BooleanValue> key) {
        return rules.getBoolean(key) && (!(this.netherman instanceof BossServant) || ServantEvents.breaksBlocks());
    }
}
