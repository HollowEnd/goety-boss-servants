package com.saints.goetybossservants.registry;

import com.saints.goetybossservants.GoetyBossServants;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;

/**
 * The servant entity types. Each is only registered by its boss mod's compat class when that mod
 * is installed (see ModCompat), so the fields of missing boss mods stay null. The ids are the ones
 * the earlier Fabric releases used, so servants in existing worlds carry over.
 */
public final class ModEntities {
    public static EntityType<?> CAPTAIN_CORNELIA;
    public static EntityType<?> VOID_BLOSSOM;
    public static EntityType<?> NIGHT_LICH;
    public static EntityType<?> OLD_CHAMPION;
    public static EntityType<?> RETURNING_KNIGHT;
    public static EntityType<?> NETHERMAN;

    private ModEntities() {
    }

    public static <T extends LivingEntity> EntityType<T> register(String name, EntityType.Builder<T> builder, AttributeSupplier.Builder attributes) {
        EntityType<T> type = register(name, builder);
        FabricDefaultAttributeRegistry.register(type, attributes);
        return type;
    }

    /** For servants whose attributes are registered later (BMD's come from its config; see BmdCompat). */
    public static <T extends LivingEntity> EntityType<T> register(String name, EntityType.Builder<T> builder) {
        return Registry.register(BuiltInRegistries.ENTITY_TYPE, GoetyBossServants.id(name), builder.build(GoetyBossServants.MOD_ID + ":" + name));
    }
}
