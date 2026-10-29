package com.saints.goetybossservants.debug;

import com.saints.goetybossservants.GoetyBossServants;
import com.saints.goetybossservants.registry.ModRegistry;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraftforge.common.ForgeHooks;

import static com.saints.goetybossservants.debug.SelfTest.check;

/**
 * Forge's two parts of the wardens' Warden Core (see WardenCoreChecks, which checks what drops):
 * the pool in vanilla's warden table, and the loot modifier that tops up a warden table a datapack
 * replaced (which never gets the pool) to one core, and never adds a second.
 */
final class WardenCoreLootChecks {
    private WardenCoreLootChecks() {
    }

    static void run(ServerLevel level) {
        ResourceLocation warden = EntityType.WARDEN.getDefaultLootTable();
        check(level.getServer().getLootData().getLootTable(warden).getPool(GoetyBossServants.MOD_ID + ":warden_core") != null,
                "vanilla's warden loot table has the Warden Core pool");
        LootContext context = new LootContext.Builder(new LootParams.Builder(level).create(LootContextParamSets.EMPTY)).create(null);
        int topped = cores(ForgeHooks.modifyLoot(warden, new ObjectArrayList<>(), context));
        context = new LootContext.Builder(new LootParams.Builder(level).create(LootContextParamSets.EMPTY)).create(null);
        int kept = cores(ForgeHooks.modifyLoot(warden, ObjectArrayList.of(new ItemStack(ModRegistry.WARDEN_CORE_ITEM.get())), context));
        context = new LootContext.Builder(new LootParams.Builder(level).create(LootContextParamSets.EMPTY)).create(null);
        int other = cores(ForgeHooks.modifyLoot(EntityType.ZOMBIE.getDefaultLootTable(), new ObjectArrayList<>(), context));
        check(topped == 1 && kept == 1 && other == 0, "the Warden Core loot modifier gives a warden roll with none one core, adds none to "
                + "one that has it, and none to other tables (" + topped + ", " + kept + ", " + other + ")");
    }

    private static int cores(ObjectArrayList<ItemStack> loot) {
        return loot.stream().filter(stack -> stack.is(ModRegistry.WARDEN_CORE_ITEM.get())).mapToInt(ItemStack::getCount).sum();
    }
}
