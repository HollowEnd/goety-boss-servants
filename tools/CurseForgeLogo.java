import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;

/**
 * Draws the mod's title logo for its CurseForge page, in the style of Aquamirae's: chunky pixel
 * letters as blocks in perspective, a dark outline, a cloudy painted fill and a soft glow, on a
 * transparent background. "GOETY" is seen from below (its face widening down to a deep underside
 * that tucks back in), in soul fire: cyan faces over a violet underside. "{BOSS SERVANTS}" sits under
 * it seen from above, crown gold with lit cream tops meeting the title's underside.
 *
 * Usage: java CurseForgeLogo.java <out.png>
 */
public class CurseForgeLogo {
    // The title's glyphs: 6x7, two-unit uprights ('#' filled).
    static final Map<Character, String[]> TITLE = new HashMap<>();
    // The band's: squat 6x5 (braces 4x5), two-unit uprights, one-unit bars.
    static final Map<Character, String[]> BAND = new HashMap<>();

    static {
        TITLE.put('G', new String[]{".#####", "##....", "##....", "##.###", "##..##", "##..##", ".####."});
        TITLE.put('O', new String[]{".####.", "##..##", "##..##", "##..##", "##..##", "##..##", ".####."});
        TITLE.put('E', new String[]{"######", "##....", "##....", "#####.", "##....", "##....", "######"});
        TITLE.put('T', new String[]{"######", "..##..", "..##..", "..##..", "..##..", "..##..", "..##.."});
        TITLE.put('Y', new String[]{"##..##", "##..##", "##..##", ".####.", "..##..", "..##..", "..##.."});

        BAND.put('A', new String[]{".####.", "##..##", "######", "##..##", "##..##"});
        BAND.put('B', new String[]{"#####.", "##..##", "#####.", "##..##", "#####."});
        BAND.put('E', new String[]{"######", "##....", "#####.", "##....", "######"});
        BAND.put('N', new String[]{"##..##", "###.##", "######", "##.###", "##..##"});
        BAND.put('O', new String[]{".####.", "##..##", "##..##", "##..##", ".####."});
        BAND.put('R', new String[]{"#####.", "##..##", "#####.", "##.##.", "##..##"});
        BAND.put('S', new String[]{".#####", "##....", ".####.", "....##", "#####."});
        BAND.put('T', new String[]{"######", "..##..", "..##..", "..##..", "..##.."});
        BAND.put('V', new String[]{"##..##", "##..##", "##..##", ".####.", "..##.."});
        BAND.put('{', new String[]{".###", ".##.", "##..", ".##.", ".###"});
        BAND.put('}', new String[]{"###.", ".##.", "..##", ".##.", "###."});
    }

    // Fill palettes, light to dark.
    static final int[] SOUL = {0xFFF4FFFF, 0xFFA8F4FF, 0xFF3FD6F2, 0xFF2E8BE0, 0xFF6C49D6, 0xFF4A2390};
    static final int[] CROWN = {0xFFFFF8D6, 0xFFFFE27A, 0xFFF5BA34, 0xFFE8902A, 0xFFC95A2A, 0xFF9A2F3A};
    // The title's underside (its darkest), the band's tops (front edge, back), the outlines.
    static final int SOUL_UNDER = 0xFF150F3A;
    static final int[] CROWN_TOP = {0xFFFFF2C4, 0xFFD88A30};
    static final int SOUL_OUTLINE = 0xFF14123F, CROWN_OUTLINE = 0xFF2A0C0C;
    static final int LILAC = 0xFFD7A6FF;

    static final int W = 2000, H = 860, CX = W / 2;

    public static void main(String[] args) throws Exception {
        int titleUnit = 50, bandUnit = 17;
        String title = "GOETY", band = "{BOSS SERVANTS}";
        int titleX = CX - width(TITLE, title) * titleUnit / 2, titleY = 140;
        int titleDrop = (int) Math.round(7 * titleUnit * 0.42), bandDrop = Math.round(5 * bandUnit * 0.5f);
        int bandX = CX - width(BAND, band) * bandUnit / 2, bandY = titleY + 7 * titleUnit + (int) (titleDrop * 0.75) + bandDrop;

        // The title from below: its face widens a little toward the seam, and its underside drops,
        // flaring out a touch. The band from above: its tops rise, tucking in a little.
        Layer top = new Layer(TITLE, title, titleX, titleY, titleUnit, 7, SOUL, 0.06, 0.6, 1, titleDrop, 0.02, 0.055, SOUL_OUTLINE, 11);
        Layer bottom = new Layer(BAND, band, bandX, bandY, bandUnit, 5, CROWN, 0.46, 0.5, -1, bandDrop, -0.02, 0.0, CROWN_OUTLINE, 29);

        // A soul-fire haze behind the title, wide and faint, risen a little above it; a tighter warm
        // one round the band.
        float[] glow = new float[W * H], warm = new float[W * H];
        for (int i = 0; i < W * H; ++i) {
            glow[i] = top.solid[i] ? 1.0f : 0.0f;
            warm[i] = bottom.solid[i] ? 1.0f : 0.0f;
        }
        for (int pass = 0; pass < 3; ++pass) {
            glow = blur(glow, 40);
            warm = blur(warm, 16);
        }
        int rise = 30;
        int[] canvas = new int[W * H];
        for (int i = 0; i < W * H; ++i) {
            float g = i + rise * W < W * H ? glow[i + rise * W] : 0.0f;
            float a = Math.min(1.0f, g * 1.5f);
            canvas[i] = argb(Math.round(a * 150), 70, 215, 255);
            canvas[i] = over(argb(Math.round(Math.min(1.0f, warm[i] * 1.5f) * 100), 255, 150, 60), canvas[i]);
            canvas[i] = over(top.pixels[i], canvas[i]);
            canvas[i] = over(bottom.pixels[i], canvas[i]);
        }

        BufferedImage out = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        out.setRGB(0, 0, W, H, canvas, 0, W);
        ImageIO.write(crop(out, 12), "png", new File(args[0]));
    }

    static int width(Map<Character, String[]> font, String text) {
        int w = 0;
        for (int i = 0; i < text.length(); ++i) {
            char c = text.charAt(i);
            w += c == ' ' ? 2 : font.get(c)[0].length();
            if (i + 1 < text.length()) {
                w += 1;
            }
        }
        return w;
    }

    /** One line of the logo: its letters' faces, the block faces beside them and the outline round both. */
    static class Layer {
        final int[] pixels = new int[W * H];
        final boolean[] solid = new boolean[W * H];

        /**
         * @param tone0    where on the palette a letter's face starts (its top)
         * @param toneSpan how far down the palette it goes by its foot
         * @param dir      1: the block face shows below the letters (seen from below); -1: above (seen from above)
         * @param drop     the block face's depth, in pixels
         * @param spread   how much it spreads out from the middle at full depth (negative: tucks in)
         * @param keystone how much narrower the letters' faces are at the top than at the foot
         */
        Layer(Map<Character, String[]> font, String text, int x0, int y0, int unit, int rows, int[] palette, double tone0, double toneSpan,
              int dir, int drop, double spread, double keystone, int outline, int seed) {
            int height = rows * unit;
            boolean[] flat = new boolean[W * H];
            int x = x0;
            for (char c : text.toCharArray()) {
                if (c == ' ') {
                    x += 3 * unit;
                    continue;
                }
                String[] glyph = font.get(c);
                for (int gy = 0; gy < rows; ++gy) {
                    for (int gx = 0; gx < glyph[gy].length(); ++gx) {
                        if (glyph[gy].charAt(gx) == '#') {
                            fill(flat, x + gx * unit, y0 + gy * unit, unit, unit);
                        }
                    }
                }
                x += (glyph[0].length() + 1) * unit;
            }
            // Perspective on the face: each row drawn narrower toward the top.
            boolean[] face = new boolean[W * H];
            for (int Y = Math.max(0, y0); Y < Math.min(H, y0 + height); ++Y) {
                double k = 1.0 - keystone * (1.0 - (Y - y0 + 0.5) / height);
                for (int X = 0; X < W; ++X) {
                    int sx = (int) Math.floor(CX + (X - CX) / k);
                    face[Y * W + X] = sx >= 0 && sx < W && flat[Y * W + sx];
                }
            }

            // The block faces: the letters pushed back (down or up), spreading out from the middle or
            // tucking in toward it, the shallowest showing. The title's underside runs into its
            // letters' holes and gaps, in violet.
            int[] depth = new int[W * H];
            for (int d = drop; d >= 1; --d) {
                double s = 1.0 + spread * d / drop;
                int lo = dir > 0 ? Math.max(d, y0) : Math.max(0, y0 - d), hi = dir > 0 ? Math.min(H, y0 + height + d) : Math.min(H, y0 + height - d);
                for (int Y = lo; Y < hi; ++Y) {
                    int from = Y - dir * d;
                    for (int X = 0; X < W; ++X) {
                        int sx = (int) Math.floor(CX + (X - CX) / s);
                        if (sx >= 0 && sx < W && face[from * W + sx] && !face[Y * W + X]) {
                            depth[Y * W + X] = d;
                        }
                    }
                }
            }
            for (int i = 0; i < W * H; ++i) {
                if (depth[i] == 0) {
                    continue;
                }
                int X = i % W, Y = i / W;
                float t = (float) depth[i] / drop;
                double n = cloud(X, Y, unit, 1, seed + 5);
                int c;
                if (dir > 0) {
                    // The underside: the face's clouds, darkened into violet, catching the band's warm
                    // light along the bottom of the title; a pale rim at the foot.
                    c = lerp(gradient(palette, clamp(0.72 + (n - 0.5) * 0.8)), SOUL_UNDER, 0.58f + t * 0.22f);
                    float m = (float) fbm(X / (unit * 0.9), Y / (unit * 0.5), seed + 13);
                    if (m > 0.55f) {
                        c = lerp(c, palette[4], Math.min(0.55f, (m - 0.55f) * 2.2f) * (1.0f - t * 0.5f));
                    }
                    float low = (float) clamp((Y - (y0 + height + drop * 0.4)) / (drop * 0.6));
                    c = lerp(c, CROWN[3], (float) (low * low * (0.25 + 0.4 * n)));
                    if (t > 0.9f) {
                        c = lerp(c, palette[3], 0.3f);
                    }
                } else {
                    // The tops: cream at the front edge going gold toward the back, softly clouded. Where
                    // one shows through a letter's gap or hole it's in shadow, so the gap still reads.
                    c = lerp(CROWN_TOP[0], CROWN_TOP[1], (float) clamp(t * 0.85 + (n - 0.5) * 0.5));
                    if (Y >= y0) {
                        c = lerp(c, outline, 0.7f);
                    }
                }
                pixels[i] = c;
                solid[i] = true;
            }

            // The letters' faces: soft warped clouds over the top-to-bottom gradient, in small texture
            // pixels, lit along the top and left edges.
            int cell = Math.max(2, unit / 12), edge = Math.max(3, unit / 9);
            for (int Y = 0; Y < H; ++Y) {
                for (int X = 0; X < W; ++X) {
                    int i = Y * W + X;
                    if (!face[i]) {
                        continue;
                    }
                    double n = cloud(X, Y, unit, cell, seed);
                    double t = (double) ((Y / cell) * cell - y0) / height;
                    int c = gradient(palette, clamp(tone0 + t * toneSpan + (n - 0.5) * 1.15));
                    // Iridescent glints where the clouds are thickest: white, or lilac low on the title.
                    if (n > 0.66) {
                        boolean high = t < 0.55 || dir < 0;
                        c = lerp(c, high ? palette[0] : LILAC, (float) Math.min(high ? 0.85 : 0.5, (n - 0.66) * 4.0));
                    }
                    // (Not on faces seen from above: their lit tops are the edge.)
                    if (dir > 0 && (!in(face, X, Y - edge) || !in(face, X - edge, Y))) {
                        c = lerp(c, 0xFFFFFFFF, 0.3f);
                    }
                    pixels[i] = c;
                    solid[i] = true;
                }
            }

            // The outline, square-cornered, round faces and blocks alike; holes inside letters are
            // filled dark, not left see-through.
            boolean[] ring = grow(solid, Math.max(dir < 0 ? 14 : 6, Math.round(unit * 0.5f)));
            for (int i = 0; i < W * H; ++i) {
                if (ring[i] && !solid[i]) {
                    pixels[i] = outline;
                }
            }
            boolean[] enclosed = holes(ring);
            for (int i = 0; i < W * H; ++i) {
                if (enclosed[i]) {
                    pixels[i] = outline;
                    ring[i] = true;
                }
            }
            System.arraycopy(ring, 0, solid, 0, W * H);
        }
    }

    // The pixels a mask encloses: those not reachable from the image's edge without crossing it.
    static boolean[] holes(boolean[] mask) {
        boolean[] outside = new boolean[W * H];
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        for (int X = 0; X < W; ++X) {
            queue.add(X);
            queue.add((H - 1) * W + X);
        }
        for (int Y = 0; Y < H; ++Y) {
            queue.add(Y * W);
            queue.add(Y * W + W - 1);
        }
        while (!queue.isEmpty()) {
            int i = queue.poll();
            if (outside[i] || mask[i]) {
                continue;
            }
            outside[i] = true;
            int X = i % W, Y = i / W;
            if (X > 0) {
                queue.add(i - 1);
            }
            if (X < W - 1) {
                queue.add(i + 1);
            }
            if (Y > 0) {
                queue.add(i - W);
            }
            if (Y < H - 1) {
                queue.add(i + W);
            }
        }
        boolean[] enclosed = new boolean[W * H];
        for (int i = 0; i < W * H; ++i) {
            enclosed[i] = !mask[i] && !outside[i];
        }
        return enclosed;
    }

    // Domain-warped clouds, sampled in cells x cells squares.
    static double cloud(int X, int Y, int unit, int cell, int seed) {
        double px = (double) ((X / cell) * cell) / (unit * 2.0), py = (double) ((Y / cell) * cell) / (unit * 2.0);
        double wx = fbm(px, py, seed), wy = fbm(px + 5.2, py + 1.3, seed);
        return fbm(px + 1.6 * wx, py + 1.6 * wy, seed + 3);
    }

    static void fill(boolean[] mask, int x, int y, int w, int h) {
        for (int Y = Math.max(0, y); Y < Math.min(H, y + h); ++Y) {
            for (int X = Math.max(0, x); X < Math.min(W, x + w); ++X) {
                mask[Y * W + X] = true;
            }
        }
    }

    static boolean in(boolean[] mask, int x, int y) {
        return x >= 0 && y >= 0 && x < W && y < H && mask[y * W + x];
    }

    // Grows a mask by r pixels with a square brush (distance to the nearest set pixel along the row, then up and down).
    static boolean[] grow(boolean[] mask, int r) {
        int[] dx = new int[W * H];
        int far = W + H;
        for (int y = 0; y < H; ++y) {
            int last = -far;
            for (int x = 0; x < W; ++x) {
                if (mask[y * W + x]) {
                    last = x;
                }
                dx[y * W + x] = x - last;
            }
            last = 2 * far;
            for (int x = W - 1; x >= 0; --x) {
                if (mask[y * W + x]) {
                    last = x;
                }
                dx[y * W + x] = Math.min(dx[y * W + x], last - x);
            }
        }
        boolean[] out = new boolean[W * H];
        for (int y = 0; y < H; ++y) {
            for (int x = 0; x < W; ++x) {
                for (int oy = -r; oy <= r && !out[y * W + x]; ++oy) {
                    int yy = y + oy;
                    if (yy >= 0 && yy < H && dx[yy * W + x] <= r) {
                        out[y * W + x] = true;
                    }
                }
            }
        }
        return out;
    }

    static float[] blur(float[] in, int r) {
        float[] tmp = new float[W * H], out = new float[W * H];
        for (int y = 0; y < H; ++y) {
            float sum = 0;
            for (int x = -r; x <= r; ++x) {
                sum += in[y * W + Math.max(0, Math.min(W - 1, x))];
            }
            for (int x = 0; x < W; ++x) {
                tmp[y * W + x] = sum / (2 * r + 1);
                sum += in[y * W + Math.min(W - 1, x + r + 1)] - in[y * W + Math.max(0, x - r)];
            }
        }
        for (int x = 0; x < W; ++x) {
            float sum = 0;
            for (int y = -r; y <= r; ++y) {
                sum += tmp[Math.max(0, Math.min(H - 1, y)) * W + x];
            }
            for (int y = 0; y < H; ++y) {
                out[y * W + x] = sum / (2 * r + 1);
                sum += tmp[Math.min(H - 1, y + r + 1) * W + x] - tmp[Math.max(0, y - r) * W + x];
            }
        }
        return out;
    }

    // Value noise, four octaves, in [0, 1].
    static double fbm(double x, double y, int seed) {
        double sum = 0, amp = 0.5, norm = 0;
        for (int o = 0; o < 4; ++o) {
            sum += amp * noise(x, y, seed + o * 101);
            norm += amp;
            x *= 2.03;
            y *= 2.03;
            amp *= 0.5;
        }
        return sum / norm;
    }

    static double noise(double x, double y, int seed) {
        int ix = (int) Math.floor(x), iy = (int) Math.floor(y);
        double fx = x - ix, fy = y - iy;
        fx = fx * fx * (3 - 2 * fx);
        fy = fy * fy * (3 - 2 * fy);
        double a = hash(ix, iy, seed), b = hash(ix + 1, iy, seed), c = hash(ix, iy + 1, seed), d = hash(ix + 1, iy + 1, seed);
        return (a + (b - a) * fx) * (1 - fy) + (c + (d - c) * fx) * fy;
    }

    static double hash(int x, int y, int seed) {
        int h = x * 374761393 + y * 668265263 + seed * 1442695041;
        h = (h ^ (h >>> 13)) * 1274126177;
        h ^= h >>> 16;
        return (h & 0xFFFFFF) / (double) 0xFFFFFF;
    }

    static double clamp(double t) {
        return Math.max(0.0, Math.min(1.0, t));
    }

    static int gradient(int[] palette, double t) {
        double p = t * (palette.length - 1);
        int i = Math.min(palette.length - 2, (int) p);
        return lerp(palette[i], palette[i + 1], (float) (p - i));
    }

    static int lerp(int a, int b, float t) {
        t = Math.max(0.0f, Math.min(1.0f, t));
        int r = Math.round((a >> 16 & 255) * (1 - t) + (b >> 16 & 255) * t);
        int g = Math.round((a >> 8 & 255) * (1 - t) + (b >> 8 & 255) * t);
        int bl = Math.round((a & 255) * (1 - t) + (b & 255) * t);
        int al = Math.round((a >>> 24) * (1 - t) + (b >>> 24) * t);
        return al << 24 | r << 16 | g << 8 | bl;
    }

    static int argb(int a, int r, int g, int b) {
        return Math.max(0, Math.min(255, a)) << 24 | r << 16 | g << 8 | b;
    }

    // Source-over compositing of straight-alpha pixels.
    static int over(int src, int dst) {
        float sa = (src >>> 24) / 255f, da = (dst >>> 24) / 255f;
        float oa = sa + da * (1 - sa);
        if (oa <= 0) {
            return 0;
        }
        int r = Math.round(((src >> 16 & 255) * sa + (dst >> 16 & 255) * da * (1 - sa)) / oa);
        int g = Math.round(((src >> 8 & 255) * sa + (dst >> 8 & 255) * da * (1 - sa)) / oa);
        int b = Math.round(((src & 255) * sa + (dst & 255) * da * (1 - sa)) / oa);
        return Math.round(oa * 255) << 24 | r << 16 | g << 8 | b;
    }

    // Trims the transparent margin, keeping a little.
    static BufferedImage crop(BufferedImage img, int margin) {
        int minX = W, minY = H, maxX = 0, maxY = 0;
        for (int y = 0; y < H; ++y) {
            for (int x = 0; x < W; ++x) {
                if ((img.getRGB(x, y) >>> 24) > 1) {
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                }
            }
        }
        minX = Math.max(0, minX - margin);
        minY = Math.max(0, minY - margin);
        maxX = Math.min(W - 1, maxX + margin);
        maxY = Math.min(H - 1, maxY + margin);
        return img.getSubimage(minX, minY, maxX - minX + 1, maxY - minY + 1);
    }
}
