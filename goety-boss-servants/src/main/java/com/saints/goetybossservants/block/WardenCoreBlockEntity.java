package com.saints.goetybossservants.block;

import com.saints.goetybossservants.registry.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Holds no data: it's there for the Warden Core's heartbeat (its client ticker and WardenCoreRenderer). */
public class WardenCoreBlockEntity extends BlockEntity {
    public WardenCoreBlockEntity(BlockPos pos, BlockState state) {
        super(ModRegistry.WARDEN_CORE_BE, pos, state);
    }
}
