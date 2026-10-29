import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds the material monstrosity skins. Goety's Redstone Monstrosity textures (MIT) give the UV
 * layout and where the energy channels run; the surfaces are the vanilla block of each material
 * (diamond block, lapis block, ...), tiled from the middle of every model face so the seams sit
 * evenly, with panel edges and a motif per material: copper patina, iron rivets and rust, gold
 * engraving and rubies, lapis gold trim and enchanting glyphs, diamond sparkles, netherite forged
 * into bevelled war plate with gilded corner brackets. Goety's cracks are left out. Eyes get
 * their own colour so they stand out.
 *
 * The Iron Monstrosity's channels are running water, the Lapis one's a starfield, the Diamond
 * one's cut crystal and the Netherite one's molten netherite, all animated: their glowing layer
 * comes as numbered frames (see MaterialMonstrosityRenderer).
 *
 * Also draws each material's texture for its extra model pieces (crown, cape, crystals, ...) and
 * the 18x18 effect icons.
 *
 * Usage: java MonstrositySkins.java <goety redstone_monstrosity texture dir> <vanilla textures dir
 * (assets/minecraft/textures)> <resources/assets/goetybossservants>
 */
public class MonstrositySkins {
    record Material(String id, String surface, int accent, int eyes, int trim, int border) {
    }

    static final Material[] MATERIALS = {
            new Material("copper", "copper_block", 0xFFE14A, 0x3CF2FF, 0x4FA58A, 0x7A3A22),
            new Material("iron", "iron_block", 0x4F9CFF, 0xFFB000, 0x5A5A5A, 0x8E8E8E),
            new Material("gold", "gold_block", 0xE0203C, 0x3CFF78, 0x8A5A10, 0xB8800C),
            new Material("lapis", "lapis_block", 0xC45CFF, 0xFFE070, 0xE2C25A, 0xE2C25A),
            new Material("diamond", "diamond_block", 0xC8F6FF, 0xFF3355, 0xF0FFFF, 0xC8FFF8),
            new Material("netherite", "netherite_block", 0xFF6A1A, 0x4FF0FF, 0xE8B84A, 0xE8B84A),
            new Material("sculk", "sculk", 0x29DFEB, 0x3FF4FF, 0x2A3438, 0x2A3438),
    };

    /** Animated channels: frame count per material (the renderer matches these). */
    static int frames(Material m) {
        return switch (m.id) {
            case "iron" -> 32;
            case "lapis", "diamond" -> 16;
            case "netherite" -> 20;
            case "sculk" -> 16;
            default -> 0;
        };
    }

    /** Goety's model boxes (texOffs u, v, size w, h, d) and the body part each belongs to. */
    record Box(String part, int u, int v, int w, int h, int d) {
    }

    static final Box[] BOXES = {
            new Box("head", 0, 0, 28, 31, 21), new Box("horn", 196, 0, 20, 13, 13), new Box("horn", 262, 0, 9, 15, 13),
            new Box("horn", 446, 0, 20, 13, 13), new Box("horn", 306, 0, 9, 15, 13), new Box("jaw", 98, 0, 28, 10, 21),
            new Box("jaw", 77, 31, 28, 0, 21),
            new Box("body", 1, 52, 74, 57, 30), new Box("pack", 179, 41, 28, 16, 11),
            new Box("shoulder", 257, 28, 37, 23, 27), new Box("shoulder", 358, 0, 20, 23, 27),
            new Box("arm", 209, 78, 22, 22, 16), new Box("hand", 182, 116, 29, 20, 27), new Box("finger", 182, 163, 3, 10, 5),
            new Box("finger", 198, 163, 3, 10, 5), new Box("shoulder", 285, 78, 37, 23, 27), new Box("shoulder", 386, 50, 20, 23, 27),
            new Box("hand", 294, 128, 29, 20, 27), new Box("pelvis", 0, 139, 28, 11, 21),
            new Box("leg", 0, 171, 22, 29, 21), new Box("leg", 86, 171, 22, 29, 21),
    };

    /** One face of a box in the texture: its rectangle, body part and which side it is. */
    record Face(String part, String side, int x, int y, int w, int h) {
        boolean contains(int px, int py) {
            return px >= x && py >= y && px < x + w && py < y + h;
        }

        boolean edge(int px, int py) {
            return px == x || py == y || px == x + w - 1 || py == y + h - 1;
        }

        /** Twice the distance from the face's centre line, so it is symmetric for odd and even sizes. */
        int dx2(int px) {
            return 2 * (px - x) - (w - 1);
        }

        int dy2(int py) {
            return 2 * (py - y) - (h - 1);
        }
    }

    static final List<Face> FACES = new ArrayList<>();

    static {
        for (Box b : BOXES) {
            FACES.add(new Face(b.part, "top", b.u + b.d, b.v, b.w, b.d));
            FACES.add(new Face(b.part, "bottom", b.u + b.d + b.w, b.v, b.w, b.d));
            FACES.add(new Face(b.part, "right", b.u, b.v + b.d, b.d, b.h));
            FACES.add(new Face(b.part, "front", b.u + b.d, b.v + b.d, b.w, b.h));
            FACES.add(new Face(b.part, "left", b.u + b.d + b.w, b.v + b.d, b.d, b.h));
            FACES.add(new Face(b.part, "back", b.u + 2 * b.d + b.w, b.v + b.d, b.w, b.h));
        }
    }

    static File vanilla;
    /** Goety's energy channels (not its cracks), and their outer rim. */
    static boolean[][] channel;
    static boolean[][] rim;

    public static void main(String[] args) throws Exception {
        File src = new File(args[0]);
        vanilla = new File(args[1]);
        File assets = new File(args[2]);
        findChannels(read(src, "_lines"), read(src, "_glow"));
        boolean[][] allChannels = channel, allRims = rim;
        for (Material material : MATERIALS) {
            // The Lapis one's pack is its enchanting table, with no channels running over it.
            channel = material.id.equals("lapis") ? withoutPack(allChannels) : allChannels;
            rim = material.id.equals("lapis") ? withoutPack(allRims) : allRims;
            if (material.id.equals("sculk")) {
                // It glows where the sculk has grown, as well as along Goety's channels.
                sculkMasks(read(src, ""), allChannels);
                channel = new boolean[allChannels.length][];
                rim = new boolean[allRims.length][];
                for (int x = 0; x < channel.length; ++x) {
                    channel[x] = allChannels[x].clone();
                    rim[x] = allRims[x].clone();
                    for (int y = 0; y < channel[x].length; ++y) {
                        channel[x][y] |= sculkDots[x][y] || sculkHeart[x][y];
                        // Its ribs and the dark middle of its chest cover the channels there (all but its heart);
                        // its lost eye's socket is dark; its shoulder shriekers do not glow (the sculk inside them
                        // animates in its own layer, lit as the rest of it is, as a real shrieker's is).
                        Face face = faceAt(x, y);
                        double chest = ribcage(face, x, y);
                        if (face != null && (chestRib(face, x, y) >= 0 || chest < 0.45 && !sculkHeart[x][y] || socket(face, x, y) || shriekerShoulder(face, x, y))) {
                            channel[x][y] = false;
                        }
                        // Only the channels left have glowing edges.
                        rim[x][y] &= channel[x][y];
                    }
                }
            }
            File dir = new File(assets, "textures/entity/monstrosity/" + material.id);
            dir.mkdirs();
            String name = material.id + "_monstrosity";
            BufferedImage base = read(src, "");
            BufferedImage glyphs = new BufferedImage(base.getWidth(), base.getHeight(), BufferedImage.TYPE_INT_ARGB);
            write(base(base, material, glyphs), new File(dir, name + ".png"));
            if (frames(material) > 0) {
                for (int f = 0; f < frames(material); ++f) {
                    BufferedImage lines = withGlyphs(animated(read(src, "_lines"), material, f), glyphs);
                    write(lines, new File(dir, name + "_lines_" + f + ".png"));
                    write(glow(lines, false), new File(dir, name + "_glow_" + f + ".png"));
                    if (material.id.equals("sculk")) {
                        write(shriekerInsides(base, f), new File(dir, name + "_inner_" + f + ".png"));
                    }
                }
                new File(dir, name + "_lines.png").delete();
                new File(dir, name + "_glow.png").delete();
                write(rimOnly(read(src, "_active"), material), new File(dir, name + "_active.png"));
            } else {
                BufferedImage lines = withGlyphs(accent(read(src, "_lines"), material.accent), glyphs);
                write(lines, new File(dir, name + "_lines.png"));
                write(glow(lines, true), new File(dir, name + "_glow.png"));
                write(withGlyphs(accent(read(src, "_active"), material.accent), glyphs), new File(dir, name + "_active.png"));
            }
            write(flat(read(src, "_eyes"), material.eyes), new File(dir, name + "_eyes.png"));
            write(nonActive(read(src, "_non_active"), material), new File(dir, name + "_non_active.png"));
            write(bands(read(src, "_bands"), material), new File(dir, name + "_bands.png"));
            write(parts(material), new File(dir, name + "_parts.png"));
            if (material.id.equals("netherite")) {
                write(rift(), new File(dir, "netherite_rift.png"));
                write(crater(), new File(dir, "netherite_crater.png"));
            }
            if (material.id.equals("sculk")) {
                write(sculkMire(), new File(dir, "sculk_mire.png"));
                write(sculkPool(), new File(dir, "sculk_pool.png"));
                write(sculkGlob(ImageIO.read(new File(src, "scatter_mine_glow.png"))), new File(dir, "sculk_glob.png"));
            }
        }
        File effects = new File(assets, "textures/mob_effect");
        effects.mkdirs();
        write(gildedIcon(), new File(effects, "gilded.png"));
        for (Map.Entry<String, String> icon : ICONS.entrySet()) {
            write(icon(icon.getValue()), new File(effects, icon.getKey() + ".png"));
        }
    }

    static BufferedImage read(File dir, String suffix) throws Exception {
        return centreEyeSocket(ImageIO.read(new File(dir, "redstone_monstrosity" + suffix + ".png")));
    }

    /**
     * Goety's middle eye sits 1 pixel off the head's centre line; MaterialMonstrosityRenderer draws
     * it centred, so the socket painted behind it on the head's front moves 1 pixel left with it
     * (only the band round the middle eye, not the side eyes).
     */
    static BufferedImage centreEyeSocket(BufferedImage image) {
        for (int y = 35; y <= 39; ++y) {
            for (int x = 26; x < 44; ++x) {
                image.setRGB(x, y, image.getRGB(x + 1, y));
            }
        }
        return image;
    }

    static void write(BufferedImage image, File file) throws Exception {
        ImageIO.write(image, "png", file);
        System.out.println("wrote " + file);
    }

    static final Map<String, BufferedImage> TILES = new HashMap<>();

    static BufferedImage texture(String path) throws Exception {
        BufferedImage image = TILES.get(path);
        if (image == null) {
            image = ImageIO.read(new File(vanilla, path + ".png"));
            TILES.put(path, image);
        }
        return image;
    }

    static BufferedImage tile(String name) throws Exception {
        return texture("block/" + name);
    }

    static BufferedImage glyph(int i) throws Exception {
        return texture("particle/sga_" + (char) ('a' + i));
    }

    // ----- colour helpers ------------------------------------------------------------------------

    static int r(int c) {
        return c >> 16 & 255;
    }

    static int g(int c) {
        return c >> 8 & 255;
    }

    static int b(int c) {
        return c & 255;
    }

    static int rgb(double r, double g, double b) {
        return (clamp(r) << 16) | (clamp(g) << 8) | clamp(b);
    }

    static int clamp(double v) {
        return (int) Math.max(0, Math.min(255, Math.round(v)));
    }

    static double clamp01(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }

    static int mix(int a, int b, double t) {
        return rgb(r(a) + (r(b) - r(a)) * t, g(a) + (g(b) - g(a)) * t, b(a) + (b(b) - b(a)) * t);
    }

    static int scale(int c, double f) {
        return rgb(r(c) * f, g(c) * f, b(c) * f);
    }

    static double luminance(int c) {
        return (0.299 * r(c) + 0.587 * g(c) + 0.114 * b(c)) / 255.0;
    }

    static double saturation(int c) {
        int max = Math.max(r(c), Math.max(g(c), b(c)));
        int min = Math.min(r(c), Math.min(g(c), b(c)));
        return max == 0 ? 0.0 : (max - min) / (double) max;
    }

    static double value(int c) {
        return Math.max(r(c), Math.max(g(c), b(c))) / 255.0;
    }

    static double noise(int x, int y, int seed) {
        long h = x * 374761393L + y * 668265263L + seed * 2147483647L;
        h = (h ^ (h >>> 13)) * 1274126177L;
        h ^= h >>> 16;
        return (h & 0xFFFFFF) / (double) 0x1000000;
    }

    /** Value noise smoothed between cells of the given size, for blotches. */
    static double smooth(int x, int y, int cell, int seed) {
        double fx = x / (double) cell, fy = y / (double) cell;
        int ix = (int) Math.floor(fx), iy = (int) Math.floor(fy);
        double tx = fx - ix, ty = fy - iy;
        tx = tx * tx * (3 - 2 * tx);
        ty = ty * ty * (3 - 2 * ty);
        double top = noise(ix, iy, seed) + (noise(ix + 1, iy, seed) - noise(ix, iy, seed)) * tx;
        double bottom = noise(ix, iy + 1, seed) + (noise(ix + 1, iy + 1, seed) - noise(ix, iy + 1, seed)) * tx;
        return top + (bottom - top) * ty;
    }

    /** A glow colour at a given brightness: dark for dim pixels, nearly white for the brightest. */
    static int accentShade(int accent, double v) {
        int c = scale(accent, 0.25 + 0.75 * v);
        return v > 0.8 ? mix(c, 0xFFFFFF, (v - 0.8) * 1.5) : c;
    }

    static int withAlpha(int rgb, int alpha) {
        return (alpha << 24) | (rgb & 0xFFFFFF);
    }

    static int sample(BufferedImage tile, int x, int y) {
        return tile.getRGB(Math.floorMod(x, tile.getWidth()), Math.floorMod(y, tile.getHeight())) & 0xFFFFFF;
    }

    static Face faceAt(int x, int y) {
        for (Face face : FACES) {
            if (face.contains(x, y)) {
                return face;
            }
        }
        return null;
    }

    // Goety's eye planes (texOffs (0,14) and (0,18)), front and back.
    static boolean isEyes(int x, int y) {
        return x < 12 && y >= 14 && y < 21;
    }

    // ----- channels ------------------------------------------------------------------------------

    /**
     * Goety's glow layers hold both its energy channels and the glowing cracks scattered over the
     * stone. The channels are the larger connected shapes; the cracks are small specks and are
     * dropped everywhere.
     */
    static void findChannels(BufferedImage lines, BufferedImage glow) {
        int w = lines.getWidth(), h = lines.getHeight();
        boolean[][] lit = new boolean[w][h];
        for (int y = 0; y < h; ++y) {
            for (int x = 0; x < w; ++x) {
                lit[x][y] = (lines.getRGB(x, y) >>> 24) > 0 || (glow.getRGB(x, y) >>> 24) > 0;
            }
        }
        channel = new boolean[w][h];
        boolean[][] seen = new boolean[w][h];
        for (int y = 0; y < h; ++y) {
            for (int x = 0; x < w; ++x) {
                if (!lit[x][y] || seen[x][y]) {
                    continue;
                }
                List<int[]> shape = new ArrayList<>();
                ArrayDeque<int[]> queue = new ArrayDeque<>();
                queue.add(new int[]{x, y});
                seen[x][y] = true;
                while (!queue.isEmpty()) {
                    int[] p = queue.poll();
                    shape.add(p);
                    for (int dy = -1; dy <= 1; ++dy) {
                        for (int dx = -1; dx <= 1; ++dx) {
                            int nx = p[0] + dx, ny = p[1] + dy;
                            if (nx >= 0 && ny >= 0 && nx < w && ny < h && lit[nx][ny] && !seen[nx][ny]) {
                                seen[nx][ny] = true;
                                queue.add(new int[]{nx, ny});
                            }
                        }
                    }
                }
                if (shape.size() >= 24) {
                    shape.forEach(p -> channel[p[0]][p[1]] = true);
                }
            }
        }
        rim = new boolean[w][h];
        for (int y = 0; y < h; ++y) {
            for (int x = 0; x < w; ++x) {
                rim[x][y] = channel[x][y] && (x == 0 || y == 0 || x == w - 1 || y == h - 1
                        || !channel[x - 1][y] || !channel[x + 1][y] || !channel[x][y - 1] || !channel[x][y + 1]);
            }
        }
    }

    /**
     * The colour of an animated channel pixel: running water (iron), a starfield (lapis), cut
     * crystal (diamond) or molten netherite.
     */
    static int animatedChannel(Material m, int x, int y, int frame) throws Exception {
        if (m.id.equals("diamond")) {
            // Clear, near-white crystal with faint facets and a glint sweeping across; the glint's
            // trailing edge splits into a faint rainbow, the way light comes off a cut diamond, and a
            // few white points twinkle.
            double phase = frame / (double) frames(m);
            if (rim[x][y]) {
                return 0xF6FFFF;
            }
            double facet = ((x / 3 + y / 3) % 2 == 0 ? 0.0 : 0.12) + smooth(x, y, 4, 91) * 0.25;
            int c = mix(0xF0FDFF, 0x9FDCF0, facet);
            int sweep = Math.floorMod(x + y - (int) (phase * 96), 96);
            if (sweep < 3) {
                c = mix(c, 0xFFFFFF, 1.0 - sweep / 3.0);
            } else if (sweep < 8) {
                int[] spectrum = {0xFFB0D8, 0xFFE8A0, 0xB0FFC8, 0xA8E8FF, 0xC8B8FF};
                c = mix(c, spectrum[sweep - 3], 0.35);
            }
            if (noise(x, y, 92) > 0.985) {
                c = mix(c, 0xFFFFFF, 0.5 + 0.5 * Math.cos(Math.PI * 2.0 * (phase * 2 + noise(x, y, 93))));
            }
            return c;
        }
        if (m.id.equals("sculk")) {
            return sculkGlow(x, y, frame);
        }
        if (m.id.equals("netherite")) {
            // Molten netherite: vanilla lava's own flow, white-hot in the middle, cooling through
            // orange to dark red in its folds; along the banks it has cooled to a dark crust.
            BufferedImage lava = tile("lava_still");
            double t = luminance(sample(lava, x, y + 16 * (frame % 20)));
            t = clamp01((t - 0.3) / 0.32);
            if (rim[x][y]) {
                return mix(0x5A1204, 0xA8300A, t);
            }
            int c = t < 0.2 ? mix(0x4A0C02, 0xC8340A, t / 0.2) : t < 0.6 ? mix(0xC8340A, 0xFF8A1A, (t - 0.2) / 0.4) : mix(0xFF8A1A, 0xFFE89A, (t - 0.6) / 0.4);
            return t > 0.9 ? mix(c, 0xFFFBE8, (t - 0.9) * 6.0) : c;
        }
        if (m.id.equals("iron")) {
            BufferedImage water = tile("water_still");
            double t = clamp01((luminance(sample(water, x, y + 16 * (frame % 32))) - 0.647) / 0.353);
            if (rim[x][y]) {
                // Foam along the banks, rippling.
                return mix(0x9FD8FF, 0xF2FCFF, clamp01(t * 1.4));
            }
            return mix(0x1A3F9E, 0x7CC4FF, t * t * 1.1);
        }
        int count = frames(m);
        double angle = Math.PI * 2.0 * frame / count;
        if (rim[x][y]) {
            return mix(0x9A5CFF, 0xE8C8FF, 0.5 + 0.5 * Math.sin(angle + x * 0.4 + y * 0.3));
        }
        // A slowly turning nebula (two noise fields blended round a circle, so the loop is seamless).
        double n = 0.5 + (smooth(x, y, 6, 71) - 0.5) * Math.cos(angle) * 1.6 + (smooth(x + 40, y, 6, 72) - 0.5) * Math.sin(angle) * 1.6;
        n = clamp01(n);
        int c = n < 0.5 ? mix(0x07031A, 0x2C0E66, n * 2.0) : mix(0x2C0E66, 0x9B3BEA, (n - 0.5) * 2.0);
        if (n > 0.85) {
            c = mix(c, 0xF0A8FF, (n - 0.85) * 3.0);
        }
        // Stars, each twinkling at its own pace.
        double star = noise(x, y, 73);
        if (star > 0.93) {
            double twinkle = 0.5 + 0.5 * Math.cos(angle * (1 + (int) (noise(x, y, 74) * 2)) + noise(x, y, 75) * Math.PI * 2.0);
            c = mix(c, star > 0.985 ? 0xFFFFFF : 0xFFE6A8, twinkle * (star > 0.97 ? 1.0 : 0.7));
        }
        return c;
    }

    // ----- the base skin -------------------------------------------------------------------------

    static BufferedImage base(BufferedImage in, Material m, BufferedImage glyphs) throws Exception {
        BufferedImage out = new BufferedImage(in.getWidth(), in.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < in.getHeight(); ++y) {
            for (int x = 0; x < in.getWidth(); ++x) {
                int argb = in.getRGB(x, y);
                int alpha = argb >>> 24;
                // Goety leaves the underside of the head and the gaps between its teeth see-through (its jaw
                // fills them); without a jaw, the Sculk one's are filled with sculk.
                if (alpha == 0 && m.id.equals("sculk") && faceAt(x, y) != null && faceAt(x, y).part.equals("head")) {
                    out.setRGB(x, y, withAlpha(scale(sculkTile(faceAt(x, y), x, y, 0), 0.8), 255));
                    continue;
                }
                if (alpha == 0) {
                    continue;
                }
                int c = argb & 0xFFFFFF;
                int result;
                if (isEyes(x, y)) {
                    // Dark here: the eyes layer adds the full colour on top (additively).
                    result = scale(m.eyes, 0.25 + 0.1 * value(c));
                } else if (m.id.equals("sculk")) {
                    result = sculkSkin(m, argb, x, y);
                } else if (channel[x][y] && saturation(c) > 0.3) {
                    result = frames(m) > 0 ? animatedChannel(m, x, y, 0) : accentShade(m.accent, value(c));
                } else {
                    Face face = faceAt(x, y);
                    result = surface(m, face, x, y);
                    // Only Goety's deep holes (the chest cavity) stay dark; its cracks and stone shading are gone.
                    if (luminance(c) < 0.08) {
                        result = scale(result, 0.3);
                    }
                    result = motif(m, face, result, x, y, glyphs);
                }
                out.setRGB(x, y, withAlpha(result, alpha));
            }
        }
        return out;
    }

    /** The surface texture at this pixel, tiled out from the middle of its face. */
    static int surface(Material m, Face face, int x, int y) throws Exception {
        String part = face != null ? face.part : "";
        String name = m.surface;
        switch (m.id) {
            case "copper" -> {
                // Patina blotches: exposed, weathered and oxidized copper in growing rings.
                double n = smooth(x, y, 7, 11);
                name = n > 0.8 ? "oxidized_copper" : n > 0.72 ? "weathered_copper" : n > 0.64 ? "exposed_copper"
                        : part.equals("shoulder") || part.equals("pelvis") ? "cut_copper" : "copper_block";
            }
            case "lapis" -> {
                if (part.equals("pack")) {
                    return enchantingTablePack(face, x - face.x, y - face.y);
                }
            }
            default -> {
            }
        }
        if (face == null) {
            return sample(tile(name), x, y);
        }
        // A tile centred on the face: the seams fall evenly on both sides.
        return sample(tile(name), x - face.x + 8 - face.w / 2, y - face.y + 8 - face.h / 2);
    }

    /**
     * The Lapis Monstrosity's back pack drawn as its enchanting table: the table's cloth top on top,
     * its draped sides all round, recoloured like the rest of it (see lapisTable).
     */
    static int enchantingTablePack(Face face, int ox, int oy) throws Exception {
        int sx = Math.min(15, ox * 16 / face.w);
        return switch (face.side) {
            case "top" -> lapisTable(sample(tile("enchanting_table_top"), sx, Math.min(15, oy * 16 / face.h)), ox, oy);
            case "bottom" -> lapisTable(sample(tile("enchanting_table_bottom"), sx, Math.min(15, oy * 16 / face.h)), ox, oy);
            default -> lapisTable(sample(tile("enchanting_table_side"), sx, 4 + Math.min(11, oy * 12 / face.h)), ox, oy);
        };
    }

    static boolean[][] withoutPack(boolean[][] mask) {
        boolean[][] out = new boolean[mask.length][];
        for (int x = 0; x < mask.length; ++x) {
            out[x] = mask[x].clone();
        }
        for (Face face : FACES) {
            if (face.part.equals("pack")) {
                for (int y = face.y; y < face.y + face.h; ++y) {
                    for (int x = face.x; x < face.x + face.w; ++x) {
                        out[x][y] = false;
                    }
                }
            }
        }
        return out;
    }

    static int motif(Material m, Face face, int c, int x, int y, BufferedImage glyphs) throws Exception {
        if (face == null) {
            return c;
        }
        if (m.id.equals("lapis") && face.part.equals("pack")) {
            // The enchanting table keeps its own look, framed in the gold trim.
            return face.edge(x, y) ? mix(m.trim, 0xFFF1A8, noise(x, y, 31) * 0.5) : c;
        }
        boolean edge = face.edge(x, y);
        boolean big = face.w >= 12 && face.h >= 12;
        int ox = x - face.x, oy = y - face.y;
        int ax = Math.abs(face.dx2(x)), ay = Math.abs(face.dy2(y));
        switch (m.id) {
            case "copper" -> {
                if (edge) {
                    return scale(c, 0.7);
                }
            }
            case "iron" -> {
                if (edge) {
                    return scale(c, 0.72);
                }
                if (big) {
                    // Rivets near each corner and along long edges.
                    int rx = rivet(ox, face.w), ry = rivet(oy, face.h);
                    if (rx >= 0 && ry >= 0) {
                        return rx == 0 && ry == 0 ? 0xF4F4F4 : scale(0x8A8A8A, rx == 1 && ry == 1 ? 0.7 : 1.0);
                    }
                    // Rust running down from some of them.
                    if (rx >= 0 && oy > 3 && noise(x, 0, 21) > 0.6 && noise(x, y, 22) > 0.25 && smooth(x, y, 5, 23) > 0.5) {
                        return mix(c, 0x8A4B2A, 0.55);
                    }
                }
            }
            case "gold" -> {
                // Polished gold: a dark rim, a bright bevel inside it, and engraved diamonds round a gem.
                if (edge) {
                    return scale(c, 0.62);
                }
                if (ox == 1 || oy == 1 || ox == face.w - 2 || oy == face.h - 2) {
                    return mix(c, 0xFFF6C8, 0.45);
                }
                if (face.w >= 16 && face.h >= 16) {
                    int d = (ax + ay) / 2;
                    if (d <= 2) {
                        return d == 0 ? 0xFF8A95 : face.dx2(x) < 0 || face.dy2(y) < 0 ? 0xD8142C : 0x7A0814;
                    }
                    if (d == 3) {
                        return 0x6B4507;
                    }
                    if (d % 6 == 0) {
                        return scale(c, 0.8);
                    }
                }
            }
            case "lapis" -> {
                // Gold trim like an enchanting table's, and the enchanting table's glyphs carved in (they glow).
                if (edge) {
                    return mix(m.trim, 0xFFF1A8, noise(x, y, 31) * 0.5);
                }
                int nx = face.w / 10, ny = face.h / 10;
                if (nx > 0 && ny > 0) {
                    int sx = ox - (face.w - nx * 10) / 2, sy = oy - (face.h - ny * 10) / 2;
                    if (sx >= 0 && sy >= 0 && sx < nx * 10 && sy < ny * 10) {
                        int gx = sx / 10, gy = sy / 10, px = sx % 10 - 1, py = sy % 10 - 1;
                        if (px >= 0 && py >= 0 && px < 8 && py < 8 && noise(face.x + gx, face.y + gy, 33) > 0.35) {
                            BufferedImage glyph = glyph((int) (noise(face.x + gx, face.y + gy, 34) * 26));
                            if ((glyph.getRGB(px, py) >>> 24) > 128) {
                                glyphs.setRGB(x, y, withAlpha(m.accent, 255));
                                return 0x9FC3FF;
                            }
                        }
                    }
                }
            }
            case "diamond" -> {
                if (edge) {
                    return mix(c, m.border, 0.6);
                }
                if (noise(x, y, 41) > 0.985) {
                    return 0xFFFFFF;
                }
            }
            case "netherite" -> {
                // Forged war plate: a black seam, a bevel lit from above, gilded brackets on the
                // corners, and on big plates a sunken inner panel held by gold-capped bolts.
                if (edge) {
                    return 0x141012;
                }
                int bracket = Math.min(5, Math.min(face.w, face.h) / 3);
                boolean nearX = ox <= bracket || ox >= face.w - 1 - bracket, nearY = oy <= bracket || oy >= face.h - 1 - bracket;
                if ((ox == 1 || ox == face.w - 2) && nearY || (oy == 1 || oy == face.h - 2) && nearX) {
                    return mix(m.trim, 0xFFF0B0, (ox == 1 || oy == 1 ? 0.35 : 0.0) + noise(x, y, 51) * 0.15);
                }
                if (ox == 1 || oy == 1) {
                    return mix(c, 0xA49AA0, 0.45);
                }
                if (ox == face.w - 2 || oy == face.h - 2) {
                    return scale(c, 0.55);
                }
                // Brushed sheen, brighter towards the top of the plate.
                c = scale(c, 0.85 + 0.3 * (1.0 - oy / (double) face.h) + 0.06 * (noise(x / 2, y, 52) - 0.5));
                if (face.w >= 16 && face.h >= 16) {
                    int ix = Math.min(ox - 4, face.w - 5 - ox), iy = Math.min(oy - 4, face.h - 5 - oy);
                    if (ix >= 0 && iy >= 0 && (ix == 0 || iy == 0)) {
                        // The inner panel's groove: shadow on its top and left, light on its bottom and right.
                        boolean shadow = ox - 4 == 0 || oy - 4 == 0;
                        if (ix == 0 && iy == 0) {
                            return mix(m.trim, 0xFFF0B0, 0.3);
                        }
                        return shadow ? scale(c, 0.45) : mix(c, 0xA49AA0, 0.3);
                    }
                    if (ix > 0 && iy > 0) {
                        return scale(c, 0.88);
                    }
                }
            }
            default -> {
            }
        }
        return c;
    }

    /** 0 for the bright top-left pixel of a rivet, 1 for the rest of it, -1 when not on one. */
    static int rivet(int o, int size) {
        int[] spots = size >= 20 ? new int[]{2, size / 2 - 1, size - 4} : new int[]{2, size - 4};
        for (int s : spots) {
            if (o == s) {
                return 0;
            }
            if (o == s + 1) {
                return 1;
            }
        }
        return -1;
    }

    // ----- glow layers ---------------------------------------------------------------------------

    /** A glow layer recoloured to the accent, keeping only the channels (no glowing cracks). */
    static BufferedImage accent(BufferedImage in, int colour) {
        BufferedImage out = new BufferedImage(in.getWidth(), in.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < in.getHeight(); ++y) {
            for (int x = 0; x < in.getWidth(); ++x) {
                int argb = in.getRGB(x, y);
                int alpha = argb >>> 24;
                out.setRGB(x, y, alpha == 0 || !channel[x][y] ? 0 : withAlpha(accentShade(colour, value(argb & 0xFFFFFF)), alpha));
            }
        }
        return out;
    }

    /**
     * The bright flash during attacks (drawn over the channels at up to full opacity): the channel
     * layer itself lit up, so its pattern (and the water or stars, frame by frame) still shows
     * instead of one flat colour. The rim lights up most. Still channels (a plain gradient) also get
     * branching veins of energy, so the flash has some texture of its own.
     */
    static BufferedImage glow(BufferedImage lines, boolean veins) {
        BufferedImage out = new BufferedImage(lines.getWidth(), lines.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < lines.getHeight(); ++y) {
            for (int x = 0; x < lines.getWidth(); ++x) {
                int argb = lines.getRGB(x, y);
                if ((argb >>> 24) != 0) {
                    int c = scale(argb & 0xFFFFFF, 1.35);
                    if (veins && !rim[x][y]) {
                        double v = smooth(x, y, 7, 31) * 0.65 + smooth(x, y, 3, 37) * 0.35;
                        double vein = Math.pow(1.0 - Math.abs(2.0 * v - 1.0), 6.0);
                        c = mix(scale(c, 0.75 + 0.5 * v), 0xFFFFFF, vein * 0.7);
                    }
                    out.setRGB(x, y, withAlpha(mix(c, 0xFFFFFF, rim[x][y] ? 0.5 : 0.12), 255));
                }
            }
        }
        return out;
    }

    /** One frame of an animated channel layer. */
    static BufferedImage animated(BufferedImage in, Material m, int frame) throws Exception {
        BufferedImage out = new BufferedImage(in.getWidth(), in.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < in.getHeight(); ++y) {
            for (int x = 0; x < in.getWidth(); ++x) {
                // The Sculk one also glows where the sculk has grown over the stone, outside Goety's channels.
                if (channel[x][y] && (m.id.equals("sculk") || (in.getRGB(x, y) >>> 24) != 0)) {
                    out.setRGB(x, y, withAlpha(animatedChannel(m, x, y, frame), 255));
                }
            }
        }
        return out;
    }

    /** The pulsing glow for animated channels: only their rim, so the water or stars show through. */
    static BufferedImage rimOnly(BufferedImage in, Material m) {
        BufferedImage out = new BufferedImage(in.getWidth(), in.getHeight(), BufferedImage.TYPE_INT_ARGB);
        int colour = switch (m.id) {
            case "iron" -> 0xC8F0FF;
            case "diamond" -> 0xFFFFFF;
            case "netherite" -> 0xFFC870;
            case "sculk" -> 0x3FF4FF;
            default -> 0xE0A8FF;
        };
        for (int y = 0; y < in.getHeight(); ++y) {
            for (int x = 0; x < in.getWidth(); ++x) {
                if ((in.getRGB(x, y) >>> 24) != 0 && rim[x][y]) {
                    out.setRGB(x, y, withAlpha(colour, 200));
                }
            }
        }
        return out;
    }

    /** A glow layer in one solid colour, kept saturated (no fading to white), for the eyes. */
    static BufferedImage flat(BufferedImage in, int colour) {
        BufferedImage out = new BufferedImage(in.getWidth(), in.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < in.getHeight(); ++y) {
            for (int x = 0; x < in.getWidth(); ++x) {
                int alpha = in.getRGB(x, y) >>> 24;
                out.setRGB(x, y, alpha == 0 ? 0 : withAlpha(colour, alpha));
            }
        }
        return out;
    }

    /** Adds the lapis glyphs to a glow layer so they light up with the channels. */
    static BufferedImage withGlyphs(BufferedImage layer, BufferedImage glyphs) {
        for (int y = 0; y < layer.getHeight(); ++y) {
            for (int x = 0; x < layer.getWidth(); ++x) {
                if ((glyphs.getRGB(x, y) >>> 24) != 0) {
                    layer.setRGB(x, y, glyphs.getRGB(x, y));
                }
            }
        }
        return layer;
    }

    static BufferedImage nonActive(BufferedImage in, Material m) throws Exception {
        int dark = scale(sample(tile(m.surface), 7, 7), 0.3);
        BufferedImage out = new BufferedImage(in.getWidth(), in.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < in.getHeight(); ++y) {
            for (int x = 0; x < in.getWidth(); ++x) {
                int argb = in.getRGB(x, y);
                int alpha = argb >>> 24;
                out.setRGB(x, y, alpha == 0 ? 0 : withAlpha(mix(argb & 0xFFFFFF, dark, 0.6), alpha));
            }
        }
        return out;
    }

    static BufferedImage bands(BufferedImage in, Material m) {
        BufferedImage out = new BufferedImage(in.getWidth(), in.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < in.getHeight(); ++y) {
            for (int x = 0; x < in.getWidth(); ++x) {
                int argb = in.getRGB(x, y);
                int alpha = argb >>> 24;
                double v = luminance(argb & 0xFFFFFF);
                out.setRGB(x, y, alpha == 0 ? 0 : withAlpha(scale(m.trim, 0.55 + v * 1.2), alpha));
            }
        }
        return out;
    }

    // ----- the sculk monstrosity -----------------------------------------------------------------

    /*
     * The Sculk Monstrosity is a Redstone Monstrosity the sculk killed: Goety's own pale stone, greyed
     * and dead, with sculk in place of every bit of redstone (channels and the specks in its cracks)
     * and spreading out from it, furthest over its legs and lower body. Its chest is a dark hollow
     * behind its ribs (see MonstrosityExtras) with a glowing heart, and where its jaw was is hollow too.
     */
    static int[][] sculkDistance;
    static boolean[][] sculkCover;
    static boolean[][] sculkDots;
    static boolean[][] sculkHeart;

    // A redstone pixel of Goety's skin.
    static boolean isRed(int argb) {
        int c = argb & 0xFFFFFF;
        return (argb >>> 24) != 0 && saturation(c) > 0.35 && r(c) > g(c) * 1.4 && r(c) > 60;
    }

    /** Works out where the sculk has spread, from the redstone outward (never across a gap in the texture). */
    static void sculkMasks(BufferedImage base, boolean[][] channels) throws Exception {
        int w = base.getWidth(), h = base.getHeight();
        sculkDistance = new int[w][h];
        sculkCover = new boolean[w][h];
        sculkDots = new boolean[w][h];
        sculkHeart = new boolean[w][h];
        ArrayDeque<int[]> queue = new ArrayDeque<>();
        for (int x = 0; x < w; ++x) {
            for (int y = 0; y < h; ++y) {
                boolean source = (base.getRGB(x, y) >>> 24) != 0 && (channels[x][y] || isRed(base.getRGB(x, y)));
                sculkDistance[x][y] = source ? 0 : Integer.MAX_VALUE;
                if (source) {
                    queue.add(new int[]{x, y});
                }
            }
        }
        int[][] steps = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        while (!queue.isEmpty()) {
            int[] p = queue.poll();
            for (int[] step : steps) {
                int nx = p[0] + step[0], ny = p[1] + step[1];
                if (nx >= 0 && ny >= 0 && nx < w && ny < h && (base.getRGB(nx, ny) >>> 24) != 0 && sculkDistance[nx][ny] == Integer.MAX_VALUE) {
                    sculkDistance[nx][ny] = sculkDistance[p[0]][p[1]] + 1;
                    queue.add(new int[]{nx, ny});
                }
            }
        }
        for (int x = 0; x < w; ++x) {
            for (int y = 0; y < h; ++y) {
                if ((base.getRGB(x, y) >>> 24) == 0 || isEyes(x, y)) {
                    continue;
                }
                Face face = faceAt(x, y);
                // Its chest is all sculk round its ribs, out to a ragged edge.
                sculkCover[x][y] = sculkDistance[x][y] <= sculkReach(face, x, y) || hollow(face, x, y) || shoulderSpread(face, x, y) < 0.0
                        || ribcage(face, x, y) < 1.05 + 0.25 * (smooth(x, y, 3, 151) - 0.5);
                int s = sculkTile(face, x, y, 0);
                sculkDots[x][y] = sculkCover[x][y] && !hollow(face, x, y) && !(ribcage(face, x, y) < 0.45) && b(s) > 140 && g(s) > 110;
                sculkHeart[x][y] = chestHeart(face, x, y) >= 0;
            }
        }
    }

    // How far the sculk has crept from the redstone, by body part: furthest down its legs and lower body.
    static double sculkReach(Face face, int x, int y) {
        double reach = face == null ? 2.0 : switch (face.part) {
            case "leg" -> 9.0;
            case "pelvis" -> 8.0;
            case "hand", "finger", "arm", "pack" -> 5.0;
            case "body" -> face.side.equals("top") ? 3.0 : 2.0 + 8.0 * (y - face.y) / face.h;
            // The side of its face where it lost an eye.
            case "head" -> face.side.equals("front") && sculkSide(face, x) ? 14.0 : 2.0;
            default -> 2.0;
        };
        return reach + smooth(x, y, 5, 101) * 8.0 - 3.0;
    }

    // Where its jaw was.
    static boolean hollow(Face face, int x, int y) {
        return face != null && face.part.equals("head") && face.side.equals("bottom");
    }

    // Whether a pixel of a front face is on the monstrosity's right (the sculk side: tentacle, bone horn, lost eye).
    static boolean sculkSide(Face face, int x) {
        return x - face.x < face.w / 2;
    }

    // Its chest, as the Warden's: centred, its ribs and heart from 20 pixels down (below its head).
    static final double CHEST_Y = 35.0;

    // How far out a pixel of its chest is from the middle of its ribcage (1 at the ribs' ends); NaN off
    // the front of its body.
    static double ribcage(Face face, int x, int y) {
        if (face == null || !face.part.equals("body") || !face.side.equals("front")) {
            return Double.NaN;
        }
        double dx = (x - face.x) - (face.w - 1) / 2.0, dy = (y - face.y) - CHEST_Y;
        return Math.sqrt(dx * dx / (24.0 * 24.0) + dy * dy / (19.0 * 19.0));
    }

    /**
     * Its ribs, like the Warden's: five pairs of bone bars either side of a dark middle where its
     * heart is, shorter further down, each lit along its top, darker at its ends and bending down a
     * little towards them; -1 where there is no rib. The sculk round them darkens towards the middle
     * of its chest (see sculkSkin), so they sit in it rather than on it.
     */
    static int chestRib(Face face, int x, int y) {
        if (Double.isNaN(ribcage(face, x, y))) {
            return -1;
        }
        double ax = Math.abs((x - face.x) - (face.w - 1) / 2.0);
        int oy = y - face.y;
        for (int i = 0; i < 5; ++i) {
            double inner = 4.5, outer = 21.0 - i * 1.5;
            int row = oy - (int) (CHEST_Y - 15.0) - i * 7 - (int) Math.max(0.0, (ax - inner - 6.0) / 5.0);
            if (ax >= inner && ax <= outer && row >= 0 && row < 3) {
                int bone = row == 0 ? 0xD1D6B6 : row == 1 ? 0xBBC39B : 0x819988;
                return Math.min(ax - inner, outer - ax) < 1.5 ? mix(bone, 0x40576C, 0.55) : bone;
            }
        }
        return -1;
    }

    /** Its heart, the Warden's (from the Warden's heart layer, at the Warden's size), between its ribs; -1 elsewhere. */
    static int chestHeart(Face face, int x, int y) throws Exception {
        if (Double.isNaN(ribcage(face, x, y)) || ribcage(face, x, y) > 0.5) {
            return -1;
        }
        double dx = (x - face.x) - (face.w - 1) / 2.0, dy = (y - face.y) - CHEST_Y;
        int argb = texture("entity/warden/warden_heart").getRGB(20 + (int) Math.floor(dx), 22 + (int) Math.floor(dy));
        return (argb >>> 24) == 0 ? -1 : argb & 0xFFFFFF;
    }

    // The upper blocks of its shoulders, which are sculk shriekers (see MonstrosityExtras).
    static boolean shriekerShoulder(Face face, int x, int y) {
        return face != null && face.part.equals("shoulder") && (x >= 358 && x < 452 && y < 50 || x >= 386 && x < 480 && y >= 50 && y < 100);
    }

    // Where the shrieker stands on the rest of its shoulder, as a fraction of the shoulder from its
    // outer end: the shrieker covers the inner 20 of its 37 pixels.
    static final double SHRIEKER_START = 17.0 / 37.0;

    /*
     * Each upper shoulder block is a sculk shrieker built as the game builds one (its model: a base
     * whose top is the sculk inside it, and on that, inset a pixel, four walls of its bone prongs with
     * L-shaped caps at the corners), at the golem's own pixel size. A shrieker is 16 pixels across and
     * these blocks are 20 by 27, so its pieces keep their size: its corners at the corners and its
     * mouth in the middle, sculk filling between. Its base and inside are here, in the skin; its prong
     * walls are a model piece (see shriekerWalls and MonstrosityExtras).
     */

    /**
     * Which of a sculk shrieker's 16 columns a column of a {@code w} wide face shows: its 6 at each
     * edge and its mouth (6 to 9) in the middle; -1 between them.
     */
    static int shriekerColumn(int ox, int w) {
        int mouth = ox - (w - 16) / 2;
        return ox < 6 ? ox : ox >= w - 6 ? 16 - (w - ox) : mouth >= 6 && mouth <= 9 ? mouth : -1;
    }

    /**
     * A shoulder shrieker's base: round the top of each side, a sculk shrieker's (its last 8 rows),
     * sculk between its pieces and below it running on into the shoulder; on top, the sculk inside it
     * (see shriekerInside).
     */
    static int shriekerBody(Face face, int x, int y) throws Exception {
        int ox = x - face.x, oy = y - face.y;
        if (face.side.equals("top")) {
            return shriekerInside(face, x, y, 0);
        }
        int column = shriekerColumn(ox, face.w);
        if (face.side.equals("bottom") || oy >= 8 || column < 0 && oy > 0) {
            return sculkTile(face, x, y, 0);
        }
        // Its top row (the rim the walls stand on) runs on in its own dark teal between the pieces.
        return sample(tile("sculk_shrieker_side"), column < 0 ? 5 : column, 8 + oy);
    }

    /**
     * The top of a shoulder shrieker: the sculk inside a shrieker ready to summon. Its rim as the
     * shrieker's own (its 6 at each end, its pair of bone dots in the middle, its dark teal between),
     * its rounded corners kept, inside it its swirl spread out to fill the longer block (see pit); -1
     * off it.
     * {@code frame} is one of the golem's 16: the shrieker's own 10 frames, blended between as the
     * game blends them.
     */
    static int shriekerInside(Face face, int x, int y, int frame) throws Exception {
        if (face == null || !face.side.equals("top") || !shriekerShoulder(face, x, y)) {
            return -1;
        }
        double t = frame * 10.0 / 16.0;
        int a = (int) t, b = (a + 1) % 10;
        return mix(shriekerInsideFrame(face, x, y, a), shriekerInsideFrame(face, x, y, b), t - a);
    }

    static int shriekerInsideFrame(Face face, int x, int y, int frame) throws Exception {
        BufferedImage inner = tile("sculk_shrieker_can_summon_inner_top");
        int top = 16 * frame;
        int ox = x - face.x, oy = y - face.y, w = face.w, h = face.h;
        if (ox == 0 || oy == 0 || ox == w - 1 || oy == h - 1) {
            boolean across = oy == 0 || oy == h - 1;
            int column = across ? shriekerColumn(ox, w) : shriekerColumn(oy, h);
            int edge = across ? (oy == 0 ? 0 : 15) : (ox == 0 ? 0 : 15);
            if (column < 0) {
                return 0x034150;
            }
            return across ? sample(inner, column, top + edge) : sample(inner, edge, top + column);
        }
        // Its rounded corners, where they are in the shrieker's own 16 by 16.
        int sx = ox < 8 ? ox : ox >= w - 8 ? 16 - (w - ox) : -1;
        int sy = oy < 8 ? oy : oy >= h - 8 ? 16 - (h - oy) : -1;
        if (sx >= 0 && sy >= 0 && corner(sx, sy)) {
            return sample(inner, sx, top + sy);
        }
        return pit(w, h, frame)[ox][oy];
    }

    // Whether (sx, sy) is in one of the rounded corners of a 16 by 16 shrieker texture.
    static boolean corner(int sx, int sy) {
        return Math.min(sx, 15 - sx) + Math.min(sy, 15 - sy) <= 3;
    }

    static final Map<String, int[][]> PITS = new HashMap<>();
    // How much thicker its ring is than the shrieker's (radially): 1 keeps the shrieker's thickness.
    static double ringScale = 1.15;
    // The half sizes of the oval its swirl turns in: as wide as the pit and only a little longer (so its
    // swirl spreads to a pixel from the walls as the shrieker's does, and stays near round), the pit's
    // dark beyond it at the ends.
    static final double SWIRL_WIDE = 9.0, SWIRL_LONG = 10.5;

    /**
     * The swirl of one of a shrieker's frames in a {@code w} by {@code h} pit. A shrieker's swirl is a
     * ring of crescents turning round a dark eye, so its swirl is mapped onto an oval in the middle of
     * the pit, only a little longer than the shrieker's circle: each pixel takes the shrieker's at the
     * same angle round it, at about the same depth in from its edge. The crescents keep their
     * thickness and lengthen a little round the ring, keeping one outline in every frame as the
     * shrieker's keeps its circle; outside it, the shrieker's dark.
     */
    static int[][] pit(int w, int h, int frame) throws Exception {
        String key = w + "x" + h + "@" + frame;
        int[][] cached = PITS.get(key);
        if (cached != null) {
            return cached;
        }
        BufferedImage inner = tile("sculk_shrieker_can_summon_inner_top");
        int top = 16 * frame;
        int[][] out = new int[w][h];
        // The oval (inside the pit's 1 pixel rim), and the shrieker's swirl round 8, 8 (7 to its edge).
        double a = Math.min((w - 2) / 2.0, SWIRL_WIDE), b = Math.min((h - 2) / 2.0, SWIRL_LONG);
        for (int y = 0; y < h; ++y) {
            for (int x = 0; x < w; ++x) {
                double dx = x + 0.5 - w / 2.0, dy = y + 0.5 - h / 2.0;
                double angle = Math.atan2(dy / b, dx / a);
                double reach = Math.hypot(dx / a, dy / b);
                if (reach > 1.0) {
                    out[x][y] = 0x0D1217;
                    continue;
                }
                double edge = Math.hypot(a * Math.cos(angle), b * Math.sin(angle));
                double radius = Math.max(0.0, 7.0 - (1.0 - reach) * edge / ringScale);
                int sx = Math.max(1, Math.min(14, (int) Math.floor(8.0 + radius * Math.cos(angle))));
                int sy = Math.max(1, Math.min(14, (int) Math.floor(8.0 + radius * Math.sin(angle))));
                out[x][y] = sample(inner, sx, top + sy);
            }
        }
        PITS.put(key, out);
        return out;
    }

    /** One frame of the sculk inside its shoulder shriekers, alone (for its own layer, see MaterialMonstrosityRenderer). */
    static BufferedImage shriekerInsides(BufferedImage like, int frame) throws Exception {
        BufferedImage out = new BufferedImage(like.getWidth(), like.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < out.getHeight(); ++y) {
            for (int x = 0; x < out.getWidth(); ++x) {
                int c = shriekerInside(faceAt(x, y), x, y, frame);
                if (c >= 0) {
                    out.setRGB(x, y, withAlpha(c, 255));
                }
            }
        }
        return out;
    }

    /**
     * A shoulder shrieker's prong walls, a {@code w} by {@code h} by {@code d} box (see
     * MonstrosityExtras) at (u, v) in the parts texture: each side a sculk shrieker's prongs (the top 7
     * rows of its side: its two horns, its mouth in the notch between them), the top its stepped caps
     * at the corners, the bottom see-through (it stands on the block). The walls are longer than a
     * shrieker's, so its horns are broadened at their feet to meet the mouth as its own do (see
     * hornColumn; at the golem's pixel size, not stretched).
     */
    static void shriekerWalls(BufferedImage out, int u, int v, int w, int h, int d) throws Exception {
        BufferedImage side = tile("sculk_shrieker_side"), top = tile("sculk_shrieker_top");
        for (int y = v; y < v + d + h; ++y) {
            for (int x = u; x < u + 2 * d + 2 * w; ++x) {
                out.setRGB(x, y, 0);
            }
        }
        // The shrieker's own stepped caps, one in each corner (from the nearest corner, along each way).
        for (int z = 0; z < d; ++z) {
            for (int x = 0; x < w; ++x) {
                int ax = Math.min(x, w - 1 - x), az = Math.min(z, d - 1 - z);
                if (ax < 4 && az < 4) {
                    out.setRGB(u + d + x, v + z, top.getRGB(1 + ax, 1 + az));
                }
            }
        }
        // The sides, left to right as laid out: one d wide, one w, one d, one w.
        int[][] sides = {{u, d}, {u + d, w}, {u + d + w, d}, {u + 2 * d + w, w}};
        for (int[] s : sides) {
            int wide = s[1];
            // The left horn, the mouth (the shrieker's 4 middle columns), the right horn.
            int horn = 5 + (wide - 14) / 2;
            for (int row = 0; row < h; ++row) {
                for (int x = 0; x < wide; ++x) {
                    int column = x < horn ? hornColumn(x, horn, 1 + row) : x < horn + 4 ? 6 + (x - horn)
                            : mirror(hornColumn(wide - 1 - x, wide - horn - 4, 1 + row));
                    out.setRGB(s[0] + x, v + d + row, column < 0 ? 0 : side.getRGB(column, 1 + row));
                }
            }
        }
    }

    static int mirror(int column) {
        return column < 0 ? -1 : 15 - column;
    }

    /**
     * Which of a shrieker's left horn columns (1 to 5) a pixel of a {@code length} long horn shows,
     * {@code out} from its outer edge on {@code row} (1 at its tip to 7 at its foot), or -1 where it is
     * see-through. A shrieker's horn is an upright tusk (3 wide at its tip, 4 below and 5 at its foot),
     * the notch between its two a straight-sided U down to the mouth. These keep that shape, a little
     * broader for the longer walls (3, then 5, then 7 at its foot); the longer walls' notch is just
     * wider, closed at its floor by a sill of bone on its last two rows meeting the mouth, as the
     * shrieker's bottom row runs across. Its outer column, inner edge and the
     * shadow beside it are the shrieker's for that row; the rest is its middle (2 and 3), mixed.
     */
    static int hornColumn(int out, int length, int row) {
        int width = Math.min(length, row == 1 ? 3 : row <= 4 ? 5 : row == 5 ? 7 : length);
        if (out >= width) {
            return -1;
        }
        int edge = row == 1 ? 3 : row <= 4 ? 4 : 5;
        if (out == 0) {
            return 1;
        }
        if (out == width - 1) {
            return edge;
        }
        if (out == width - 2) {
            return edge - 1;
        }
        return noise(out, row, 161) > 0.5 ? 2 : 3;
    }

    // How far a pixel of a shoulder's lower block is towards its inner side (0 at the outer end, 1 at
    // the inner, where the shrieker stands); NaN off them.
    static double shoulderInner(Face face, int x, int y) {
        if (face == null || !face.part.equals("shoulder") || shriekerShoulder(face, x, y)) {
            return Double.NaN;
        }
        boolean right = x >= 257 && x < 385 && y >= 28 && y < 78;
        if (!right && !(x >= 285 && x < 413 && y >= 78 && y < 128)) {
            return Double.NaN;
        }
        double along = (x - face.x + 0.5) / face.w;
        return switch (face.side) {
            case "right" -> right ? 0.0 : 1.0;
            case "left" -> right ? 1.0 : 0.0;
            case "back", "top", "bottom" -> right ? 1.0 - along : along;
            default -> right ? along : 1.0 - along;
        };
    }

    /**
     * How far past the edge of the sculk spreading down from a shoulder shrieker a pixel of the rest of
     * that shoulder is (negative where it is covered), in parts of the shoulder: the sculk covers the
     * shoulder from its inner side, where the shrieker stands (all of it below the shrieker), two
     * thirds of the way out, in a ragged edge. NaN off the shoulders.
     */
    static double shoulderSpread(Face face, int x, int y) {
        double inner = shoulderInner(face, x, y);
        double edge = Math.min(0.3 + 0.22 * smooth(x, y, 4, 141) + 0.08 * noise(x, y, 142), SHRIEKER_START - 0.03);
        return edge - inner;
    }

    // The empty socket of its lost eye.
    static boolean socket(Face face, int x, int y) {
        int ox = x - face.x, oy = y - face.y;
        return face.part.equals("head") && face.side.equals("front") && sculkSide(face, x) && oy >= 18 && oy <= 24 && ox <= 5;
    }

    /** Vanilla sculk (one of its animation frames), tiled from the middle of the face. */
    static int sculkTile(Face face, int x, int y, int frame) throws Exception {
        BufferedImage sculk = tile("sculk");
        int sx = face == null ? x : x - face.x + 8 - face.w / 2, sy = face == null ? y : y - face.y + 8 - face.h / 2;
        return sample(sculk, Math.floorMod(sx, 16), Math.floorMod(sy, 16) + 16 * (frame % 4));
    }

    static int sculkSkin(Material m, int argb, int x, int y) throws Exception {
        Face face = faceAt(x, y);
        if (channel[x][y]) {
            return animatedChannel(m, x, y, 0);
        }
        int sculk = sculkTile(face, x, y, 0);
        int rib = chestRib(face, x, y);
        if (rib >= 0) {
            return rib;
        }
        double chest = ribcage(face, x, y);
        if (chest < 1.0) {
            // The sculk of its chest darkens towards the middle, where its heart is.
            return scale(sculk, 0.3 + 0.7 * chest * chest);
        }
        if (hollow(face, x, y)) {
            return scale(sculk, 0.35);
        }
        if (shriekerShoulder(face, x, y)) {
            return shriekerBody(face, x, y);
        }
        if (face != null && socket(face, x, y)) {
            int ox = x - face.x, oy = y - face.y;
            return ox == 5 || oy == 18 || oy == 24 ? 0x0B2A30 : 0x03080A;
        }
        if (sculkCover[x][y]) {
            return boneFade(face, sculk, x, y);
        }
        // Dead stone: Goety's own, greyed, darkening where the sculk is about to creep over it, with
        // a few thin veins of sculk running ahead of it.
        int c = argb & 0xFFFFFF;
        double grey = luminance(c) * 255.0;
        int stone = mix(rgb(grey, grey, grey), 0xA8A49A, 0.18);
        double spread = shoulderSpread(face, x, y);
        if (spread < 0.2) {
            // On a shoulder, just past the sculk flowing out of its shrieker.
            return spread < 0.08 && smooth(x, y, 2, 143) > 0.55 ? 0x0E3138 : scale(stone, 0.7 + 1.5 * spread);
        }
        double ahead = sculkDistance[x][y] - sculkReach(face, x, y);
        if (ahead < 5.0 && smooth(x, y, 2, 107) > 0.62) {
            return 0x0E3138;
        }
        return ahead < 6.0 ? scale(stone, 0.72 + 0.045 * ahead) : stone;
    }

    /**
     * Its limbs fade from bone at the top into sculk, like the Warden's arms and the sides of a sculk
     * catalyst, the bone (a sculk shrieker's colours) running down in ragged streaks.
     */
    static int boneFade(Face face, int sculk, int x, int y) {
        if (face == null || face.side.equals("top") || face.side.equals("bottom")
                || !List.of("leg", "arm", "hand", "finger", "pelvis").contains(face.part)) {
            return sculk;
        }
        double t = (y - face.y) / (double) face.h;
        double edge = 0.32 + 0.22 * noise(x, 0, 121) + 0.06 * noise(x, y, 122);
        if (t >= edge) {
            return sculk;
        }
        int bone = mix(0xD9DCBC, 0x8E9784, t / edge);
        return t > edge - 0.07 ? mix(bone, 0x2A5A60, 0.6) : bone;
    }

    /** The Sculk one's glowing parts, animated: its heart, the sculk's specks, and the redstone channels it filled. */
    static int sculkGlow(int x, int y, int frame) throws Exception {
        double phase = frame / 16.0;
        if (sculkHeart[x][y]) {
            // Its heart beats, as the Warden's does.
            double beat = Math.max(Math.pow(Math.max(0.0, Math.cos(Math.PI * 2.0 * phase)), 6.0), 0.3);
            return scale(chestHeart(faceAt(x, y), x, y), 0.4 + 0.9 * beat);
        }
        if (sculkDots[x][y]) {
            double pulse = 0.5 + 0.5 * Math.cos(Math.PI * 2.0 * (phase + noise(x / 2, y / 2, 103)));
            return scale(sculkTile(faceAt(x, y), x, y, 0), 0.35 + 0.75 * pulse);
        }
        Face face = faceAt(x, y);
        int inside = shriekerInside(face, x, y, frame);
        if (inside >= 0) {
            return inside;
        }
        if (rim[x][y]) {
            return mix(0x0A3238, 0x1C8A96, 0.5 + 0.5 * Math.cos(Math.PI * 2.0 * phase));
        }
        // The channels are sculk like the rest of it, a little darker, their specks pulsing.
        int c = scale(sculkTile(face, x, y, 0), 0.8);
        if (b(c) > 110 && g(c) > 90) {
            c = scale(c, 0.5 + 0.6 * (0.5 + 0.5 * Math.cos(Math.PI * 2.0 * (phase + noise(x / 2, y / 2, 106)))));
        }
        return c;
    }

    // ----- netherite rifts -----------------------------------------------------------------------

    /** The molten colour of a crack: white-hot at its heart (0), through orange to dark red (1). */
    static int molten(double d) {
        return d < 0.25 ? mix(0xFFF4C8, 0xFFC040, d / 0.25) : d < 0.6 ? mix(0xFFC040, 0xFF6A14, (d - 0.25) / 0.35) : mix(0xFF6A14, 0x8A1A06, (d - 0.6) / 0.4);
    }

    /**
     * One block of a Netherite fissure (see NetheriteRiftRenderer), 32 pixels along it by 16 across,
     * repeating along its length: a jagged glowing crack with small side cracks, in a scorched band.
     */
    static BufferedImage rift() {
        BufferedImage out = new BufferedImage(32, 16, BufferedImage.TYPE_INT_ARGB);
        for (int x = 0; x < 32; ++x) {
            double t = x / 32.0 * Math.PI * 2.0;
            // Repeats every 32 pixels, so the tiles join up.
            double line = 7.5 + 1.5 * Math.sin(t) + 0.9 * Math.sin(3.0 * t + 1.0) + 0.5 * Math.sin(7.0 * t + 2.0);
            for (int y = 0; y < 16; ++y) {
                double d = Math.abs(y - line);
                // Side cracks branching off at a few spots.
                for (int branch : new int[]{5, 19, 27}) {
                    int bx = x - branch;
                    if (bx >= 0 && bx < 5) {
                        double by = line + (branch == 19 ? -1 : 1) * (bx * 0.9 + 0.5);
                        d = Math.min(d, Math.abs(y - by) + 0.6 + bx * 0.15);
                    }
                }
                double width = 1.2 + 0.4 * Math.sin(5.0 * t);
                if (d <= width) {
                    out.setRGB(x, y, withAlpha(molten(d / width * 0.7), 255));
                } else if (d <= width + 1.0) {
                    out.setRGB(x, y, withAlpha(molten(0.95), 235));
                } else {
                    // Scorched ground round it, fading out with a ragged edge.
                    double scorch = 1.0 - (d - width - 1.0) / 3.5 + (noise(x, y, 81) - 0.5) * 0.5;
                    scorch = Math.min(scorch, Math.min(y, 15 - y) / 2.0);
                    if (scorch > 0.0) {
                        out.setRGB(x, y, withAlpha(mix(0x1A0C08, 0x3A1408, noise(x, y, 82) * 0.6), (int) (Math.min(1.0, scorch) * 200)));
                    }
                }
            }
        }
        return out;
    }

    /** The crater where the Netherite Monstrosity lands, 64x64: a molten pit with jagged cracks raying out, on scorched ground. */
    static BufferedImage crater() {
        BufferedImage out = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        int rays = 9;
        for (int y = 0; y < 64; ++y) {
            for (int x = 0; x < 64; ++x) {
                double dx = x - 31.5, dy = y - 31.5;
                double r = Math.sqrt(dx * dx + dy * dy);
                if (r > 32.0) {
                    continue;
                }
                double a = Math.atan2(dy, dx);
                // Distance to the nearest ray, each wobbling as it goes out.
                double best = Double.MAX_VALUE;
                for (int i = 0; i < rays; ++i) {
                    double ray = i * Math.PI * 2.0 / rays + noise(i, 0, 83) * 0.4 + Math.sin(r * 0.35 + i) * 0.08;
                    double off = Math.abs(Math.IEEEremainder(a - ray, Math.PI * 2.0)) * r;
                    best = Math.min(best, off);
                }
                double width = Math.max(0.6, 2.2 * (1.0 - r / 30.0));
                if (r < 6.0 + noise(x, y, 84) * 1.5) {
                    out.setRGB(x, y, withAlpha(molten(r / 8.0 * 0.6 + noise(x, y, 85) * 0.15), 255));
                } else if (best <= width && r < 30.0) {
                    out.setRGB(x, y, withAlpha(molten(0.2 + best / width * 0.5 + r / 60.0), 255));
                } else {
                    double scorch = 1.0 - r / 30.0 + (noise(x, y, 86) - 0.5) * 0.35;
                    if (scorch > 0.0) {
                        out.setRGB(x, y, withAlpha(mix(0x1A0C08, 0x3A1408, noise(x, y, 87) * 0.6), (int) (Math.min(1.0, scorch * 1.4) * 210)));
                    }
                }
            }
        }
        return out;
    }

    /**
     * The line of sculk mire the Sculk Monstrosity's tentacles leave (see SculkMireRenderer): 8
     * blocks along by 2 across, at a block's 16 pixels each, so drawn block by block it is never
     * stretched. Sculk (the block's own pixels) in a ragged band with a few gaps, sculk veins
     * creeping out of its edges; its first and last blocks round it off (the renderer puts them at
     * a line's two ends, and the middle ones between).
     */
    static BufferedImage sculkMire() throws Exception {
        BufferedImage out = new BufferedImage(128, 32, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 32; ++y) {
            for (int x = 0; x < 128; ++x) {
                double half = 11.5 + (smooth(x, 0, 7, 91) - 0.5) * 5.0;
                // Rounded caps: a circle of the band's half width at each end.
                double cap = Math.min(x - 1.0, 126.0 - x) - half;
                double reach = cap >= 0.0 ? half : Math.sqrt(Math.max(0.0, half * half - cap * cap));
                double from = Math.abs(y - 15.5) + (noise(x, y, 92) - 0.5) * 2.0 - reach;
                sculkGround(out, x, y, from, smooth(x, y, 5, 93) < 0.16);
            }
        }
        return out;
    }

    /** The pool of sculk mire a sculk glob leaves, 5 blocks across (80x80): the same sculk, in a ragged round patch. */
    static BufferedImage sculkPool() throws Exception {
        BufferedImage out = new BufferedImage(80, 80, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 80; ++y) {
            for (int x = 0; x < 80; ++x) {
                double dx = x - 39.5, dy = y - 39.5;
                double a = Math.atan2(dy, dx);
                double reach = 33.0 + 2.5 * Math.sin(3.0 * a + 1.0) + 1.5 * Math.sin(7.0 * a + 2.0) + (smooth(x, y, 6, 94) - 0.5) * 4.0;
                double from = Math.sqrt(dx * dx + dy * dy) + (noise(x, y, 95) - 0.5) * 2.0 - reach;
                sculkGround(out, x, y, from, smooth(x, y, 6, 96) < 0.14);
            }
        }
        return out;
    }

    // A pixel of sculk mire {@code from} pixels outside its edge (negative inside): sculk inside (but
    // in its gaps), sculk vein just outside, nothing further out. Both tile at a block's 16 pixels.
    static void sculkGround(BufferedImage out, int x, int y, double from, boolean gap) throws Exception {
        BufferedImage sculk = tile("sculk"), vein = tile("sculk_vein");
        if (from <= 0.0 && !(gap && from < -2.0)) {
            out.setRGB(x, y, sculk.getRGB(x % 16, y % 16) | 0xFF000000);
        } else if (from <= 3.0 || gap) {
            int v = vein.getRGB(x % 16, y % 16);
            if (v >>> 24 > 0 && noise(x, y, 97) < 0.8 - (gap ? 0.0 : from * 0.15)) {
                out.setRGB(x, y, v);
            }
        }
    }

    /**
     * Goety's scatter bomb (its glowing texture, scatter_mine_glow.png from its jar, in the Goety
     * textures folder) recoloured from fiery yellow to sculk's glowing teal, shade for shade, for
     * the Sculk Monstrosity's sculk globs.
     */
    static BufferedImage sculkGlob(BufferedImage in) {
        double low = 1.0, high = 0.0;
        for (int y = 0; y < in.getHeight(); ++y) {
            for (int x = 0; x < in.getWidth(); ++x) {
                int c = in.getRGB(x, y);
                if (c >>> 24 > 0) {
                    low = Math.min(low, luma(c));
                    high = Math.max(high, luma(c));
                }
            }
        }
        BufferedImage out = new BufferedImage(in.getWidth(), in.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < in.getHeight(); ++y) {
            for (int x = 0; x < in.getWidth(); ++x) {
                int c = in.getRGB(x, y);
                if (c >>> 24 > 0) {
                    double t = high > low ? (luma(c) - low) / (high - low) : 1.0;
                    int teal = t < 0.5 ? mix(0x0A5560, 0x1ACBCF, t * 2.0) : mix(0x1ACBCF, 0xC4FFF8, (t - 0.5) * 2.0);
                    out.setRGB(x, y, withAlpha(teal, c >>> 24));
                }
            }
        }
        return out;
    }

    static double luma(int c) {
        return (0.299 * r(c) + 0.587 * g(c) + 0.114 * b(c)) / 255.0;
    }

    // ----- extra model pieces --------------------------------------------------------------------

    /**
     * 256x256 texture for the material's extra pieces (see MonstrosityExtras): the material block
     * everywhere, a secondary surface in (0,64)-(64,128), solid glow colours in (64,64)-(128,96)
     * (accent on the left half, a second colour on the right), alpha art in (64,96)-(128,128),
     * and per-material pieces from (128,0) and (0,128): the Gold one's cape, ermine mantle and velvet.
     */
    static BufferedImage parts(Material m) throws Exception {
        BufferedImage out = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
        BufferedImage primary = tile(m.surface);
        BufferedImage secondary = tile(switch (m.id) {
            case "copper" -> "cut_copper";
            case "gold" -> "red_wool";
            case "netherite" -> "gold_block";
            case "sculk" -> "bone_block_top";
            default -> m.surface;
        });
        int second = switch (m.id) {
            case "copper" -> 0xFFF6C0;
            case "lapis" -> 0xFFB43C;
            case "netherite" -> 0xFFC040;
            case "sculk" -> 0xB8FFFF;
            default -> m.eyes;
        };
        for (int y = 0; y < 256; ++y) {
            for (int x = 0; x < 256; ++x) {
                int c;
                if (x < 64 && y >= 64 && y < 128) {
                    c = sample(secondary, x, y);
                    if (m.id.equals("lapis") && y >= 96) {
                        // Purple wax candles, lighter towards the top of each 8 pixel block.
                        c = scale(0x8A3FCC, 0.8 + 0.3 * (1.0 - (y % 8) / 8.0) + 0.1 * noise(x, y, 63));
                    } else if (m.id.equals("gold")) {
                        c = velvet(c);
                    } else if (m.id.equals("iron")) {
                        c = scale(c, 0.6);
                    } else if (m.id.equals("diamond")) {
                        c = mix(c, 0xFFFFFF, 0.35);
                    } else if (m.id.equals("sculk")) {
                        // A sculk shrieker's bone: pale greenish cream in uneven stripes, shadowed blue-grey.
                        int[] shades = {0xE0E3C4, 0xD0D4B4, 0xBFC3A2, 0x9AA48E};
                        c = shades[Math.floorMod(x / 2 + (int) (noise(x / 2, y / 6, 131) * 2.0), 4)];
                        if (noise(x, y, 132) > 0.9) {
                            c = mix(c, 0x5A6E78, 0.4);
                        }
                    }
                } else if (x >= 64 && x < 128 && y >= 64 && y < 96) {
                    c = accentShade(x < 96 ? m.accent : second, 0.75 + 0.25 * noise(x, y, 61));
                } else if (x >= 64 && x < 128 && y >= 96 && y < 128) {
                    continue;
                } else if (m.id.equals("gold") && y >= 160) {
                    c = velvet(sample(secondary, x, y));
                } else {
                    c = sample(primary, x, y);
                    if (m.id.equals("copper") && smooth(x, y, 6, 62) > 0.68) {
                        c = sample(tile("oxidized_copper"), x, y);
                    }
                }
                out.setRGB(x, y, withAlpha(c, 255));
            }
        }
        // Alpha art: vines and a poppy for iron, glyphs (and their mirror image for the back) for lapis.
        if (m.id.equals("iron")) {
            BufferedImage vine = tile("vine");
            for (int y = 96; y < 128; ++y) {
                for (int x = 64; x < 96; ++x) {
                    int argb = vine.getRGB(Math.floorMod(x, 16), Math.floorMod(y, 16));
                    if ((argb >>> 24) > 0) {
                        out.setRGB(x, y, withAlpha(scale(0x48B518, (argb & 0xFF) / 255.0 * 1.1), 255));
                    }
                }
            }
            BufferedImage poppy = tile("poppy");
            for (int y = 0; y < 16; ++y) {
                for (int x = 0; x < 16; ++x) {
                    out.setRGB(96 + x, 96 + y, poppy.getRGB(x, y));
                    out.setRGB(127 - x, 96 + y, poppy.getRGB(x, y));
                }
            }
        } else if (m.id.equals("lapis")) {
            for (int i = 0; i < 8; ++i) {
                BufferedImage glyph = glyph(i * 3 % 26);
                int u = 64 + (i % 4) * 16, v = 96 + (i / 4) * 8;
                for (int y = 0; y < 8; ++y) {
                    for (int x = 0; x < 8; ++x) {
                        if ((glyph.getRGB(x, y) >>> 24) > 128) {
                            out.setRGB(u + x, v + y, withAlpha(0xE8B0FF, 255));
                            out.setRGB(u + 15 - x, v + y, withAlpha(0xE8B0FF, 255));
                        }
                    }
                }
            }
        } else if (m.id.equals("gold")) {
            cape(out);
        } else if (m.id.equals("sculk")) {
            sculkArt(out);
            // The prong walls of its shoulder shriekers (see MonstrosityExtras).
            shriekerWalls(out, 128, 0, 18, 7, 25);
        }
        return out;
    }

    /**
     * The Sculk one's see-through art: strands of sculk dripping down (16x16 at (80,96), mirrored at
     * (96,96) for the back) and a Warden's tendril, from the Warden's own texture (16x16 at (64,112),
     * mirrored at (80,112)).
     */
    static void sculkArt(BufferedImage out) throws Exception {
        BufferedImage warden = texture("entity/warden/warden");
        for (int y = 0; y < 16; ++y) {
            for (int x = 0; x < 16; ++x) {
                int argb = warden.getRGB(58 + x, y);
                out.setRGB(64 + x, 112 + y, argb);
                out.setRGB(95 - x, 112 + y, argb);
            }
        }
        for (int x = 0; x < 16; ++x) {
            // Strands in clumps, each hanging its own length, a few with a glowing speck.
            if (noise(x / 2, 0, 111) < 0.3) {
                continue;
            }
            int length = 4 + (int) (noise(x, 0, 112) * 12.0);
            for (int y = 0; y < length; ++y) {
                int c = y == length - 1 ? 0x061A20 : noise(x, y, 113) > 0.92 ? 0x29DFEB : mix(0x0A2A30, 0x14424A, noise(x, y, 114));
                out.setRGB(80 + x, 96 + y, withAlpha(c, 255));
                out.setRGB(111 - x, 96 + y, withAlpha(c, 255));
            }
        }
    }

    /** Red wool deepened to royal crimson velvet. */
    static int velvet(int wool) {
        double l = luminance(wool);
        return mix(0x4A0610, 0xC81E3A, clamp01((l - 0.15) * 2.6));
    }

    /** White wool with black ermine tails in staggered rows. */
    static int ermine(int x, int y) throws Exception {
        int c = mix(sample(tile("white_wool"), x, y), 0xFFFFFF, 0.3);
        int row = Math.floorMod(y, 8), col = Math.floorMod(x + (Math.floorMod(y / 8, 2) == 0 ? 0 : 4), 8);
        if (col == 3 && row >= 2 && row <= 4) {
            return 0x141414;
        }
        if ((col == 2 || col == 4) && row == 2) {
            return 0x2A2A2A;
        }
        return c;
    }

    /**
     * The Gold Monstrosity's royal cape (a 60x80x1 box at (128,0): the inside at (129,1), the
     * outside at (190,1), gold edges) in crimson velvet with gold borders, ermine at the top and a gold crown
     * embroidered on the back; the ermine mantle (a 64x6x24 box at (0,128)) and velvet from (0,160) for the crown.
     */
    static void cape(BufferedImage out) throws Exception {
        for (int y = 0; y < 82; ++y) {
            for (int x = 128; x < 250; ++x) {
                out.setRGB(x, y, withAlpha(0xC8961E, 255));
            }
        }
        for (int side = 0; side < 2; ++side) {
            int u = 129 + side * 61;
            for (int y = 0; y < 80; ++y) {
                for (int x = 0; x < 60; ++x) {
                    int c = velvet(sample(tile("red_wool"), x, y));
                    if (side == 0) {
                        c = scale(c, 0.7);
                    }
                    // Folds: soft vertical shading.
                    c = scale(c, 0.88 + 0.12 * Math.cos(x * Math.PI / 7.5));
                    boolean border = x < 3 || x >= 57 || y >= 77;
                    if (border) {
                        c = x == 2 || x == 57 || y == 77 ? 0x8A5A10 : mix(0xF5C53A, 0xFFF1A8, noise(x, y, 81) * 0.6);
                    }
                    if (y < 8) {
                        c = ermine(x, y);
                    }
                    out.setRGB(u + x, 1 + y, withAlpha(c, 255));
                }
            }
        }
        // A gold crown embroidered on the outside, centred.
        String[] crown = {
                "g.......g.......g",
                "gg.....ggg.....gg",
                "ggg...ggrgg...ggg",
                "gggg.ggggggg.gggg",
                "ggggggggggggggggg",
                "ggrggggbgbggggrgg",
                "ggggggggggggggggg",
                "ddddddddddddddddd"};
        for (int y = 0; y < crown.length; ++y) {
            for (int x = 0; x < crown[y].length(); ++x) {
                int c = switch (crown[y].charAt(x)) {
                    case 'g' -> mix(0xF5C53A, 0xFFF1A8, noise(x, y, 82) * 0.5);
                    case 'd' -> 0x8A5A10;
                    case 'r' -> 0xE0203C;
                    case 'b' -> 0x3CFF78;
                    default -> -1;
                };
                if (c >= 0) {
                    out.setRGB(190 + 30 - 8 + x, 23 + y, withAlpha(c, 255));
                }
            }
        }
        for (int y = 128; y < 158; ++y) {
            for (int x = 0; x < 176; ++x) {
                out.setRGB(x, y, withAlpha(ermine(x, y), 255));
            }
        }
    }

    static int lapisTable(int c, int x, int y) throws Exception {
        double l = luminance(c);
        if (r(c) > g(c) + 40 && r(c) > b(c) + 20) {
            return mix(0x24093F, 0xA552F0, clamp01(l * 2.2));
        }
        if (g(c) > r(c) + 15 && b(c) > r(c) + 15) {
            return mix(0x8A5A10, 0xFFF1A8, clamp01((l - 0.4) * 1.8));
        }
        return scale(sample(tile("lapis_block"), x, y), 0.55 + l * 1.5);
    }

    // ----- effect icons --------------------------------------------------------------------------

    /** The Lapis buffs' icons: the vanilla item (or block) that best stands for the enchantment. */
    static final Map<String, String> ICONS = new java.util.LinkedHashMap<>();

    static {
        String[][] icons = {
                {"protection", "item/iron_chestplate"}, {"fire_protection", "item/magma_cream"}, {"blast_protection", "item/gunpowder"},
                {"projectile_protection", "item/arrow"}, {"feather_falling", "item/feather"}, {"thorns", "item/sweet_berries"},
                {"respiration", "item/turtle_helmet"}, {"aqua_affinity", "item/heart_of_the_sea"}, {"depth_strider", "item/nautilus_shell"},
                {"frost_walker", "item/snowball"}, {"soul_speed", "item/soul_lantern"}, {"swift_sneak", "item/echo_shard"},
                {"sharpness", "item/iron_sword"}, {"smite", "item/golden_sword"}, {"bane_of_arthropods", "item/spider_eye"},
                {"knockback", "item/stick"}, {"fire_aspect", "item/blaze_powder"}, {"looting", "item/emerald"},
                {"sweeping", "item/diamond_sword"}, {"power", "item/bow"}, {"punch", "item/spectral_arrow"}, {"flame", "item/fire_charge"},
                {"impaling", "item/trident"}, {"efficiency", "item/iron_pickaxe"}, {"fortune", "item/diamond"},
                {"luck_of_the_sea", "item/fishing_rod"}, {"unbreaking", "item/iron_ingot"}, {"mending", "item/experience_bottle"},
                {"sundered", "item/netherite_scrap"}, {"sculk_decay", "block/sculk_vein"},
        };
        for (String[] icon : icons) {
            ICONS.put(icon[0], icon[1]);
        }
    }

    /** A 16x16 item centred in the 18x18 icon. */
    static BufferedImage icon(String path) throws Exception {
        BufferedImage item = texture(path);
        BufferedImage image = new BufferedImage(18, 18, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 16; ++y) {
            for (int x = 0; x < 16; ++x) {
                image.setRGB(x + 1, y + 1, item.getRGB(x, y));
            }
        }
        return image;
    }

    static BufferedImage gildedIcon() {
        BufferedImage image = new BufferedImage(18, 18, BufferedImage.TYPE_INT_ARGB);
        String[] rows = {
                "                  ", "       aaaa       ", "      abbbba      ", "      abccba      ", "      abbbba      ",
                "       abba       ", "    aaaabbaaaa    ", "   abbbbccbbbba   ", "   ab abccba ba   ", "   ab abbbba ba   ",
                "   aa abbbba aa   ", "      abaaba      ", "      ab  ba      ", "      ab  ba      ", "     aab  baa     ",
                "    aaaaaaaaaa    ", "    addddddddda   ", "    aaaaaaaaaa    "};
        int[] colours = {0x6B4507, 0xEEC52E, 0xFFF39A, 0xB8860F};
        for (int y = 0; y < 18; ++y) {
            for (int x = 0; x < 18; ++x) {
                char ch = rows[y].charAt(x);
                if (ch != ' ') {
                    image.setRGB(x, y, withAlpha(colours[ch - 'a'], 255));
                }
            }
        }
        return image;
    }
}
