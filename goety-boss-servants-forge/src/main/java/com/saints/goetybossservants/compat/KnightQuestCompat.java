package com.saints.goetybossservants.compat;

import com.saints.goetybossservants.entity.knightquest.NethermanServant;
import com.saints.goetybossservants.entity.servant.ServantMinions;
import com.saints.goetybossservants.registry.ModRegistry;
import dev.xylonity.knightquest.common.entity.boss.NethermanCloneEntity;
import dev.xylonity.knightquest.common.entity.boss.NethermanProjectileChargeEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** The Netherman servant (Knights Quest). Only touched when Knights Quest is installed; see ModCompat. */
public final class KnightQuestCompat {
    private KnightQuestCompat() {
    }

    public static void registerEvents() {
        MinecraftForge.EVENT_BUS.register(KnightQuestCompat.class);
    }

    public static void registerAttributes(EntityAttributeCreationEvent event) {
        // Knights Quest's netherman attributes (its Forge version only hands out the finished set, so
        // they're repeated here); follow range 32 like the other servants. Its health and damage come
        // from Knights Quest's config, applied when it's added to the world.
        event.put(ModRegistry.NETHERMAN.get(), Monster.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 450.0)
                .add(Attributes.ATTACK_DAMAGE, 16.0)
                .add(Attributes.ATTACK_SPEED, 1.2)
                .add(Attributes.MOVEMENT_SPEED, 0.8)
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 5.0)
                .build());
    }

    /** Clones and explosive orbs a Netherman servant's goals spawn become its minions. */
    @SubscribeEvent
    public static void onJoinLevel(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();
        NethermanServant netherman = NethermanServant.summoning;
        if (netherman == null || event.getLevel().isClientSide || ServantMinions.isMinion(entity)
                || !(entity instanceof NethermanCloneEntity || entity instanceof NethermanProjectileChargeEntity)) {
            return;
        }
        if (!netherman.claimMinion(entity)) {
            event.setCanceled(true);
        }
    }
}
