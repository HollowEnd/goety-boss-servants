package com.saints.goetybossservants.entity.monstrosity;

import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Which Sculk Monstrosity decayed an entity last (the one its sculk zombie will fight for), kept in
 * the entity's saved data so it lasts through it being unloaded, saved or changing dimension. On
 * Fabric, in a field LivingEntityMixin gives every living entity, saved with it.
 */
public final class DecayedBy {
    private DecayedBy() {
    }

    /** What LivingEntityMixin adds to every living entity. */
    public interface Holder {
        @Nullable
        UUID goetybossservants$getDecayedBy();

        void goetybossservants$setDecayedBy(@Nullable UUID by);
    }

    static void set(LivingEntity entity, UUID by) {
        ((Holder) entity).goetybossservants$setDecayedBy(by);
    }

    /** Who decayed {@code entity} (forgetting it), or null. */
    @Nullable
    static UUID take(LivingEntity entity) {
        Holder holder = (Holder) entity;
        UUID by = holder.goetybossservants$getDecayedBy();
        holder.goetybossservants$setDecayedBy(null);
        return by;
    }
}
