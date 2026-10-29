/*
 * Parts of this file are adapted from Bosses of Mass Destruction by Barribob, licensed under the
 * GNU Lesser General Public License v3.0.
 * Modified for Goety Boss Servants: the lich and void blossom renderer setup from Entities,
 * applied to the servants (the lich without its eternal-night renderer).
 * Modified by Saints, 2026.
 */
package com.saints.goetybossservants.client;

import com.saints.goetybossservants.compat.BmdCompat;
import com.saints.goetybossservants.registry.ModEntities;
import net.barribob.boss.Mod;
import net.barribob.boss.mob.GeoModel;
import net.barribob.boss.mob.mobs.lich.BoundedLighting;
import net.barribob.boss.mob.mobs.lich.LichBoneLight;
import net.barribob.boss.mob.mobs.lich.LichCodeAnimations;
import net.barribob.boss.mob.mobs.lich.LichEntity;
import net.barribob.boss.mob.mobs.void_blossom.NoRedOnDeathOverlay;
import net.barribob.boss.mob.mobs.void_blossom.VoidBlossomBoneLight;
import net.barribob.boss.mob.mobs.void_blossom.VoidBlossomCodeAnimations;
import net.barribob.boss.mob.mobs.void_blossom.VoidBlossomEntity;
import net.barribob.boss.mob.mobs.void_blossom.VoidBlossomSpikeRenderer;
import net.barribob.boss.mob.utils.SimpleLivingGeoRenderer;
import net.barribob.boss.render.CompositeRenderer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;

/**
 * Client side of the Void Blossom and Night Lich servants. Only touched when Bosses of Mass
 * Destruction is installed; see ModCompat.
 */
public final class BmdClient {
    private BmdClient() {
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void registerRenderers() {
        EntityRendererRegistry.register((EntityType) ModEntities.VOID_BLOSSOM, (EntityRendererProvider) (EntityRendererProvider<VoidBlossomEntity>) BmdClient::voidBlossomRenderer);
        EntityRendererRegistry.register((EntityType) ModEntities.NIGHT_LICH, (EntityRendererProvider) (EntityRendererProvider<LichEntity>) BmdClient::nightLichRenderer);
        // A client joining a server needs the servants' attributes too (see BmdCompat).
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> BmdCompat.registerAttributes());
    }

    /** BMD builds its Void Blossom renderer in a lambda (Entities); the same renderer, for the servant. */
    private static EntityRenderer<VoidBlossomEntity> voidBlossomRenderer(EntityRendererProvider.Context context) {
        ResourceLocation texture = Mod.INSTANCE.identifier("textures/entity/void_blossom.png");
        GeoModel<VoidBlossomEntity> model = new GeoModel<>(entity -> Mod.INSTANCE.identifier("geo/void_blossom.geo.json"), entity -> texture,
                Mod.INSTANCE.identifier("animations/void_blossom.animation.json"), new VoidBlossomCodeAnimations(), RenderType::entityCutout);
        VoidBlossomBoneLight boneLight = new VoidBlossomBoneLight();
        NoRedOnDeathOverlay overlay = new NoRedOnDeathOverlay();
        return new SimpleLivingGeoRenderer<>(context, model, null, boneLight, new CompositeRenderer<>(new VoidBlossomSpikeRenderer(), boneLight, overlay), null, overlay, false);
    }

    /**
     * BMD's lich renderer (Entities), for the servant, minus its EternalNightRenderer: that piece
     * forces night on the client, and the servant doesn't bring eternal night.
     */
    private static EntityRenderer<LichEntity> nightLichRenderer(EntityRendererProvider.Context context) {
        ResourceLocation texture = Mod.INSTANCE.identifier("textures/entity/lich.png");
        GeoModel<LichEntity> model = new GeoModel<>(entity -> Mod.INSTANCE.identifier("geo/lich.geo.json"), entity -> texture,
                Mod.INSTANCE.identifier("animations/lich.animation.json"), new LichCodeAnimations(), RenderType::entityCutoutNoCull);
        return new SimpleLivingGeoRenderer<>(context, model, new BoundedLighting(5), new LichBoneLight(), null, null, null, false);
    }
}
