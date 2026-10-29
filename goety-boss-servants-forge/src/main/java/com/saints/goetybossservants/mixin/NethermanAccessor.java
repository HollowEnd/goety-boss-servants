package com.saints.goetybossservants.mixin;

import dev.xylonity.knightquest.common.entity.boss.NethermanEntity;
import net.minecraft.server.level.ServerBossEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Knights Quest's own field, so no remapping is needed. */
@Mixin(value = NethermanEntity.class, remap = false)
public interface NethermanAccessor {
    @Accessor("bossInfo")
    ServerBossEvent goetybossservants$getBossInfo();
}
