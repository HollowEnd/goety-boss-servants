package com.saints.goetybossservants.client;

import com.saints.goetybossservants.registry.ModRegistry;
import dev.xylonity.knightquest.client.entity.renderer.NethermanRenderer;
import dev.xylonity.knightquest.common.entity.boss.NethermanEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraftforge.client.event.EntityRenderersEvent;

/** Client side of the Netherman servant. Only touched when Knights Quest is installed; see ModCompat. */
public final class KnightQuestClient {
    private KnightQuestClient() {
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // The servant is a NethermanEntity, so Knights Quest's own renderer (three forms, transformation
        // flipbooks, glow) draws it as-is. The raw cast only bridges the generic type.
        event.registerEntityRenderer(ModRegistry.NETHERMAN.get(), (EntityRendererProvider) (EntityRendererProvider<NethermanEntity>) NethermanRenderer::new);
    }
}
