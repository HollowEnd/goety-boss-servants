package com.saints.goetybossservants.mixin;

import com.saints.goetybossservants.entity.servant.BossServant;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.soulsweaponry.entity.mobs.DraugrBoss;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = DraugrBoss.class, remap = false)
public abstract class OldChampionMixin {
    /**
     * Above half health the boss gives itself Slowness II and Weakness II whenever it can see the
     * sky. The servant fights at full strength outdoors. Both the dev and the SRG name are listed
     * since this mixin isn't remapped; one of them matches.
     */
    @Redirect(method = {"aiStep", "m_8107_"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;canSeeSky(Lnet/minecraft/core/BlockPos;)Z"), require = 0)
    private boolean goetybossservants$noSkyDebuffDev(Level level, BlockPos pos) {
        return !((Object) this instanceof BossServant) && level.canSeeSky(pos);
    }

    @Redirect(method = {"aiStep", "m_8107_"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;m_45527_(Lnet/minecraft/core/BlockPos;)Z"), require = 0)
    private boolean goetybossservants$noSkyDebuffSrg(Level level, BlockPos pos) {
        return !((Object) this instanceof BossServant) && level.canSeeSky(pos);
    }

    /**
     * The boss takes less and less damage from hits with the same held item, without limit. Mobs
     * (and unarmed players) always "hold" the same thing, so a servant ended up immune to them.
     * For the servant, no hit ever counts as the same weapon: the counter stays at zero.
     */
    @Redirect(method = {"hurt", "m_6469_"}, at = @At(value = "INVOKE", target = "Ljava/util/Objects;equals(Ljava/lang/Object;Ljava/lang/Object;)Z"), require = 1, allow = 1)
    private boolean goetybossservants$noSameWeaponResistance(Object last, Object current) {
        return !((Object) this instanceof BossServant) && java.util.Objects.equals(last, current);
    }
}
