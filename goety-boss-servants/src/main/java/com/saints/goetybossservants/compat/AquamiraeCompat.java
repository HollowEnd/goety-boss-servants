package com.saints.goetybossservants.compat;

import com.saints.goetybossservants.entity.aquamirae.CaptainCorneliaServant;
import com.saints.goetybossservants.registry.ModEntities;
import com.saints.goetybossservants.registry.ModItems;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.monster.Monster;

/** The Captain Cornelia servant (Aquamirae). Only touched when Aquamirae is installed; see ModCompat. */
public final class AquamiraeCompat {
    private AquamiraeCompat() {
    }

    public static void register() {
        // Same size and flags as Aquamirae's captain_cornelia.
        ModEntities.CAPTAIN_CORNELIA = ModEntities.register("captain_cornelia_servant",
                EntityType.Builder.<CaptainCorneliaServant>of(CaptainCorneliaServant::new, MobCategory.MISC)
                        .sized(0.6f, 2.3f).clientTrackingRange(64).fireImmune(),
                // Aquamirae's own base; its config values are applied in finalizeSpawn, as on the boss.
                // (Built here, not in the servant class: loading that class this early would make Goety
                // read its config before it's loaded.)
                Monster.createMonsterAttributes());
        ModItems.CAPTAIN_CORNELIA_SOUL = ModItems.registerSoul("captain_cornelia_soul");
    }
}
