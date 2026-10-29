package com.saints.goetybossservants.registry;

import com.mojang.serialization.Codec;
import com.saints.goetybossservants.GoetyBossServants;
import com.saints.goetybossservants.block.MiraMonstrosityHeadBlock;
import com.saints.goetybossservants.block.MiraMonstrosityHeadBlockEntity;
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
import com.saints.goetybossservants.compat.ModCompat;
import com.saints.goetybossservants.entity.CaptainCorneliaServant;
import com.saints.goetybossservants.entity.MiraMonstrosityServant;
import com.saints.goetybossservants.entity.MiraVariant;
import com.saints.goetybossservants.entity.lich.NightLichServant;
import com.saints.goetybossservants.entity.soulslike.OldChampionServant;
import com.saints.goetybossservants.entity.knightquest.NethermanServant;
import com.saints.goetybossservants.entity.soulslike.ReturningKnightServant;
import com.saints.goetybossservants.entity.voidblossom.VoidBlossomServant;
import com.saints.goetybossservants.item.BossSoulItem;
import com.saints.goetybossservants.item.MiraMonstrosityHeadItem;
import com.saints.goetybossservants.loot.AddItemModifier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Everything a boss mod is needed for is only registered when that mod is installed (see ModCompat);
 * otherwise the field stays null and that servant, its soul and its head don't exist.
 */
public class ModRegistry {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, GoetyBossServants.MOD_ID);
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, GoetyBossServants.MOD_ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, GoetyBossServants.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, GoetyBossServants.MOD_ID);
    public static final DeferredRegister<Codec<? extends IGlobalLootModifier>> LOOT_MODIFIERS = DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, GoetyBossServants.MOD_ID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, GoetyBossServants.MOD_ID);

    @Nullable
    private static <T> RegistryObject<T> ifLoaded(boolean loaded, Supplier<RegistryObject<T>> register) {
        return loaded ? register.get() : null;
    }

    // Same size and flags as meowhem's own mira_monstrosity entity type.
    public static final RegistryObject<EntityType<MiraMonstrosityServant>> MIRA_MONSTROSITY = ifLoaded(ModCompat.meowhem(), () -> ENTITIES.register("mira_monstrosity",
            () -> EntityType.Builder.of(MiraMonstrosityServant::new, MobCategory.MISC)
                    .setShouldReceiveVelocityUpdates(true)
                    .setTrackingRange(32)
                    .fireImmune()
                    .immuneTo(Blocks.POWDER_SNOW)
                    .sized(5.5f, 11.0f)
                    .build(GoetyBossServants.MOD_ID + ":mira_monstrosity")));

    // Same size and flags as Aquamirae's captain_cornelia.
    public static final RegistryObject<EntityType<CaptainCorneliaServant>> CAPTAIN_CORNELIA = ifLoaded(ModCompat.aquamirae(), () -> ENTITIES.register("captain_cornelia",
            () -> EntityType.Builder.of(CaptainCorneliaServant::new, MobCategory.MISC)
                    .setShouldReceiveVelocityUpdates(true)
                    .sized(0.6f, 2.3f)
                    .setTrackingRange(64)
                    .setUpdateInterval(3)
                    .fireImmune()
                    .build(GoetyBossServants.MOD_ID + ":captain_cornelia")));

    public static final RegistryObject<Item> CAPTAIN_CORNELIA_SOUL = ifLoaded(ModCompat.aquamirae(), () -> ITEMS.register("captain_cornelia_soul", BossSoulItem::new));

    // Same size as BMD's void_blossom, but not fire-immune: the servant can burn.
    public static final RegistryObject<EntityType<VoidBlossomServant>> VOID_BLOSSOM = ifLoaded(ModCompat.bmd(), () -> ENTITIES.register("void_blossom",
            () -> EntityType.Builder.of(VoidBlossomServant::new, MobCategory.MISC)
                    .sized(8.0f, 10.0f)
                    .setTrackingRange(3)
                    .build(GoetyBossServants.MOD_ID + ":void_blossom")));

    public static final RegistryObject<Item> VOID_BLOSSOM_SOUL = ifLoaded(ModCompat.bmd(), () -> ITEMS.register("void_blossom_soul", BossSoulItem::new));

    // Same size as BMD's lich.
    public static final RegistryObject<EntityType<NightLichServant>> NIGHT_LICH = ifLoaded(ModCompat.bmd(), () -> ENTITIES.register("night_lich",
            () -> EntityType.Builder.of(NightLichServant::new, MobCategory.MISC)
                    .sized(1.8f, 3.0f)
                    .setUpdateInterval(1)
                    .build(GoetyBossServants.MOD_ID + ":night_lich")));
    public static final RegistryObject<EntityType<OldChampionServant>> OLD_CHAMPION = ifLoaded(ModCompat.soulslike(), () -> ENTITIES.register("old_champion",
            () -> EntityType.Builder.of(OldChampionServant::new, MobCategory.MISC)
                    .sized(1.5f, 3.0f)
                    .clientTrackingRange(10)
                    .build(GoetyBossServants.MOD_ID + ":old_champion")));
    public static final RegistryObject<EntityType<ReturningKnightServant>> RETURNING_KNIGHT = ifLoaded(ModCompat.soulslike(), () -> ENTITIES.register("returning_knight",
            () -> EntityType.Builder.of(ReturningKnightServant::new, MobCategory.MISC)
                    .sized(3.0f, 8.0f)
                    .clientTrackingRange(10)
                    .build(GoetyBossServants.MOD_ID + ":returning_knight")));
    // Same size as Knights Quest's netherman.
    public static final RegistryObject<EntityType<NethermanServant>> NETHERMAN = ifLoaded(ModCompat.knightquest(), () -> ENTITIES.register("netherman",
            () -> EntityType.Builder.of(NethermanServant::new, MobCategory.MISC)
                    .sized(0.8f, 2.8f)
                    .clientTrackingRange(10)
                    .build(GoetyBossServants.MOD_ID + ":netherman")));

    public static final RegistryObject<Item> NIGHT_LICH_SOUL = ifLoaded(ModCompat.bmd(), () -> ITEMS.register("night_lich_soul", BossSoulItem::new));
    public static final RegistryObject<Item> OLD_CHAMPION_SOUL = ifLoaded(ModCompat.soulslike(), () -> ITEMS.register("old_champion_soul", BossSoulItem::new));
    public static final RegistryObject<Item> RETURNING_KNIGHT_SOUL = ifLoaded(ModCompat.soulslike(), () -> ITEMS.register("returning_knight_soul", BossSoulItem::new));
    public static final RegistryObject<Item> NETHERMAN_SOUL = ifLoaded(ModCompat.knightquest(), () -> ITEMS.register("netherman_soul", BossSoulItem::new));

    public static final RegistryObject<Block> MIRA_MONSTROSITY_HEAD = ifLoaded(ModCompat.meowhem(), () -> BLOCKS.register("mira_monstrosity_head", MiraMonstrosityHeadBlock::new));

    public static final RegistryObject<Item> MIRA_MONSTROSITY_HEAD_ITEM = ifLoaded(ModCompat.meowhem(), () -> ITEMS.register("mira_monstrosity_head",
            () -> new MiraMonstrosityHeadItem(MIRA_MONSTROSITY_HEAD.get(), new Item.Properties().rarity(Rarity.EPIC).fireResistant())));

    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<MiraMonstrosityHeadBlockEntity>> MIRA_MONSTROSITY_HEAD_BE = ifLoaded(ModCompat.meowhem(), () -> BLOCK_ENTITIES.register("mira_monstrosity_head",
            () -> BlockEntityType.Builder.of(MiraMonstrosityHeadBlockEntity::new, MIRA_MONSTROSITY_HEAD.get()).build(null)));

    // ----- Material monstrosities (Goety only, always registered) ---------------------------------

    // Same size and flags as Goety's redstone_monstrosity.
    public static final Map<MonstrosityMaterial, RegistryObject<EntityType<MaterialMonstrosity>>> MONSTROSITIES = new EnumMap<>(MonstrosityMaterial.class);
    // Pets only: each egg's monstrosity belongs to whoever uses it. There are no hostile ones.
    public static final Map<MonstrosityMaterial, RegistryObject<Item>> MONSTROSITY_SPAWN_EGGS = new EnumMap<>(MonstrosityMaterial.class);

    static {
        for (MonstrosityMaterial material : MonstrosityMaterial.values()) {
            RegistryObject<EntityType<MaterialMonstrosity>> type = ENTITIES.register(material.entityName(),
                    () -> EntityType.Builder.<MaterialMonstrosity>of(MaterialMonstrosity::new, MobCategory.MISC)
                            .sized(4.0f * material.scale(), 5.4f * material.scale())
                            .fireImmune()
                            .clientTrackingRange(10)
                            .build(GoetyBossServants.MOD_ID + ":" + material.entityName()));
            MONSTROSITIES.put(material, type);
            MONSTROSITY_SPAWN_EGGS.put(material, ITEMS.register(material.entityName() + "_spawn_egg",
                    () -> new OwnedSpawnEggItem(type, material.eggColor, material.accentColor, new Item.Properties())));
        }
    }

    // A vanilla iron golem's size.
    public static final RegistryObject<EntityType<MonstrosityIronGolem>> MONSTROSITY_IRON_GOLEM = ENTITIES.register("monstrosity_iron_golem",
            () -> EntityType.Builder.<MonstrosityIronGolem>of(MonstrosityIronGolem::new, MobCategory.MISC)
                    .sized(1.4f, 2.7f)
                    .clientTrackingRange(10)
                    .build(GoetyBossServants.MOD_ID + ":monstrosity_iron_golem"));

    public static final RegistryObject<EntityType<DiamondSpike>> DIAMOND_SPIKE = ENTITIES.register("diamond_spike",
            () -> EntityType.Builder.<DiamondSpike>of(DiamondSpike::new, MobCategory.MISC)
                    .sized(0.8f, 1.8f)
                    .clientTrackingRange(8)
                    .updateInterval(1)
                    .noSave()
                    .build(GoetyBossServants.MOD_ID + ":diamond_spike"));

    public static final RegistryObject<EntityType<NetheriteRift>> NETHERITE_RIFT = ENTITIES.register("netherite_rift",
            () -> EntityType.Builder.<NetheriteRift>of(NetheriteRift::new, MobCategory.MISC)
                    .sized(0.5f, 0.1f)
                    .clientTrackingRange(10)
                    .updateInterval(20)
                    .noSave()
                    .build(GoetyBossServants.MOD_ID + ":netherite_rift"));

    // Sized like the mob it was, see SculkZombie.getDimensions.
    public static final RegistryObject<EntityType<SculkZombie>> SCULK_ZOMBIE = ENTITIES.register("sculk_zombie",
            () -> EntityType.Builder.<SculkZombie>of(SculkZombie::new, MobCategory.MISC)
                    .sized(0.6f, 1.95f)
                    .clientTrackingRange(10)
                    .build(GoetyBossServants.MOD_ID + ":sculk_zombie"));

    public static final RegistryObject<EntityType<SculkMire>> SCULK_MIRE = ENTITIES.register("sculk_mire",
            () -> EntityType.Builder.<SculkMire>of(SculkMire::new, MobCategory.MISC)
                    .sized(0.5f, 0.1f)
                    .clientTrackingRange(10)
                    .updateInterval(20)
                    .noSave()
                    .build(GoetyBossServants.MOD_ID + ":sculk_mire"));

    public static final RegistryObject<EntityType<SculkTentacle>> SCULK_TENTACLE = ENTITIES.register("sculk_tentacle",
            () -> EntityType.Builder.<SculkTentacle>of(SculkTentacle::new, MobCategory.MISC)
                    .sized(1.4f, 2.0f)
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .noSave()
                    .build(GoetyBossServants.MOD_ID + ":sculk_tentacle"));

    // Goety's scatter bomb's size.
    public static final RegistryObject<EntityType<SculkGlob>> SCULK_GLOB = ENTITIES.register("sculk_glob",
            () -> EntityType.Builder.<SculkGlob>of(SculkGlob::new, MobCategory.MISC)
                    .sized(0.5f, 0.5f)
                    .clientTrackingRange(8)
                    .updateInterval(1)
                    .noSave()
                    .build(GoetyBossServants.MOD_ID + ":sculk_glob"));

    public static final RegistryObject<Block> MATERIAL_MONSTROSITY_HEAD = BLOCKS.register("material_monstrosity_head", MaterialMonstrosityHeadBlock::new);

    public static final RegistryObject<Item> MATERIAL_MONSTROSITY_HEAD_ITEM = ITEMS.register("material_monstrosity_head",
            () -> new MaterialMonstrosityHeadItem(MATERIAL_MONSTROSITY_HEAD.get(), new Item.Properties().rarity(Rarity.EPIC).fireResistant()));

    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<MaterialMonstrosityHeadBlockEntity>> MATERIAL_MONSTROSITY_HEAD_BE = BLOCK_ENTITIES.register("material_monstrosity_head",
            () -> BlockEntityType.Builder.of(MaterialMonstrosityHeadBlockEntity::new, MATERIAL_MONSTROSITY_HEAD.get()).build(null));

    // The Sculk Monstrosity mold's core, dropped by wardens.
    public static final RegistryObject<Block> WARDEN_CORE = BLOCKS.register("warden_core", WardenCoreBlock::new);

    public static final RegistryObject<Item> WARDEN_CORE_ITEM = ITEMS.register("warden_core",
            () -> new BlockItem(WARDEN_CORE.get(), new Item.Properties().rarity(Rarity.RARE)));

    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<WardenCoreBlockEntity>> WARDEN_CORE_BE = BLOCK_ENTITIES.register("warden_core",
            () -> BlockEntityType.Builder.of(WardenCoreBlockEntity::new, WARDEN_CORE.get()).build(null));

    // Adds an item to a loot table's drops (wardens' Warden Cores, see loot_modifiers/warden_core.json).
    public static final RegistryObject<Codec<AddItemModifier>> ADD_ITEM = LOOT_MODIFIERS.register("add_item", AddItemModifier.CODEC);

    // Everything this mod adds, in its own tab after the vanilla ones (a boss mod's items only when it's installed).
    public static final RegistryObject<CreativeModeTab> TAB = TABS.register(GoetyBossServants.MOD_ID, () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup." + GoetyBossServants.MOD_ID))
            .icon(() -> MaterialMonstrosityHeadItem.create(MonstrosityMaterial.NETHERITE))
            .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
            .displayItems((parameters, output) -> {
                output.accept(WARDEN_CORE_ITEM.get());
                for (MonstrosityMaterial material : MonstrosityMaterial.values()) {
                    output.accept(MaterialMonstrosityHeadItem.create(material));
                }
                // The Mira Monstrosity's head, in each of its variants (the head revives the variant it is).
                if (MIRA_MONSTROSITY_HEAD_ITEM != null) {
                    for (MiraVariant variant : MiraVariant.values()) {
                        ItemStack head = new ItemStack(MIRA_MONSTROSITY_HEAD_ITEM.get());
                        if (variant != MiraVariant.MIRA) {
                            MiraMonstrosityHeadItem.setVariant(head, variant);
                        }
                        output.accept(head);
                    }
                }
                for (RegistryObject<Item> item : Arrays.asList(CAPTAIN_CORNELIA_SOUL, VOID_BLOSSOM_SOUL, NIGHT_LICH_SOUL,
                        OLD_CHAMPION_SOUL, RETURNING_KNIGHT_SOUL, NETHERMAN_SOUL)) {
                    if (item != null) {
                        output.accept(item.get());
                    }
                }
                MONSTROSITY_SPAWN_EGGS.values().forEach(egg -> output.accept(egg.get()));
            })
            .build());

    public static void register(IEventBus modBus) {
        ENTITIES.register(modBus);
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        LOOT_MODIFIERS.register(modBus);
        TABS.register(modBus);
        ModEffects.EFFECTS.register(modBus);
    }
}
