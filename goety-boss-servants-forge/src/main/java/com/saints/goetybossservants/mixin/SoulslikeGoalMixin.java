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

@Mixin(value = {DraugrBossGoal.class, ReturningKnightGoal.class}, remap = false)
public abstract class SoulslikeGoalMixin {
    /**
     * The Old Champion's and Returning Knight's area attacks (ground slam, battle cry, heavy blow,
     * Mace of Spades, Obliterate, Rupture) hurt, launch, knock back and debuff everything the boss
     * finds around it with level.getEntities(boss, box). For a boss servant, leave out its owner,
     * allies, players, pets and player-owned servants unless that's what it's fighting.
     * Both the dev and the SRG name are listed since this mixin isn't remapped; one of them matches.
     */
    @Redirect(method = "*", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getEntities(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;"), require = 0)
    private List<Entity> goetybossservants$spareFriendliesDev(Level level, Entity boss, AABB box) {
        return goetybossservants$spare(level.getEntities(boss, box), boss);
    }

    @Redirect(method = "*", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;m_45933_(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;"), require = 0)
    private List<Entity> goetybossservants$spareFriendliesSrg(Level level, Entity boss, AABB box) {
        return goetybossservants$spare(level.getEntities(boss, box), boss);
    }

    private static List<Entity> goetybossservants$spare(List<Entity> found, Entity boss) {
        if (boss instanceof BossServant servant && boss instanceof Mob mob) {
            return found.stream().filter(entity -> entity == mob.getTarget() || !BossServant.sparedByAreaAttacks(servant, entity)).toList();
        }
        return found;
    }
}
