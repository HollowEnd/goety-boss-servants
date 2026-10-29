package com.saints.goetybossservants.mixin;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Another mob's goals, for adjusting a boss servant's minions (see ReturningKnightServant). */
@Mixin(Mob.class)
public interface MobAccessor {
    @Accessor("goalSelector")
    GoalSelector goetybossservants$getGoalSelector();

    @Accessor("targetSelector")
    GoalSelector goetybossservants$getTargetSelector();
}
