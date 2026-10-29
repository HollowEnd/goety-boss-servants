/*
 * Parts of this file are adapted from Bosses of Mass Destruction (Forge port by CerbonXD, original
 * Fabric mod by Barribob), licensed under the GNU Lesser General Public License v3.0.
 * Modified for Goety Boss Servants: the lich and void blossom renderer setup from BMDEntities,
 * applied to the servants (the lich without its eternal-night renderer).
 * Modified by Saints, 2026.
 */
package com.saints.goetybossservants.client;

import com.cerbon.bosses_of_mass_destruction.client.render.CompositeRenderer;
import com.cerbon.bosses_of_mass_destruction.entity.GeoModel;
import com.cerbon.bosses_of_mass_destruction.entity.custom.lich.BoundedLighting;
import com.cerbon.bosses_of_mass_destruction.entity.custom.lich.LichBoneLight;
import com.cerbon.bosses_of_mass_destruction.entity.custom.lich.LichCodeAnimations;
import com.cerbon.bosses_of_mass_destruction.entity.custom.lich.LichEntity;
import com.cerbon.bosses_of_mass_destruction.entity.custom.void_blossom.NoRedOnDeathOverlay;
import com.cerbon.bosses_of_mass_destruction.entity.custom.void_blossom.VoidBlossomBoneLight;
import com.cerbon.bosses_of_mass_destruction.entity.custom.void_blossom.VoidBlossomCodeAnimations;
import com.cerbon.bosses_of_mass_destruction.entity.custom.void_blossom.VoidBlossomEntity;
import com.cerbon.bosses_of_mass_destruction.entity.custom.void_blossom.VoidBlossomSpikeRenderer;
import com.cerbon.bosses_of_mass_destruction.entity.util.SimpleLivingGeoRenderer;
import com.saints.goetybossservants.registry.ModRegistry;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.EntityRenderersEvent;

/**
 * Client side of the Void Blossom and Night Lich servants. Only touched when Bosses of Mass
 * Destruction is installed; see ModCompat.
 */
public final class BmdClient {
    private BmdClient() {
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModRegistry.VOID_BLOSSOM.get(), (EntityRendererProvider) (EntityRendererProvider<VoidBlossomEntity>) BmdClient::voidBlossomRenderer);
        event.registerEntityRenderer(ModRegistry.NIGHT_LICH.get(), (EntityRendererProvider) (EntityRendererProvider<LichEntity>) BmdClient::nightLichRenderer);
    }

    /** BMD builds its Void Blossom renderer in a lambda (BMDEntities); the same renderer, for the servant. */
    private static EntityRenderer<VoidBlossomEntity> voidBlossomRenderer(EntityRendererProvider.Context context) {
        ResourceLocation texture = new ResourceLocation("bosses_of_mass_destruction", "textures/entity/void_blossom.png");
        GeoModel<VoidBlossomEntity> model = new GeoModel<>(entity -> new ResourceLocation("bosses_of_mass_destruction", "geo/void_blossom.geo.json"), entity -> texture,
                new ResourceLocation("bosses_of_mass_destruction", "animations/void_blossom.animation.json"), new VoidBlossomCodeAnimations(), RenderType::entityCutout);
        VoidBlossomBoneLight boneLight = new VoidBlossomBoneLight();
        NoRedOnDeathOverlay overlay = new NoRedOnDeathOverlay();
        return new SimpleLivingGeoRenderer<>(context, model, null, boneLight, new CompositeRenderer(new VoidBlossomSpikeRenderer(), boneLight, overlay), null, overlay, false);
    }

    /**
     * BMD's lich renderer (BMDEntities), for the servant, minus its EternalNightRenderer: that piece
     * forces night on the client, and the servant doesn't bring eternal night.
     */
    private static EntityRenderer<LichEntity> nightLichRenderer(EntityRendererProvider.Context context) {
        ResourceLocation texture = new ResourceLocation("bosses_of_mass_destruction", "textures/entity/lich.png");
        GeoModel<LichEntity> model = new GeoModel<>(entity -> new ResourceLocation("bosses_of_mass_destruction", "geo/lich.geo.json"), entity -> texture,
                new ResourceLocation("bosses_of_mass_destruction", "animations/lich.animation.json"), new LichCodeAnimations(), RenderType::entityCutoutNoCull);
        return new SimpleLivingGeoRenderer<>(context, model, new BoundedLighting(5), new LichBoneLight(), null, null, null, true);
    }
}
