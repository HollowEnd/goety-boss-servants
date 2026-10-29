package com.saints.goetybossservants.mixin;

import com.Polarice3.Goety.common.entities.ally.golem.RedstoneMonstrosity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Goety's own fields (named the same in dev and in the real game), used by the material monstrosities. */
@Mixin(value = RedstoneMonstrosity.class, remap = false)
public interface RedstoneMonstrosityAccessor {
    /** Nonzero from the start of the summon attack until its Redstone Cubes are spawned. */
    @Accessor("summonCount")
    int goetybossservants$getSummonCount();

    @Accessor("summonCount")
    void goetybossservants$setSummonCount(int summonCount);

    @Accessor("summonCool")
    int goetybossservants$getSummonCool();

    @Accessor("summonCool")
    void goetybossservants$setSummonCool(int summonCool);

    /** Ticks it stands still after an attack (its goals don't run meanwhile). */
    @Accessor("postAttackCool")
    void goetybossservants$setPostAttackCool(int postAttackCool);
}
