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
 *
 * <p>tick and die override vanilla methods and the targets are vanilla methods, so each is listed by
 * its dev and SRG names ("m_8119_" tick, "m_6667_" die, "m_6907_" players, "m_146917_" setTicksFrozen);
 * one of each pair matches.
 */
@Mixin(value = NethermanCloneEntity.class, remap = false)
public abstract class NethermanCloneMixin {
    @Redirect(method = {"tick", "m_8119_"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;players()Ljava/util/List;"), require = 0)
    private List<? extends Player> goetybossservants$noPlayerProximityDev(Level level) {
        return ServantMinions.isMinion((NethermanCloneEntity) (Object) this) ? List.of() : level.players();
    }

    @Redirect(method = {"tick", "m_8119_"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;m_6907_()Ljava/util/List;"), require = 0)
    private List<? extends Player> goetybossservants$noPlayerProximitySrg(Level level) {
        return ServantMinions.isMinion((NethermanCloneEntity) (Object) this) ? List.of() : level.players();
    }

    @Inject(method = {"tick", "m_8119_"}, at = @At("TAIL"))
    private void goetybossservants$burstOnTarget(CallbackInfo ci) {
        NethermanCloneEntity clone = (NethermanCloneEntity) (Object) this;
        LivingEntity target = clone.getTarget();
        if (!clone.level().isClientSide && clone.isAlive() && target != null && ServantMinions.isMinion(clone) && clone.distanceTo(target) <= 2.0f) {
            clone.kill();
        }
    }

    @Redirect(method = {"die", "m_6667_"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;setTicksFrozen(I)V"), require = 0)
    private void goetybossservants$spareFriendlyPlayersDev(Player player, int ticks) {
        if (!this.goetybossservants$spared(player)) {
            player.setTicksFrozen(ticks);
        }
    }

    @Redirect(method = {"die", "m_6667_"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;m_146917_(I)V"), require = 0)
    private void goetybossservants$spareFriendlyPlayersSrg(Player player, int ticks) {
        if (!this.goetybossservants$spared(player)) {
            player.setTicksFrozen(ticks);
        }
    }

    @Inject(method = {"die", "m_6667_"}, at = @At("TAIL"))
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
