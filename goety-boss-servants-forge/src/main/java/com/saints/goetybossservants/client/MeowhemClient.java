package com.saints.goetybossservants.client;

import com.jerotes.meowhem.client.renderer.MiraMonstrosityRenderer;
import com.jerotes.meowhem.entity.Boss.MiraMonstrosityEntity;
import com.saints.goetybossservants.entity.MiraMonstrosityServant;
import com.saints.goetybossservants.registry.ModRegistry;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraftforge.client.event.EntityRenderersEvent;

/** Client side of the Mira Monstrosity servant. Only touched when meowhem is installed; see ModCompat. */
public final class MeowhemClient {
    private MeowhemClient() {
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // The servant is a MiraMonstrosityEntity, so meowhem's own renderer (model, animations, glow
        // layers, variant textures) draws it as-is. The raw cast only bridges the generic type.
        EntityRendererProvider<MiraMonstrosityEntity> meowhemRenderer = MiraMonstrosityRenderer::new;
        EntityRendererProvider<MiraMonstrosityServant> provider = (EntityRendererProvider) meowhemRenderer;
        event.registerEntityRenderer(ModRegistry.MIRA_MONSTROSITY.get(), provider);
        event.registerBlockEntityRenderer(ModRegistry.MIRA_MONSTROSITY_HEAD_BE.get(), MiraHeadBlockRenderer::new);
    }
}
