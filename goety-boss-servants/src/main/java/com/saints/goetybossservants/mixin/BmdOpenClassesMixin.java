package com.saints.goetybossservants.mixin;

import org.spongepowered.asm.mixin.Mixin;

/**
 * Empty on purpose: it only makes these Bosses of Mass Destruction classes pass through
 * GoetyBossServantsMixinPlugin.preApply, which removes their "final" flags (Kotlin makes classes and
 * members final by default) so the Void Blossom and Night Lich servants can extend them.
 */
@Mixin(targets = {
        "net.barribob.boss.mob.utils.BaseEntity",
        "net.barribob.boss.mob.mobs.lich.LichEntity",
        "net.barribob.boss.mob.mobs.void_blossom.VoidBlossomEntity"
})
public abstract class BmdOpenClassesMixin {
}
