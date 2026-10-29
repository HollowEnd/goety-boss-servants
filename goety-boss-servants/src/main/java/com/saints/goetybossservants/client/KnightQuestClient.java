package com.saints.goetybossservants.client;

import com.saints.goetybossservants.registry.ModEntities;
import dev.xylonity.knightquest.client.entity.renderer.NethermanRenderer;
import dev.xylonity.knightquest.common.entity.boss.NethermanEntity;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.EntityType;

/** Client side of the Netherman servant. Only touched when Knights Quest is installed; see ModCompat. */
public final class KnightQuestClient {
    private KnightQuestClient() {
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void registerRenderers() {
        // The servant is a NethermanEntity, so Knights Quest's own renderer (three forms, transformation
        // flipbooks, glow) draws it as-is. The raw cast only bridges the generic type.
        EntityRendererRegistry.register((EntityType) ModEntities.NETHERMAN, (EntityRendererProvider) (EntityRendererProvider<NethermanEntity>) NethermanRenderer::new);
    }
}
