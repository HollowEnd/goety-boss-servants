package com.saints.goetybossservants.client;

import com.saints.goetybossservants.registry.ModEntities;
import dev.obscuria.aquamirae.client.renderer.entity.CaptainCorneliaRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.EntityType;

/** Client side of the Captain Cornelia servant. Only touched when Aquamirae is installed; see ModCompat. */
public final class AquamiraeClient {
    private AquamiraeClient() {
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void registerRenderers() {
        // The servant is a CaptainCorneliaBoss, so Aquamirae's own renderer (model, weapon layer,
        // glowing overlay) draws it as-is. The raw cast only bridges the generic type.
        EntityRendererProvider provider = CaptainCorneliaRenderer::new;
        EntityRendererRegistry.register((EntityType) ModEntities.CAPTAIN_CORNELIA, provider);
    }
}
