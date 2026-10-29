package com.saints.goetybossservants.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import com.saints.goetybossservants.client.compat.AstemirPuppets;
import com.saints.goetybossservants.compat.ModCompat;
import com.saints.goetybossservants.entity.monstrosity.SculkZombie;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.Set;

/**
 * A sculk zombie, looking like the mob it was: its copy of that mob (see SculkPuppets) is posed like
 * the zombie and drawn by its own renderer, then drawn again in only the sculk grown over parts of it
 * (SculkedTextures). While it rises, it climbs up out of the ground.
 */
public class SculkZombieRenderer extends EntityRenderer<SculkZombie> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final boolean ASTEMIRLIB = ModCompat.isLoaded(ModCompat.ASTEMIRLIB);
    // Mobs whose copies may be drawn frozen (logged once each).
    private static final Set<EntityType<?>> UNPOSED = new HashSet<>();

    public SculkZombieRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.5f;
    }

    @Override
    public void render(@NotNull SculkZombie zombie, float yaw, float partialTicks, @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffers, int light) {
        LivingEntity puppet = SculkPuppets.puppet(zombie);
        EntityRenderer<? super LivingEntity> renderer = puppet == null ? null : this.entityRenderDispatcher.getRenderer(puppet);
        if (renderer != null && (Object) renderer != this) {
            SculkPuppets.pose(zombie, puppet);
            // A mob a library poses some other way than its renderer (AstemirLib's), posed for it. Any other
            // renderer that isn't a living one may draw it frozen, which is worth knowing.
            if (!(ASTEMIRLIB && AstemirPuppets.pose(renderer, puppet, partialTicks)) && !(renderer instanceof LivingEntityRenderer) && UNPOSED.add(puppet.getType())) {
                LOGGER.info("Sculk zombies looking like {} may not move: its renderer isn't one this mod knows how to pose", EntityType.getKey(puppet.getType()));
            }
            float risen = zombie.risen(partialTicks);
            this.shadowRadius = zombie.getBbWidth() * 0.5f;
            this.shadowStrength = risen;
            // Its own pose stack, so a renderer that fails part way can't leave the world's unbalanced.
            PoseStack own = new PoseStack();
            own.last().pose().set(poseStack.last().pose());
            own.last().normal().set(poseStack.last().normal());
            own.translate(0.0f, -(1.0f - risen) * zombie.getBbHeight(), 0.0f);
            Vec3 offset = renderer.getRenderOffset(puppet, partialTicks);
            own.translate(offset.x, offset.y, offset.z);
            try {
                // The mob as it was, then its sculk over it.
                renderer.render(puppet, yaw, partialTicks, own, buffers, light);
                renderer.render(puppet, yaw, partialTicks, own, SculkedTextures.overlay(buffers), light);
            } catch (RuntimeException e) {
                // Its renderer can't draw a mob that isn't in the world: it looks like a zombie from now on.
                zombie.puppetFailed = true;
                zombie.puppet = SculkPuppets.create(zombie, EntityType.ZOMBIE, new CompoundTag());
            }
        }
        super.render(zombie, yaw, partialTicks, poseStack, buffers, light);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull SculkZombie zombie) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
