package com.saints.goetybossservants.debug;

import net.minecraft.server.level.ServerLevel;

/**
 * The loader's own parts of the wardens' Warden Core (see WardenCoreChecks, which checks what
 * drops). On Fabric there's only the pool LootTableEvents.MODIFY adds to every warden table,
 * datapack-replaced ones included, and what it drops is checked there; nothing more to check here.
 */
final class WardenCoreLootChecks {
    private WardenCoreLootChecks() {
    }

    static void run(ServerLevel level) {
    }
}
