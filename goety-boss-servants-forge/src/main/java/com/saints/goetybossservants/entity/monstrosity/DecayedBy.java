package com.saints.goetybossservants.entity.monstrosity;

import com.saints.goetybossservants.GoetyBossServants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Which Sculk Monstrosity decayed an entity last (the one its sculk zombie will fight for), kept in
 * the entity's saved data so it lasts through it being unloaded, saved or changing dimension. On
 * Forge, in the entity's persistent data.
 */
final class DecayedBy {
    private static final String KEY = GoetyBossServants.MOD_ID + ":sculk_decayed_by";

    private DecayedBy() {
    }

    static void set(LivingEntity entity, UUID by) {
        entity.getPersistentData().putUUID(KEY, by);
    }

    /** Who decayed {@code entity} (forgetting it), or null. */
    @Nullable
    static UUID take(LivingEntity entity) {
        CompoundTag data = entity.getPersistentData();
        if (!data.hasUUID(KEY)) {
            return null;
        }
        UUID by = data.getUUID(KEY);
        data.remove(KEY);
        return by;
    }
}
