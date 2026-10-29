package com.saints.goetybossservants.debug;

import com.saints.goetybossservants.compat.ModCompat;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.registries.ForgeRegistries;

import static com.saints.goetybossservants.debug.SelfTest.check;

/**
 * Each boss mod only unlocks its own servants (see ModCompat). For every boss mod, check that its
 * entities, items and rituals exist exactly when the mod is loaded. Run with -PwithoutMods=... to
 * check the missing side.
 */
class OptionalModChecks {
    private record Boss(String modId, String[] entities, String[] items, String[] rituals) {
    }

    private static final Boss[] BOSSES = {
            new Boss(ModCompat.MEOWHEM, new String[]{"mira_monstrosity"}, new String[]{"mira_monstrosity_head"}, new String[]{}),
            new Boss(ModCompat.AQUAMIRAE, new String[]{"captain_cornelia"}, new String[]{"captain_cornelia_soul"},
                    new String[]{"summon_captain_cornelia", "revive_captain_cornelia"}),
            new Boss(ModCompat.BMD, new String[]{"void_blossom", "night_lich"}, new String[]{"void_blossom_soul", "night_lich_soul"},
                    new String[]{"summon_void_blossom", "revive_void_blossom", "summon_night_lich", "revive_night_lich"}),
            new Boss(ModCompat.SOULSLIKE, new String[]{"old_champion", "returning_knight"}, new String[]{"old_champion_soul", "returning_knight_soul"},
                    new String[]{"summon_old_champion", "revive_old_champion", "summon_returning_knight", "revive_returning_knight"}),
            new Boss(ModCompat.KNIGHTQUEST, new String[]{"netherman"}, new String[]{"netherman_soul"}, new String[]{"summon_netherman", "revive_netherman"}),
    };

    private static ResourceLocation id(String path) {
        return new ResourceLocation("goetybossservants", path);
    }

    static void check(ServerLevel level) {
        for (Boss boss : BOSSES) {
            boolean loaded = ModCompat.isLoaded(boss.modId());
            String state = boss.modId() + (loaded ? " loaded" : " missing");
            for (String entity : boss.entities()) {
                SelfTest.check(ForgeRegistries.ENTITY_TYPES.containsKey(id(entity)) == loaded, state + ": entity " + entity + (loaded ? " registered" : " not registered"));
            }
            for (String item : boss.items()) {
                SelfTest.check(ForgeRegistries.ITEMS.containsKey(id(item)) == loaded, state + ": item " + item + (loaded ? " registered" : " not registered"));
            }
            for (String ritual : boss.rituals()) {
                SelfTest.check(level.getRecipeManager().byKey(id(ritual)).isPresent() == loaded, state + ": ritual " + ritual + (loaded ? " loaded" : " skipped"));
            }
        }
    }
}
