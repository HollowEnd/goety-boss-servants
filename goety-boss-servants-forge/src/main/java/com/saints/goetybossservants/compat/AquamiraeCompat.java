package com.saints.goetybossservants.compat;

import com.saints.goetybossservants.entity.CaptainCorneliaServant;
import com.saints.goetybossservants.registry.ModRegistry;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;

/** The Captain Cornelia servant (Aquamirae). Only touched when Aquamirae is installed; see ModCompat. */
public final class AquamiraeCompat {
    private AquamiraeCompat() {
    }

    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ModRegistry.CAPTAIN_CORNELIA.get(), CaptainCorneliaServant.createAttributes().build());
    }
}
