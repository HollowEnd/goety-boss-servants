package com.saints.goetybossservants.compat;

import com.Polarice3.Goety.common.items.ModItems;
import com.jerotes.meowhem.entity.Boss.MiraMonstrosityEntity;
import com.saints.goetybossservants.construct.MaterialMonstrosityMold;
import com.saints.goetybossservants.construct.MiraMonstrosityMold;
import com.saints.goetybossservants.entity.MiraVariant;
import com.saints.goetybossservants.registry.ModRegistry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** The Mira Monstrosity servant (meowhem). Only touched when meowhem is installed; see ModCompat. */
public final class MeowhemCompat {
    private MeowhemCompat() {
    }

    public static void registerEvents() {
        MinecraftForge.EVENT_BUS.register(MeowhemCompat.class);
    }

    public static void registerAttributes(EntityAttributeCreationEvent event) {
        // Same base attributes as the boss; the servant config overrides health/damage/armor per entity.
        event.put(ModRegistry.MIRA_MONSTROSITY.get(), MiraMonstrosityEntity.createAttributes().build());
    }

    /**
     * The Mira molds share the Redstone Monstrosity's core block, and Goety only lets one mold own a
     * core block. So the Animation Core is intercepted here first: if a complete Mira mold surrounds
     * the core we handle it, otherwise Goety's own Animation Core logic runs as normal (spectators
     * and adventure mode are kept out as before Goety's useOn, see MaterialMonstrosityMold.mayActivate).
     */
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getItemStack().is(ModItems.ANIMATION_CORE.get()) || event.getUseItem() == Event.Result.DENY
                || !MaterialMonstrosityMold.mayActivate(event.getEntity(), event.getItemStack(), event.getLevel(), event.getPos())) {
            return;
        }
        MiraVariant variant = MiraMonstrosityMold.findVariant(event.getLevel(), event.getPos());
        if (variant == null) {
            return;
        }
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            MiraMonstrosityMold.spawnServant(serverLevel, event.getPos(), variant, event.getEntity(), event.getItemStack());
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));
    }
}
