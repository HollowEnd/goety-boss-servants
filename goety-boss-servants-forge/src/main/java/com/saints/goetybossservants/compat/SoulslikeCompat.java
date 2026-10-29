package com.saints.goetybossservants.compat;

import com.saints.goetybossservants.entity.servant.ServantMinions;
import com.saints.goetybossservants.entity.soulslike.OldChampionServant;
import com.saints.goetybossservants.entity.soulslike.ReturningKnightServant;
import com.saints.goetybossservants.registry.ModRegistry;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
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
    private SoulslikeCompat() {
    }

    public static void registerEvents() {
        MinecraftForge.EVENT_BUS.register(SoulslikeCompat.class);
    }

    public static void registerAttributes(EntityAttributeCreationEvent event) {
        // Soulslike Weaponry's boss attributes (from its own config); follow range 32 like the other servants.
        event.put(ModRegistry.OLD_CHAMPION.get(), DraugrBoss.createBossAttributes().add(Attributes.FOLLOW_RANGE, 32.0).build());
        event.put(ModRegistry.RETURNING_KNIGHT.get(), ReturningKnight.createBossAttributes().add(Attributes.FOLLOW_RANGE, 32.0).build());
    }

    @SubscribeEvent
    public static void onJoinLevel(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();
        if (event.getLevel().isClientSide) {
            return;
        }
        // The boss rises again as the Night Shade boss when the Old Champion dies; the servant doesn't.
        if (OldChampionServant.dying != null && entity instanceof NightShade) {
            event.setCanceled(true);
            return;
        }
        // Remnants and Dark Sorcerers the Returning Knight servant's goal spawns become its minions.
        if (entity instanceof Mob mob && (mob instanceof Remnant || mob instanceof DarkSorcerer)) {
            ReturningKnightServant knight = ReturningKnightServant.summoning;
            if (knight != null && !ServantMinions.isMinion(mob) && !knight.claimMinion(mob)) {
                event.setCanceled(true);
                return;
            }
            if (ServantMinions.isMinion(mob)) {
                ReturningKnightServant.adjustMinionGoals(mob);
            }
        }
    }
}
