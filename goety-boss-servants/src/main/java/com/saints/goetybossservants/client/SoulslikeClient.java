package com.saints.goetybossservants.client;

import com.saints.goetybossservants.compat.SoulslikeCompat;
import com.saints.goetybossservants.registry.ModEntities;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.EntityType;
import net.soulsweaponry.client.renderer.entity.mobs.DraugrBossRenderer;
import net.soulsweaponry.client.renderer.entity.mobs.ReturningKnightRenderer;
import net.soulsweaponry.entity.mobs.DraugrBoss;
import net.soulsweaponry.entity.mobs.ReturningKnight;

/**
 * Client side of the Old Champion and Returning Knight servants. Only touched when Soulslike
 * Weaponry is installed; see ModCompat.
 */
public final class SoulslikeClient {
    private SoulslikeClient() {
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void registerRenderers() {
        // Soulslike Weaponry's own renderers; the servants are a DraugrBoss and a ReturningKnight.
        EntityRendererRegistry.register((EntityType) ModEntities.OLD_CHAMPION, (EntityRendererProvider) (EntityRendererProvider<DraugrBoss>) DraugrBossRenderer::new);
        EntityRendererRegistry.register((EntityType) ModEntities.RETURNING_KNIGHT, (EntityRendererProvider) (EntityRendererProvider<ReturningKnight>) ReturningKnightRenderer::new);
        // A client joining a server needs the servants' attributes too (see SoulslikeCompat).
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> SoulslikeCompat.registerAttributes());
    }
}
