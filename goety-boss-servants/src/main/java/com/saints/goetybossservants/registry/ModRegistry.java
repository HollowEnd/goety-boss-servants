package com.saints.goetybossservants.registry;

import com.saints.goetybossservants.GoetyBossServants;
import com.saints.goetybossservants.block.MaterialMonstrosityHeadBlock;
import com.saints.goetybossservants.block.MaterialMonstrosityHeadBlockEntity;
import com.saints.goetybossservants.block.WardenCoreBlock;
import com.saints.goetybossservants.block.WardenCoreBlockEntity;
import com.saints.goetybossservants.entity.monstrosity.DiamondSpike;
import com.saints.goetybossservants.entity.monstrosity.NetheriteRift;
import com.saints.goetybossservants.entity.monstrosity.MaterialMonstrosity;
import com.saints.goetybossservants.entity.monstrosity.MonstrosityIronGolem;
import com.saints.goetybossservants.entity.monstrosity.MonstrosityMaterial;
import com.saints.goetybossservants.entity.monstrosity.SculkGlob;
import com.saints.goetybossservants.entity.monstrosity.SculkMire;
import com.saints.goetybossservants.entity.monstrosity.SculkTentacle;
import com.saints.goetybossservants.entity.monstrosity.SculkZombie;
import com.saints.goetybossservants.item.MaterialMonstrosityHeadItem;
import com.saints.goetybossservants.item.OwnedSpawnEggItem;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.EnumMap;
import java.util.Map;

/**
 * The material monstrosities (Goety only, always registered): their entity types and pet-only
 * spawn eggs, the Iron one's golems, the Diamond one's spikes, the shared head block and the
 * Sculk one's Warden Core.
 */
public final class ModRegistry {
    // Same size and flags as Goety's redstone_monstrosity.
    public static final Map<MonstrosityMaterial, EntityType<MaterialMonstrosity>> MONSTROSITIES = new EnumMap<>(MonstrosityMaterial.class);
    // Pets only: each egg's monstrosity belongs to whoever uses it. There are no hostile ones.
    public static final Map<MonstrosityMaterial, Item> MONSTROSITY_SPAWN_EGGS = new EnumMap<>(MonstrosityMaterial.class);

    static {
        for (MonstrosityMaterial material : MonstrosityMaterial.values()) {
            EntityType<MaterialMonstrosity> type = ModEntities.register(material.entityName(), EntityType.Builder.<MaterialMonstrosity>of(MaterialMonstrosity::new, MobCategory.MISC)
                    .sized(4.0f * material.scale(), 5.4f * material.scale())
                    .fireImmune()
                    .clientTrackingRange(10));
            MONSTROSITIES.put(material, type);
            MONSTROSITY_SPAWN_EGGS.put(material, Registry.register(BuiltInRegistries.ITEM, GoetyBossServants.id(material.entityName() + "_spawn_egg"),
                    new OwnedSpawnEggItem(type, material.eggColor, material.accentColor, new Item.Properties())));
        }
    }

    // A vanilla iron golem's size.
    public static final EntityType<MonstrosityIronGolem> MONSTROSITY_IRON_GOLEM = ModEntities.register("monstrosity_iron_golem",
            EntityType.Builder.<MonstrosityIronGolem>of(MonstrosityIronGolem::new, MobCategory.MISC)
                    .sized(1.4f, 2.7f)
                    .clientTrackingRange(10));

    public static final EntityType<DiamondSpike> DIAMOND_SPIKE = Registry.register(BuiltInRegistries.ENTITY_TYPE, GoetyBossServants.id("diamond_spike"),
            EntityType.Builder.<DiamondSpike>of(DiamondSpike::new, MobCategory.MISC)
                    .sized(0.8f, 1.8f)
                    .clientTrackingRange(8)
                    .updateInterval(1)
                    .noSave()
                    .build(GoetyBossServants.MOD_ID + ":diamond_spike"));

    public static final EntityType<NetheriteRift> NETHERITE_RIFT = Registry.register(BuiltInRegistries.ENTITY_TYPE, GoetyBossServants.id("netherite_rift"),
            EntityType.Builder.<NetheriteRift>of(NetheriteRift::new, MobCategory.MISC)
                    .sized(0.5f, 0.1f)
                    .clientTrackingRange(10)
                    .updateInterval(20)
                    .noSave()
                    .build(GoetyBossServants.MOD_ID + ":netherite_rift"));

    // Sized like the mob it was, see SculkZombie.getDimensions.
    public static final EntityType<SculkZombie> SCULK_ZOMBIE = ModEntities.register("sculk_zombie",
            EntityType.Builder.<SculkZombie>of(SculkZombie::new, MobCategory.MISC)
                    .sized(0.6f, 1.95f)
                    .clientTrackingRange(10));

    public static final EntityType<SculkMire> SCULK_MIRE = Registry.register(BuiltInRegistries.ENTITY_TYPE, GoetyBossServants.id("sculk_mire"),
            EntityType.Builder.<SculkMire>of(SculkMire::new, MobCategory.MISC)
                    .sized(0.5f, 0.1f)
                    .clientTrackingRange(10)
                    .updateInterval(20)
                    .noSave()
                    .build(GoetyBossServants.MOD_ID + ":sculk_mire"));

    public static final EntityType<SculkTentacle> SCULK_TENTACLE = Registry.register(BuiltInRegistries.ENTITY_TYPE, GoetyBossServants.id("sculk_tentacle"),
            EntityType.Builder.<SculkTentacle>of(SculkTentacle::new, MobCategory.MISC)
                    .sized(1.4f, 2.0f)
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .noSave()
                    .build(GoetyBossServants.MOD_ID + ":sculk_tentacle"));

    // Goety's scatter bomb's size.
    public static final EntityType<SculkGlob> SCULK_GLOB = Registry.register(BuiltInRegistries.ENTITY_TYPE, GoetyBossServants.id("sculk_glob"),
            EntityType.Builder.<SculkGlob>of(SculkGlob::new, MobCategory.MISC)
                    .sized(0.5f, 0.5f)
                    .clientTrackingRange(8)
                    .updateInterval(1)
                    .noSave()
                    .build(GoetyBossServants.MOD_ID + ":sculk_glob"));

    public static final Block MATERIAL_MONSTROSITY_HEAD = Registry.register(BuiltInRegistries.BLOCK, GoetyBossServants.id("material_monstrosity_head"), new MaterialMonstrosityHeadBlock());

    public static final Item MATERIAL_MONSTROSITY_HEAD_ITEM = Registry.register(BuiltInRegistries.ITEM, GoetyBossServants.id("material_monstrosity_head"),
            new MaterialMonstrosityHeadItem(MATERIAL_MONSTROSITY_HEAD, new Item.Properties().rarity(Rarity.EPIC).fireResistant()));

    public static final BlockEntityType<MaterialMonstrosityHeadBlockEntity> MATERIAL_MONSTROSITY_HEAD_BE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
            GoetyBossServants.id("material_monstrosity_head"), FabricBlockEntityTypeBuilder.create(MaterialMonstrosityHeadBlockEntity::new, MATERIAL_MONSTROSITY_HEAD).build());

    // The Sculk Monstrosity mold's core, dropped by wardens.
    public static final Block WARDEN_CORE = Registry.register(BuiltInRegistries.BLOCK, GoetyBossServants.id("warden_core"), new WardenCoreBlock());

    public static final Item WARDEN_CORE_ITEM = Registry.register(BuiltInRegistries.ITEM, GoetyBossServants.id("warden_core"),
            new BlockItem(WARDEN_CORE, new Item.Properties().rarity(Rarity.RARE)));

    public static final BlockEntityType<WardenCoreBlockEntity> WARDEN_CORE_BE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
            GoetyBossServants.id("warden_core"), FabricBlockEntityTypeBuilder.create(WardenCoreBlockEntity::new, WARDEN_CORE).build());

    // Everything this mod adds, in its own tab (a boss mod's souls only when it's installed).
    public static final CreativeModeTab TAB = Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, GoetyBossServants.id(GoetyBossServants.MOD_ID), FabricItemGroup.builder()
            .title(Component.translatable("itemGroup." + GoetyBossServants.MOD_ID))
            .icon(() -> MaterialMonstrosityHeadItem.create(MonstrosityMaterial.NETHERITE))
            .displayItems((parameters, output) -> {
                output.accept(WARDEN_CORE_ITEM);
                for (MonstrosityMaterial material : MonstrosityMaterial.values()) {
                    output.accept(MaterialMonstrosityHeadItem.create(material));
                }
                ModItems.souls().forEach(output::accept);
                MONSTROSITY_SPAWN_EGGS.values().forEach(output::accept);
            })
            .build());

    private static boolean attributesRegistered;

    private ModRegistry() {
    }

    /**
     * The monstrosities' attributes start from Goety's, which read Goety's config: only ready once
     * Goety itself has initialized, so this runs when the server (or client) starts.
     */
    public static void registerAttributes() {
        if (attributesRegistered) {
            return;
        }
        attributesRegistered = true;
        MONSTROSITIES.forEach((material, type) -> FabricDefaultAttributeRegistry.register(type, MaterialMonstrosity.createAttributes(material)));
        FabricDefaultAttributeRegistry.register(MONSTROSITY_IRON_GOLEM, MonstrosityIronGolem.createAttributes());
        FabricDefaultAttributeRegistry.register(SCULK_ZOMBIE, SculkZombie.createAttributes());
    }

    /** Registers everything above (and the effects), and adds the eggs and heads to the creative tabs. */
    public static void init() {
        ModEffects.init();
        ServerLifecycleEvents.SERVER_STARTING.register(server -> registerAttributes());
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.SPAWN_EGGS).register(entries -> MONSTROSITY_SPAWN_EGGS.values().forEach(entries::accept));
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(entries -> {
            for (MonstrosityMaterial material : MonstrosityMaterial.values()) {
                entries.accept(MaterialMonstrosityHeadItem.create(material));
            }
        });
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.INGREDIENTS).register(entries -> entries.accept(WARDEN_CORE_ITEM));
    }
}
