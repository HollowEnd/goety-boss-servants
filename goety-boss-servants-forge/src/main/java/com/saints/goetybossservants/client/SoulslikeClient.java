package com.saints.goetybossservants.client;

import com.saints.goetybossservants.registry.ModRegistry;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraftforge.client.event.EntityRenderersEvent;
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
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // Soulslike Weaponry's own renderers; the servants are a DraugrBoss and a ReturningKnight.
        event.registerEntityRenderer(ModRegistry.OLD_CHAMPION.get(), (EntityRendererProvider) (EntityRendererProvider<DraugrBoss>) DraugrBossRenderer::new);
        event.registerEntityRenderer(ModRegistry.RETURNING_KNIGHT.get(), (EntityRendererProvider) (EntityRendererProvider<ReturningKnight>) ReturningKnightRenderer::new);
    }
}
