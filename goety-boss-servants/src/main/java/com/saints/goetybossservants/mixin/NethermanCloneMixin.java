package com.saints.goetybossservants.mixin;

import com.saints.goetybossservants.entity.servant.ServantMinions;
import dev.xylonity.knightquest.common.entity.boss.NethermanCloneEntity;
import dev.xylonity.knightquest.config.values.KQConfigValues;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Knights Quest's Netherman clones are built around players: one bursts as soon as any player comes
 * within 2 blocks, and the burst freezes every player within 3. A Netherman servant's clones are its
 * minions (see NethermanServant and ServantMinions), so for them: burst on reaching their target
 * (any mob), and freeze whatever is near except the servant's owner and other friendlies.
 */
@Mixin(NethermanCloneEntity.class)
public abstract class NethermanCloneMixin {
    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;players()Ljava/util/List;"))
    private List<? extends Player> goetybossservants$noPlayerProximity(Level level) {
        return ServantMinions.isMinion((NethermanCloneEntity) (Object) this) ? List.of() : level.players();
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void goetybossservants$burstOnTarget(CallbackInfo ci) {
        NethermanCloneEntity clone = (NethermanCloneEntity) (Object) this;
        LivingEntity target = clone.getTarget();
        if (!clone.level().isClientSide && clone.isAlive() && target != null && ServantMinions.isMinion(clone) && clone.distanceTo(target) <= 2.0f) {
            clone.kill();
        }
    }

    @Redirect(method = "die", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;setTicksFrozen(I)V"))
    private void goetybossservants$spareFriendlyPlayers(Player player, int ticks) {
        if (!this.goetybossservants$spared(player)) {
            player.setTicksFrozen(ticks);
        }
    }

    @Inject(method = "die", at = @At("TAIL"))
    private void goetybossservants$freezeMobs(DamageSource source, CallbackInfo ci) {
        NethermanCloneEntity clone = (NethermanCloneEntity) (Object) this;
        if (clone.level().isClientSide || !ServantMinions.isMinion(clone)) {
            return;
        }
        for (LivingEntity living : clone.level().getEntitiesOfClass(LivingEntity.class, clone.getBoundingBox().inflate(3.0),
                entity -> entity != clone && !(entity instanceof Player) && clone.distanceTo(entity) <= 3.0f)) {
            if (!this.goetybossservants$spared(living)) {
                living.setTicksFrozen(living.getTicksFrozen() + KQConfigValues.CLONE_EXPLOSION_FREEZE_TICKS);
            }
        }
    }

    private boolean goetybossservants$spared(LivingEntity entity) {
        NethermanCloneEntity clone = (NethermanCloneEntity) (Object) this;
        Mob boss = ServantMinions.isMinion(clone) ? ServantMinions.summoner(clone) : null;
        return boss != null && (ServantMinions.spares(boss, entity) || ServantMinions.isMinionOf(entity, boss));
    }
}
