package com.saints.goetybossservants.mixin;

import com.saints.goetybossservants.entity.servant.BossServant;
import dev.xylonity.knightquest.common.entity.boss.NethermanEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Knights Quest's Netherman, adjusted at runtime for the servant (NethermanServant); the boss is unchanged. */
@Mixin(NethermanEntity.class)
public abstract class NethermanMixin {
    /** The boss sprinkles experience through its death animation; a servant doesn't (no XP farm). */
    @Redirect(method = "tickDeath", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ExperienceOrb;award(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/phys/Vec3;I)V"))
    private void goetybossservants$noDeathExperience(ServerLevel level, Vec3 pos, int amount) {
        if (!((Object) this instanceof BossServant)) {
            ExperienceOrb.award(level, pos, amount);
        }
    }
}
