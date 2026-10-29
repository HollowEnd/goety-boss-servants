package com.saints.goetybossservants.compat;

import com.saints.goetybossservants.entity.knightquest.NethermanServant;
import com.saints.goetybossservants.entity.servant.ServantMinions;
import com.saints.goetybossservants.event.ServantEvents;
import com.saints.goetybossservants.registry.ModEntities;
import com.saints.goetybossservants.registry.ModItems;
import dev.xylonity.knightquest.common.entity.boss.NethermanCloneEntity;
import dev.xylonity.knightquest.common.entity.boss.NethermanEntity;
import dev.xylonity.knightquest.common.entity.boss.NethermanProjectileChargeEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** The Netherman servant (Knights Quest). Only touched when Knights Quest is installed; see ModCompat. */
public final class KnightQuestCompat {
    private KnightQuestCompat() {
    }

    public static void register() {
        // Same size as Knights Quest's netherman. Its health and damage come from Knights Quest's config
        // (applied by NethermanEntity on its first tick); follow range 32 like the other servants.
        ModEntities.NETHERMAN = ModEntities.register("netherman_servant",
                EntityType.Builder.<NethermanServant>of(NethermanServant::new, MobCategory.MISC).sized(0.8f, 2.8f).clientTrackingRange(10),
                NethermanEntity.setAttributes().add(Attributes.FOLLOW_RANGE, 32.0));
        ModItems.NETHERMAN_SOUL = ModItems.registerSoul("netherman_soul");
        ServantEvents.SPAWN_FILTERS.add(KnightQuestCompat::allowSpawn);
    }

    /** Clones and explosive orbs a Netherman servant's goals spawn become its minions. */
    private static boolean allowSpawn(Entity entity) {
        NethermanServant netherman = NethermanServant.summoning;
        if (netherman == null || entity.level().isClientSide || ServantMinions.isMinion(entity)
                || !(entity instanceof NethermanCloneEntity || entity instanceof NethermanProjectileChargeEntity)) {
            return true;
        }
        return netherman.claimMinion(entity);
    }
}
