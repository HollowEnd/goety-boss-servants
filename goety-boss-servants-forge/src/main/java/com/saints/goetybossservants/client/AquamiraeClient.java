package com.saints.goetybossservants.client;

import dev.obscuria.aquamirae.client.renderer.entity.CaptainCorneliaRenderer;
import dev.obscuria.aquamirae.common.entity.CaptainCorneliaBoss;
import com.saints.goetybossservants.registry.ModRegistry;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraftforge.client.event.EntityRenderersEvent;

/** Client side of the Captain Cornelia servant. Only touched when Aquamirae is installed; see ModCompat. */
public final class AquamiraeClient {
    private AquamiraeClient() {
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        EntityRendererProvider<CaptainCorneliaBoss> corneliaRenderer = CaptainCorneliaRenderer::new;
        event.registerEntityRenderer(ModRegistry.CAPTAIN_CORNELIA.get(), (EntityRendererProvider) corneliaRenderer);
    }
}
