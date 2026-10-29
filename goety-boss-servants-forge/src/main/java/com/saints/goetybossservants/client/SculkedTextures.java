package com.saints.goetybossservants.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.saints.goetybossservants.GoetyBossServants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Sculk grown over any mob, for sculk zombies (SculkZombieRenderer). The mob is drawn as usual, then
 * drawn again through overlay(), which draws only its sculk: for every mob texture it was drawn
 * with, a mask made from it, with patches of sculk (the block's own pixels, at its size) over parts
 * of its visible pixels, keeping their transparency, and nothing anywhere else. So the sculk never
 * spreads past its outline or over its see-through parts, and isn't dyed with the layers that are
 * (a sheep's wool, a collar). The masks are made the first time they're needed, and again on a
 * resource reload.
 */
final class SculkedTextures {
    private static final ResourceLocation SCULK = new ResourceLocation("textures/block/sculk.png");
    // Every render type seen, and what it draws its sculk with (NONE: nothing). Weak, since a few
    // (a charged creeper's swirl) are made afresh every frame.
    private static final Map<RenderType, Object> MASKS = new WeakHashMap<>();
    private static final Object NONE = new Object();
    // Every texture seen, and its mask (empty when it isn't a texture file that can be copied).
    private static final Map<ResourceLocation, Optional<ResourceLocation>> TEXTURES = new HashMap<>();
    // The render types mobs' own textures are drawn with; the rest (glowing eyes and parts, armour,
    // items, glints, swirls, outlines) get no sculk.
    private static final Set<String> SKINS = Set.of("entity_cutout_no_cull", "entity_translucent", "entity_smooth_cutout", "entity_decal",
            "entity_no_outline", "entity_solid", "entity_cutout", "entity_translucent_cull", "item_entity_translucent_cull",
            "entity_cutout_no_cull_z_offset");
    private static final String NAME = "RenderType[";
    private static final String TEXTURE = "texture[Optional[";

    private SculkedTextures() {
    }

    /** {@code buffers}, but drawing only the sculk over what is drawn with them, in its own colours. */
    static MultiBufferSource overlay(MultiBufferSource buffers) {
        // (A buffer of its own each time, even one that draws nothing: some renderers join two buffers,
        // and can't join one to itself.)
        return type -> new Untinted(MASKS.computeIfAbsent(type, SculkedTextures::mask) instanceof RenderType mask ? buffers.getBuffer(mask) : null);
    }

    // What a render type draws its sculk with. Render types don't say what they are or draw with,
    // except in their names: "RenderType[entity_cutout_no_cull:CompositeState[[
    // texture[Optional[minecraft:textures/entity/pig/pig.png](blur=false, mipmap=false)], ...".
    private static Object mask(RenderType type) {
        String name = type.toString();
        int colon = name.indexOf(':');
        if (!name.startsWith(NAME) || colon < 0) {
            return NONE;
        }
        String kind = name.substring(NAME.length(), colon);
        int start = name.indexOf(TEXTURE);
        int end = start < 0 ? -1 : name.indexOf(']', start);
        if (!SKINS.contains(kind) || end < 0) {
            return NONE;
        }
        ResourceLocation source = ResourceLocation.tryParse(name.substring(start + TEXTURE.length(), end));
        Optional<ResourceLocation> mask = source == null ? Optional.empty() : texture(source);
        if (mask.isEmpty()) {
            return NONE;
        }
        // A shulker's is drawn a little in front of the rest, so its sculk is too (see-through only where
        // there is none: its stain is solid).
        if (kind.equals("entity_cutout_no_cull_z_offset")) {
            return RenderType.entityCutoutNoCullZOffset(mask.get(), false);
        }
        // Drawn from both sides (the faces it covers from behind are hidden behind the mob anyway), and
        // with the hurt flash, which the culled translucent type has none of.
        return RenderType.entityTranslucent(mask.get(), false);
    }

    // The mask of a texture, made (and registered) the first time it's asked for.
    private static Optional<ResourceLocation> texture(ResourceLocation source) {
        return TEXTURES.computeIfAbsent(source, key -> {
            Minecraft minecraft = Minecraft.getInstance();
            Optional<Resource> resource = minecraft.getResourceManager().getResource(key);
            try {
                // Not a texture file (a player skin, a texture made in game), or an animated one (which
                // some mods draw a frame at a time): left without sculk.
                if (resource.isEmpty() || resource.get().metadata().getSection(AnimationMetadataSection.SERIALIZER).isPresent()) {
                    return Optional.empty();
                }
            } catch (IOException | RuntimeException e) {
                return Optional.empty();
            }
            ResourceLocation id = new ResourceLocation(GoetyBossServants.MOD_ID, "sculked/" + key.getNamespace() + "/" + key.getPath());
            minecraft.getTextureManager().register(id, new Mask(key));
            return Optional.of(id);
        });
    }

    /** The sculk mask of a mob texture. */
    private static class Mask extends SimpleTexture {
        Mask(ResourceLocation source) {
            super(source);
        }

        @Override
        protected @NotNull TextureImage getTextureImage(@NotNull ResourceManager manager) {
            TextureImage original = super.getTextureImage(manager);
            NativeImage image = null;
            try {
                image = original.getImage();
                paint(image, sculk(manager), this.location.toString().hashCode());
                return new TextureImage(original.getTextureMetadata(), image);
            } catch (IOException | RuntimeException e) {
                if (image != null) {
                    image.close();
                }
                // No sculk on it, rather than the missing texture's checkers all over the mob.
                return new TextureImage(null, new NativeImage(1, 1, true));
            }
        }
    }

    // The sculk block's pixels (its first frame, however big a resource pack makes it), as NativeImage
    // colours: a square of side pixels[0].length.
    private static int[][] sculk(ResourceManager manager) throws IOException {
        try (InputStream in = manager.open(SCULK); NativeImage block = NativeImage.read(in)) {
            int size = Math.max(1, Math.min(block.getWidth(), block.getHeight()));
            int[][] pixels = new int[size][size];
            for (int y = 0; y < size; ++y) {
                for (int x = 0; x < size; ++x) {
                    pixels[y][x] = block.getPixelRGBA(x, y);
                }
            }
            return pixels;
        }
    }

    // Patches of sculk (blotches of smooth noise, a different pattern for each texture) over the
    // image's visible pixels, each pixel the sculk block's own at that spot, and round the edges of
    // the patches a dark stain, as if the sculk were spreading; everything else see-through.
    private static void paint(NativeImage image, int[][] sculk, int seed) {
        int cell = Math.max(4, Math.min(image.getWidth(), image.getHeight()) / 10);
        int size = sculk.length;
        for (int y = 0; y < image.getHeight(); ++y) {
            for (int x = 0; x < image.getWidth(); ++x) {
                int alpha = image.getPixelRGBA(x, y) >>> 24;
                double cover = smooth(x, y, cell, seed) * 0.75 + smooth(x, y, Math.max(2, cell / 3), seed + 1) * 0.25;
                int mask = 0;
                if (alpha > 0 && cover > 0.56) {
                    mask = sculk[y % size][x % size] & 0xFFFFFF | alpha << 24;
                } else if (alpha > 0 && cover > 0.5) {
                    mask = 0x2A1F0A | (int) (alpha * (cover - 0.5) / 0.06 * 0.6) << 24;
                }
                image.setPixelRGBA(x, y, mask);
            }
        }
    }

    private static double noise(int x, int y, int seed) {
        long h = x * 374761393L + y * 668265263L + seed * 2147483647L;
        h = (h ^ (h >>> 13)) * 1274126177L;
        h ^= h >>> 16;
        return (h & 0xFFFFFF) / (double) 0x1000000;
    }

    // Value noise smoothed between cells of the given size.
    private static double smooth(int x, int y, int cell, int seed) {
        double fx = x / (double) cell, fy = y / (double) cell;
        int ix = (int) Math.floor(fx), iy = (int) Math.floor(fy);
        double tx = fx - ix, ty = fy - iy;
        tx = tx * tx * (3 - 2 * tx);
        ty = ty * ty * (3 - 2 * ty);
        double top = noise(ix, iy, seed) + (noise(ix + 1, iy, seed) - noise(ix, iy, seed)) * tx;
        double bottom = noise(ix, iy + 1, seed) + (noise(ix + 1, iy + 1, seed) - noise(ix, iy + 1, seed)) * tx;
        return top + (bottom - top) * ty;
    }

    /**
     * Passes vertices on in white (whatever colour a layer is drawn in: the sculk keeps its own),
     * keeping their transparency, light and hurt flash; with no buffer, draws nothing.
     */
    private record Untinted(VertexConsumer buffer) implements VertexConsumer {
        @Override
        public @NotNull VertexConsumer vertex(double x, double y, double z) {
            if (this.buffer != null) {
                this.buffer.vertex(x, y, z);
            }
            return this;
        }

        @Override
        public @NotNull VertexConsumer color(int red, int green, int blue, int alpha) {
            if (this.buffer != null) {
                this.buffer.color(255, 255, 255, alpha);
            }
            return this;
        }

        @Override
        public @NotNull VertexConsumer uv(float u, float v) {
            if (this.buffer != null) {
                this.buffer.uv(u, v);
            }
            return this;
        }

        @Override
        public @NotNull VertexConsumer overlayCoords(int u, int v) {
            if (this.buffer != null) {
                this.buffer.overlayCoords(u, v);
            }
            return this;
        }

        @Override
        public @NotNull VertexConsumer uv2(int u, int v) {
            if (this.buffer != null) {
                this.buffer.uv2(u, v);
            }
            return this;
        }

        @Override
        public @NotNull VertexConsumer normal(float x, float y, float z) {
            if (this.buffer != null) {
                this.buffer.normal(x, y, z);
            }
            return this;
        }

        @Override
        public void endVertex() {
            if (this.buffer != null) {
                this.buffer.endVertex();
            }
        }

        @Override
        public void defaultColor(int red, int green, int blue, int alpha) {
            if (this.buffer != null) {
                this.buffer.defaultColor(255, 255, 255, alpha);
            }
        }

        @Override
        public void unsetDefaultColor() {
            if (this.buffer != null) {
                this.buffer.unsetDefaultColor();
            }
        }
    }
}
