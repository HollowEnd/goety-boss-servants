package com.saints.goetybossservants.mixin;

import dev.xylonity.knightquest.common.entity.boss.NethermanEntity;
import net.minecraft.server.level.ServerBossEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The Netherman's boss bar, so the servant can hide it (NethermanServant). */
@Mixin(NethermanEntity.class)
public interface NethermanAccessor {
    @Accessor("bossInfo")
    ServerBossEvent goetybossservants$getBossInfo();
}
