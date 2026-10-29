package com.saints.goetybossservants.mixin;

import com.saints.goetybossservants.entity.servant.BossServant;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.soulsweaponry.entity.ai.goal.DraugrBossGoal;
import net.soulsweaponry.entity.ai.goal.ReturningKnightGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

@Mixin({DraugrBossGoal.class, ReturningKnightGoal.class})
public abstract class SoulslikeGoalMixin {
    /**
     * The Old Champion's and Returning Knight's area attacks (ground slam, battle cry, heavy blow,
     * Mace of Spades, Obliterate, Rupture) hurt, launch, knock back and debuff everything the boss
     * finds around it with level.getEntities(boss, box). For a boss servant, leave out its owner,
     * allies, players, pets and player-owned servants unless that's what it's fighting.
     */
    @Redirect(method = "*", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getEntities(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;"))
    private List<Entity> goetybossservants$spareFriendlies(Level level, Entity boss, AABB box) {
        List<Entity> found = level.getEntities(boss, box);
        if (boss instanceof BossServant servant && boss instanceof Mob mob) {
            return found.stream().filter(entity -> entity == mob.getTarget() || !BossServant.sparedByAreaAttacks(servant, entity)).toList();
        }
        return found;
    }
}
