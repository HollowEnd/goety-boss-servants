package com.saints.goetybossservants.compat;

import com.saints.goetybossservants.entity.servant.ServantMinions;
import com.saints.goetybossservants.entity.soulslike.OldChampionServant;
import com.saints.goetybossservants.entity.soulslike.ReturningKnightServant;
import com.saints.goetybossservants.event.ServantEvents;
import com.saints.goetybossservants.registry.ModEntities;
import com.saints.goetybossservants.registry.ModItems;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.soulsweaponry.entity.mobs.DarkSorcerer;
import net.soulsweaponry.entity.mobs.DraugrBoss;
import net.soulsweaponry.entity.mobs.NightShade;
import net.soulsweaponry.entity.mobs.Remnant;
import net.soulsweaponry.entity.mobs.ReturningKnight;

/**
 * The Old Champion and Returning Knight servants (Marium's Soulslike Weaponry). Only touched when
 * Soulslike Weaponry is installed; see ModCompat.
 */
public final class SoulslikeCompat {
    private static boolean attributesRegistered;

    private SoulslikeCompat() {
    }

    public static void register() {
        // Soulslike Weaponry registers the draugr boss as 1.5 x 3.0 and the Returning Knight as 3.0 x 8.0.
        ModEntities.OLD_CHAMPION = ModEntities.register("draugr_boss_servant",
                EntityType.Builder.<OldChampionServant>of(OldChampionServant::new, MobCategory.MISC).sized(1.5f, 3.0f).clientTrackingRange(10));
        ModEntities.RETURNING_KNIGHT = ModEntities.register("returning_knight_servant",
                EntityType.Builder.<ReturningKnightServant>of(ReturningKnightServant::new, MobCategory.MISC).sized(3.0f, 8.0f).clientTrackingRange(10));
        ModItems.OLD_CHAMPION_SOUL = ModItems.registerSoul("old_champion_soul");
        ModItems.RETURNING_KNIGHT_SOUL = ModItems.registerSoul("returning_knight_soul");
        // The boss attributes come from Soulslike Weaponry's config, which is loaded with that mod.
        ServerLifecycleEvents.SERVER_STARTING.register(server -> registerAttributes());
        ServantEvents.SPAWN_FILTERS.add(SoulslikeCompat::allowSpawn);
    }

    /** Soulslike Weaponry's boss attributes; follow range 32 like the other servants. Also called by the client. */
    @SuppressWarnings("unchecked")
    public static void registerAttributes() {
        if (attributesRegistered) {
            return;
        }
        attributesRegistered = true;
        FabricDefaultAttributeRegistry.register((EntityType<? extends LivingEntity>) ModEntities.OLD_CHAMPION,
                DraugrBoss.createBossAttributes().add(Attributes.FOLLOW_RANGE, 32.0));
        FabricDefaultAttributeRegistry.register((EntityType<? extends LivingEntity>) ModEntities.RETURNING_KNIGHT,
                ReturningKnight.createBossAttributes().add(Attributes.FOLLOW_RANGE, 32.0));
    }

    private static boolean allowSpawn(Entity entity) {
        if (entity.level().isClientSide) {
            return true;
        }
        // The boss rises again as the Night Shade boss when the Old Champion dies; the servant doesn't.
        if (OldChampionServant.dying != null && entity instanceof NightShade) {
            return false;
        }
        // Remnants and Dark Sorcerers the Returning Knight servant's goal spawns become its minions.
        if (entity instanceof Mob mob && (mob instanceof Remnant || mob instanceof DarkSorcerer)) {
            ReturningKnightServant knight = ReturningKnightServant.summoning;
            if (knight != null && !ServantMinions.isMinion(mob) && !knight.claimMinion(mob)) {
                return false;
            }
            if (ServantMinions.isMinion(mob)) {
                ReturningKnightServant.adjustMinionGoals(mob);
            }
        }
        return true;
    }
}
