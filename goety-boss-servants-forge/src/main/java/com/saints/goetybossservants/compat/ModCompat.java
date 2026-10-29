package com.saints.goetybossservants.compat;

import net.minecraftforge.fml.ModList;

/**
 * The boss mods are optional: each one unlocks only its own servants. Without it that servant isn't
 * registered at all (no entity, soul, head or ritual), so it's simply unobtainable.
 *
 * <p>Anything that touches a boss mod's classes lives in that mod's compat class (MeowhemCompat,
 * AquamiraeCompat, BmdCompat, SoulslikeCompat, KnightQuestCompat and their client halves) and is only called after
 * checking here, so those classes are never loaded when their mod is missing. Mixins are skipped
 * the same way (see GoetyBossServantsMixinPlugin) and rituals use forge:mod_loaded conditions.
 */
public final class ModCompat {
    public static final String MEOWHEM = "meowhem";
    public static final String AQUAMIRAE = "aquamirae";
    public static final String BMD = "bosses_of_mass_destruction";
    public static final String SOULSLIKE = "soulsweapons";
    public static final String KNIGHTQUEST = "knightquest";
    // Mods whose mobs rise from Sculk Decay with compat of their own (see RisenMobs, SculkPuppets).
    public static final String SAINTS_DRAGONS = "saintsdragons";
    public static final String ASTEMIRLIB = "astemirlib";

    private ModCompat() {
    }

    public static boolean isLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    public static boolean meowhem() {
        return isLoaded(MEOWHEM);
    }

    public static boolean aquamirae() {
        return isLoaded(AQUAMIRAE);
    }

    public static boolean bmd() {
        return isLoaded(BMD);
    }

    public static boolean soulslike() {
        return isLoaded(SOULSLIKE);
    }

    public static boolean knightquest() {
        return isLoaded(KNIGHTQUEST);
    }
}
