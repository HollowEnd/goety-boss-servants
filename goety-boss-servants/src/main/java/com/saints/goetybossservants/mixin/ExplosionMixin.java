package com.saints.goetybossservants.mixin;

import com.saints.goetybossservants.entity.monstrosity.RisenMobs;
import com.saints.goetybossservants.entity.servant.BossServant;
import com.saints.goetybossservants.event.ServantEvents;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Explosions push everything nearby even when the damage is stopped, and break blocks. For a boss
 * servant's explosions (the lich's comets) and a risen mob's (a creeper's): spare friendlies the push,
 * and keep blocks intact unless block breaking is turned on.
 */
@Mixin(Explosion.class)
public abstract class ExplosionMixin {
    @Shadow
    @Final
    private ObjectArrayList<BlockPos> toBlow;

    @Shadow
    public abstract LivingEntity getIndirectSourceEntity();

    @Redirect(method = "explode", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getEntities(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;"))
    private List<Entity> goetybossservants$spareFriendlies(Level level, Entity source, AABB box) {
        List<Entity> found = level.getEntities(source, box);
        if (this.getIndirectSourceEntity() instanceof Mob risen && RisenMobs.isRisen(risen)) {
            found.removeIf(entity -> ServantEvents.sparedByRisenExplosion(risen, entity));
            return found;
        }
        BossServant servant = ServantEvents.attackingServant(this.getIndirectSourceEntity());
        if (servant != null) {
            found.removeIf(entity -> ServantEvents.sparedByExplosion(servant, entity));
        }
        return found;
    }

    @Inject(method = "explode", at = @At("TAIL"))
    private void goetybossservants$keepBlocks(CallbackInfo ci) {
        if (!ServantEvents.breaksBlocks() && (ServantEvents.attackingServant(this.getIndirectSourceEntity()) != null || RisenMobs.isRisen(this.getIndirectSourceEntity())
                || RisenMobs.isRisenShot(((Explosion) (Object) this).getDirectSourceEntity()))) {
            this.toBlow.clear();
        }
    }
}
