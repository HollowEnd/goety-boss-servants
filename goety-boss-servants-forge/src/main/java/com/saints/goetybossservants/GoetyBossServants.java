package com.saints.goetybossservants;

import com.saints.goetybossservants.compat.AquamiraeCompat;
import com.saints.goetybossservants.compat.BmdCompat;
import com.saints.goetybossservants.compat.MeowhemCompat;
import com.saints.goetybossservants.compat.ModCompat;
import com.saints.goetybossservants.compat.PatchouliMolds;
import com.saints.goetybossservants.compat.KnightQuestCompat;
import com.saints.goetybossservants.compat.SoulslikeCompat;
import com.saints.goetybossservants.config.ServantConfig;
import com.saints.goetybossservants.entity.monstrosity.MaterialMonstrosity;
import com.saints.goetybossservants.entity.monstrosity.MonstrosityIronGolem;
import com.saints.goetybossservants.entity.monstrosity.MonstrosityMaterial;
import com.saints.goetybossservants.entity.monstrosity.SculkZombie;
import com.saints.goetybossservants.item.MaterialMonstrosityHeadItem;
import com.saints.goetybossservants.registry.ModRegistry;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.RegistryObject;

/** Goety is required; each boss mod is optional and only unlocks its own servants (see ModCompat). */
@Mod(GoetyBossServants.MOD_ID)
public class GoetyBossServants {
    public static final String MOD_ID = "goetybossservants";

    public GoetyBossServants() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ServantConfig.SPEC, MOD_ID + "-common.toml");
        ModRegistry.register(modBus);
        modBus.addListener(this::registerAttributes);
        modBus.addListener(this::addCreativeTabItems);
        modBus.addListener(this::commonSetup);
        if (ModCompat.meowhem()) {
            MeowhemCompat.registerEvents();
        }
        if (ModCompat.bmd()) {
            BmdCompat.registerEvents();
        }
        if (ModCompat.soulslike()) {
            SoulslikeCompat.registerEvents();
        }
        if (ModCompat.knightquest()) {
            KnightQuestCompat.registerEvents();
        }
    }

    private void registerAttributes(EntityAttributeCreationEvent event) {
        ModRegistry.MONSTROSITIES.forEach((material, type) -> event.put(type.get(), MaterialMonstrosity.createAttributes(material).build()));
        event.put(ModRegistry.MONSTROSITY_IRON_GOLEM.get(), MonstrosityIronGolem.createAttributes().build());
        event.put(ModRegistry.SCULK_ZOMBIE.get(), SculkZombie.createAttributes().build());
        if (ModCompat.meowhem()) {
            MeowhemCompat.registerAttributes(event);
        }
        if (ModCompat.aquamirae()) {
            AquamiraeCompat.registerAttributes(event);
        }
        if (ModCompat.bmd()) {
            BmdCompat.registerAttributes(event);
        }
        if (ModCompat.soulslike()) {
            SoulslikeCompat.registerAttributes(event);
        }
        if (ModCompat.knightquest()) {
            KnightQuestCompat.registerAttributes(event);
        }
    }

    // The molds for this mod's pages of Goety's Black Book, which needs Patchouli (see PatchouliMolds).
    private void commonSetup(FMLCommonSetupEvent event) {
        if (ModCompat.isLoaded("patchouli")) {
            event.enqueueWork(PatchouliMolds::register);
        }
    }

    private void addCreativeTabItems(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            accept(event, ModRegistry.MIRA_MONSTROSITY_HEAD_ITEM);
            for (MonstrosityMaterial material : MonstrosityMaterial.values()) {
                event.accept(MaterialMonstrosityHeadItem.create(material));
            }
        } else if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            ModRegistry.MONSTROSITY_SPAWN_EGGS.values().forEach(event::accept);
        } else if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(ModRegistry.WARDEN_CORE_ITEM);
            accept(event, ModRegistry.CAPTAIN_CORNELIA_SOUL);
            accept(event, ModRegistry.VOID_BLOSSOM_SOUL);
            accept(event, ModRegistry.NIGHT_LICH_SOUL);
            accept(event, ModRegistry.OLD_CHAMPION_SOUL);
            accept(event, ModRegistry.RETURNING_KNIGHT_SOUL);
            accept(event, ModRegistry.NETHERMAN_SOUL);
        }
    }

    /** Items for missing boss mods aren't registered (null); skip them. */
    private static void accept(BuildCreativeModeTabContentsEvent event, RegistryObject<Item> item) {
        if (item != null) {
            event.accept(item);
        }
    }
}
