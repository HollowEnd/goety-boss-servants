package com.saints.goetybossservants.debug;

import com.saints.goetybossservants.GoetyBossServants;
import com.saints.goetybossservants.compat.ModCompat;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;

/**
 * Each boss mod only unlocks its own servants (see ModCompat). For every boss mod, check that its
 * entities, items and rituals exist exactly when the mod is loaded. Run with -PwithoutMods=... to
 * check the missing side.
 */
class OptionalModChecks {
    private record Boss(String modId, String[] entities, String[] items, String[] rituals) {
    }

    private static final Boss[] BOSSES = {
            new Boss(ModCompat.AQUAMIRAE, new String[]{"captain_cornelia_servant"}, new String[]{"captain_cornelia_soul"},
                    new String[]{"summon_captain_cornelia_servant", "revive_captain_cornelia_servant"}),
            new Boss(ModCompat.BMD, new String[]{"void_blossom_servant", "lich_servant"}, new String[]{"void_blossom_soul", "night_lich_soul"},
                    new String[]{"summon_void_blossom_servant", "revive_void_blossom_servant", "summon_lich_servant", "revive_lich_servant"}),
            new Boss(ModCompat.SOULSLIKE, new String[]{"draugr_boss_servant", "returning_knight_servant"}, new String[]{"old_champion_soul", "returning_knight_soul"},
                    new String[]{"summon_draugr_boss_servant", "revive_draugr_boss_servant", "summon_returning_knight_servant", "revive_returning_knight_servant"}),
            new Boss(ModCompat.KNIGHTQUEST, new String[]{"netherman_servant"}, new String[]{"netherman_soul"},
                    new String[]{"summon_netherman_servant", "revive_netherman_servant"}),
    };

    static void check(ServerLevel level) {
        for (Boss boss : BOSSES) {
            boolean loaded = ModCompat.isLoaded(boss.modId());
            String state = boss.modId() + (loaded ? " loaded" : " missing");
            for (String entity : boss.entities()) {
                SelfTest.check(BuiltInRegistries.ENTITY_TYPE.containsKey(GoetyBossServants.id(entity)) == loaded, state + ": entity " + entity + (loaded ? " registered" : " not registered"));
            }
            for (String item : boss.items()) {
                SelfTest.check(BuiltInRegistries.ITEM.containsKey(GoetyBossServants.id(item)) == loaded, state + ": item " + item + (loaded ? " registered" : " not registered"));
            }
            for (String ritual : boss.rituals()) {
                SelfTest.check(level.getRecipeManager().byKey(GoetyBossServants.id(ritual)).isPresent() == loaded, state + ": ritual " + ritual + (loaded ? " loaded" : " skipped"));
            }
        }
    }
}
