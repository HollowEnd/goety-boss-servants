package com.saints.goetybossservants.mixin;

import com.jerotes.jerotes.event.JerotesBossEvent;
import com.jerotes.meowhem.entity.Boss.MiraMonstrosityEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Lets the servant take players back off the boss bar the boss adds them to. */
@Mixin(value = MiraMonstrosityEntity.class, remap = false)
public interface MiraMonstrosityAccessor {
    @Accessor("bossEvent")
    JerotesBossEvent goetybossservants$getBossEvent();
}
