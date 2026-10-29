package com.saints.goetybossservants.registry;

import com.saints.goetybossservants.GoetyBossServants;
import com.saints.goetybossservants.item.BossSoulItem;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.List;

/**
 * Boss souls (dropped by a dead servant; the activation item of its revive ritual). Each is only
 * registered by its boss mod's compat class when that mod is installed, so the fields of missing
 * boss mods stay null.
 */
public final class ModItems {
    public static Item CAPTAIN_CORNELIA_SOUL;
    public static Item VOID_BLOSSOM_SOUL;
    public static Item NIGHT_LICH_SOUL;
    public static Item OLD_CHAMPION_SOUL;
    public static Item RETURNING_KNIGHT_SOUL;
    public static Item NETHERMAN_SOUL;

    private static final List<Item> SOULS = new ArrayList<>();

    private ModItems() {
    }

    public static Item registerSoul(String name) {
        Item soul = Registry.register(BuiltInRegistries.ITEM, GoetyBossServants.id(name), new BossSoulItem());
        SOULS.add(soul);
        return soul;
    }

    /** The souls registered (for the installed boss mods). */
    public static List<Item> souls() {
        return List.copyOf(SOULS);
    }

    /** Puts the registered souls in the Ingredients creative tab. */
    public static void addToCreativeTab() {
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.INGREDIENTS).register(entries -> SOULS.forEach(entries::accept));
    }
}
