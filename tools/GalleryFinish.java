import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.GlyphVector;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/**
 * Finishes the dev client's gallery pictures (debug/Gallery, gradlew runClient -Pgallery): each was
 * drawn twice, over black and over white, so the pair gives every pixel's colour and coverage. Sized
 * for the CurseForge page: the monstrosities 850 px wide, and the other bosses and their summoning pages
 * (cut from the book) 400 each, and each boss and its page side by side in one 850 px picture; scaled down
 * from the full-size drawings, averaging, so edges stay smooth. Each mold's two layers, trimmed
 * alike so they line up, on their own and laid side by side 850 px wide with labels and a legend of the
 * blocks (the Sculk mold's with a close-up of its centre underneath).
 *
 * Usage: java GalleryFinish.java <gallery dir> <out dir>
 */
public class GalleryFinish {
    static final int GOLEM_WIDTH = 850, BOSS_WIDTH = 400;

    public static void main(String[] args) throws Exception {
        File in = new File(args[0]), out = new File(args[1]);
        java.util.Map<String, BufferedImage> bosses = new java.util.HashMap<>();
        for (String name : names(new File(in, "mobs"))) {
            BufferedImage image = matte(new File(in, "mobs"), name);
            image = trim(image, bounds(image), 32);
            save(toWidth(image, name.contains("monstrosity") ? GOLEM_WIDTH : BOSS_WIDTH), new File(out, "mobs/" + name + ".png"));
            bosses.put(name, image);
        }
        for (String name : names(new File(in, "summoning"))) {
            BufferedImage image = matte(new File(in, "summoning"), name);
            String[] r = Files.readString(new File(in, "summoning/" + name + ".rect").toPath()).trim().split(" ");
            int x = Integer.parseInt(r[0]), y = Integer.parseInt(r[1]), w = Integer.parseInt(r[2]), h = Integer.parseInt(r[3]);
            // The left page, with the whole of the spine's stitching (it runs 2 GUI pixels past the middle),
            // as wide as the boss beside it.
            BufferedImage page = toWidth(image.getSubimage(x, y, w / 2 + 8, h), BOSS_WIDTH);
            save(page, new File(out, "summoning/" + name + ".png"));
            // And the boss beside its page in one picture (the Black Book entry "cornelia" is the mob "captain_cornelia").
            BufferedImage boss = bosses.getOrDefault(name, bosses.get("captain_" + name));
            if (boss != null) {
                save(withPage(boss, page), new File(out, "bosses/" + name + ".png"));
            }
        }
        TreeSet<String> molds = new TreeSet<>();
        for (String name : names(new File(in, "molds"))) {
            molds.add(name.replaceAll("_(bottom|top|centre)$", ""));
        }
        for (String mold : molds) {
            BufferedImage bottom = matte(new File(in, "molds"), mold + "_bottom"), top = matte(new File(in, "molds"), mold + "_top");
            int[] a = bounds(bottom), b = bounds(top);
            int[] both = {Math.min(a[0], b[0]), Math.min(a[1], b[1]), Math.max(a[2], b[2]), Math.max(a[3], b[3])};
            bottom = trim(bottom, both, 24);
            top = trim(top, both, 24);
            save(halve(bottom), new File(out, "molds/" + mold + "_bottom_layer.png"));
            save(halve(top), new File(out, "molds/" + mold + "_top_layer.png"));
            BufferedImage centre = null;
            if (new File(in, "molds/" + mold + "_centre.black.png").exists()) {
                centre = matte(new File(in, "molds"), mold + "_centre");
                centre = trim(centre, bounds(centre), 24);
                save(halve(centre), new File(out, "molds/" + mold + "_centre.png"));
            }
            save(layers(bottom, top, centre, LEGENDS.getOrDefault(mold, "")), new File(out, "molds/" + mold + "_layers.png"));
        }
    }

    // The base names of the black/white pairs in a folder.
    static List<String> names(File dir) {
        List<String> names = new ArrayList<>();
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.getName().endsWith(".black.png")) {
                    names.add(file.getName().substring(0, file.getName().length() - ".black.png".length()));
                }
            }
        }
        names.sort(null);
        return names;
    }

    /**
     * Over black a pixel shows c*a; over white, c*a + (1 - a). So a = 1 - (white - black), and the
     * colour is black / a. Kept premultiplied (colour times coverage) until saved, so halving averages
     * correctly.
     */
    static BufferedImage matte(File dir, String name) throws Exception {
        BufferedImage black = ImageIO.read(new File(dir, name + ".black.png")), white = ImageIO.read(new File(dir, name + ".white.png"));
        int w = black.getWidth(), h = black.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB_PRE);
        for (int y = 0; y < h; ++y) {
            for (int x = 0; x < w; ++x) {
                int k = black.getRGB(x, y), l = white.getRGB(x, y);
                int spread = ((l >> 16 & 255) - (k >> 16 & 255) + (l >> 8 & 255) - (k >> 8 & 255) + (l & 255) - (k & 255)) / 3;
                int alpha = Math.max(0, Math.min(255, 255 - spread));
                int r = Math.min(alpha, k >> 16 & 255), g = Math.min(alpha, k >> 8 & 255), b = Math.min(alpha, k & 255);
                // (Into the raster as is: setRGB would take it for straight colour and premultiply it again.)
                out.getRaster().setPixel(x, y, new int[]{r, g, b, alpha});
            }
        }
        return out;
    }

    // Half size, each pixel the average of four (in premultiplied colour).
    static BufferedImage halve(BufferedImage image) {
        int w = image.getWidth() / 2, h = image.getHeight() / 2;
        int[] src = image.getRaster().getPixels(0, 0, image.getWidth(), image.getHeight(), (int[]) null);
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB_PRE);
        int[] dst = new int[w * h * 4];
        int stride = image.getWidth() * 4;
        for (int y = 0; y < h; ++y) {
            for (int x = 0; x < w; ++x) {
                for (int c = 0; c < 4; ++c) {
                    int i = (2 * y) * stride + (2 * x) * 4 + c;
                    dst[(y * w + x) * 4 + c] = (src[i] + src[i + 4] + src[i + stride] + src[i + stride + 4] + 2) / 4;
                }
            }
        }
        out.getRaster().setPixels(0, 0, w, h, dst);
        return out;
    }

    // Scaled to the given width (height to match), each new pixel the average of the old ones it covers.
    static BufferedImage toWidth(BufferedImage image, int width) {
        return resize(image, width, (int) Math.round((double) image.getHeight() * width / image.getWidth()));
    }

    static BufferedImage resize(BufferedImage image, int w, int h) {
        int sw = image.getWidth(), sh = image.getHeight();
        int[] src = image.getRaster().getPixels(0, 0, sw, sh, (int[]) null);
        double[] across = new double[w * sh * 4];
        double sx = (double) sw / w, sy = (double) sh / h;
        for (int x = 0; x < w; ++x) {
            double x0 = x * sx, x1 = (x + 1) * sx;
            for (int i = (int) x0; i < Math.min(sw, (int) Math.ceil(x1)); ++i) {
                double weight = (Math.min(i + 1, x1) - Math.max(i, x0)) / sx;
                for (int y = 0; y < sh; ++y) {
                    for (int c = 0; c < 4; ++c) {
                        across[(y * w + x) * 4 + c] += src[(y * sw + i) * 4 + c] * weight;
                    }
                }
            }
        }
        int[] dst = new int[w * h * 4];
        for (int y = 0; y < h; ++y) {
            double y0 = y * sy, y1 = (y + 1) * sy;
            double[] sum = new double[w * 4];
            for (int j = (int) y0; j < Math.min(sh, (int) Math.ceil(y1)); ++j) {
                double weight = (Math.min(j + 1, y1) - Math.max(j, y0)) / sy;
                for (int k = 0; k < w * 4; ++k) {
                    sum[k] += across[j * w * 4 + k] * weight;
                }
            }
            for (int k = 0; k < w * 4; ++k) {
                dst[y * w * 4 + k] = (int) Math.min(255, Math.round(sum[k]));
            }
        }
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB_PRE);
        out.getRaster().setPixels(0, 0, w, h, dst);
        return out;
    }

    // A boss and its summoning page side by side, 850 px wide: each in its own 400 px half, 50 apart, the boss
    // no taller than 560 (so a tall one doesn't tower over the page), both centred top to bottom.
    static BufferedImage withPage(BufferedImage boss, BufferedImage page) {
        double scale = Math.min((double) BOSS_WIDTH / boss.getWidth(), 560.0 / boss.getHeight());
        BufferedImage left = resize(boss, (int) Math.round(boss.getWidth() * scale), (int) Math.round(boss.getHeight() * scale));
        int height = Math.max(left.getHeight(), page.getHeight());
        BufferedImage out = new BufferedImage(GOLEM_WIDTH, height, BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics2D g = out.createGraphics();
        g.drawImage(left, (BOSS_WIDTH - left.getWidth()) / 2, (height - left.getHeight()) / 2, null);
        g.drawImage(page, GOLEM_WIDTH - page.getWidth(), (height - page.getHeight()) / 2, null);
        g.dispose();
        return out;
    }

    // Where anything shows (x0, y0, x1, y1).
    static int[] bounds(BufferedImage image) {
        int x0 = image.getWidth(), y0 = image.getHeight(), x1 = -1, y1 = -1;
        for (int y = 0; y < image.getHeight(); ++y) {
            for (int x = 0; x < image.getWidth(); ++x) {
                if ((image.getRGB(x, y) >>> 24) > 3) {
                    x0 = Math.min(x0, x);
                    y0 = Math.min(y0, y);
                    x1 = Math.max(x1, x);
                    y1 = Math.max(y1, y);
                }
            }
        }
        return new int[]{x0, y0, x1, y1};
    }

    static BufferedImage trim(BufferedImage image, int[] box, int margin) {
        int x0 = Math.max(0, box[0] - margin), y0 = Math.max(0, box[1] - margin);
        int x1 = Math.min(image.getWidth() - 1, box[2] + margin), y1 = Math.min(image.getHeight() - 1, box[3] + margin);
        return image.getSubimage(x0, y0, x1 - x0 + 1, y1 - y0 + 1);
    }

    // What each mold is built from, under its pictures.
    static final java.util.Map<String, String> LEGENDS = new java.util.HashMap<>();

    static {
        String core = "Core: Reinforced Redstone Block";
        LEGENDS.put("copper_monstrosity", "Ring: Lava     Fill: any Copper Block     " + core);
        LEGENDS.put("iron_monstrosity", "Ring: Lava     Fill: Iron Blocks     " + core);
        LEGENDS.put("gold_monstrosity", "Ring: Lava     Fill: Gold Blocks     " + core);
        LEGENDS.put("lapis_monstrosity", "Ring: Lava     Fill: Lapis Lazuli Blocks     " + core);
        LEGENDS.put("diamond_monstrosity", "Ring: Lava     Fill: Diamond Blocks     " + core);
        LEGENDS.put("netherite_monstrosity", "Ring: Lava     Fill: Netherite Blocks     " + core);
        LEGENDS.put("sculk_monstrosity", "Ring: Sculk Sensors     Fill: Sculk Catalysts     Centre: 4 Sculk Shriekers     Core: Warden Core");
        LEGENDS.put("mira_monstrosity", "Ring: Lava     Fill: Orange Terracotta     " + core);
        LEGENDS.put("mira_monstrosity_obsidian", "Ring: Obsidian     Fill: Orange Terracotta     " + core);
        LEGENDS.put("mira_monstrosity_warden", "Ring: Sculk     Fill: Orange Terracotta     " + core);
    }

    // The two layers side by side, 850 px wide, each titled above; a close-up of the centre under them if
    // given; the legend at the bottom. Drawn at the final size, so the text stays sharp.
    static BufferedImage layers(BufferedImage bottom, BufferedImage top, BufferedImage centre, String legend) {
        int width = GOLEM_WIDTH, gap = 24, title = 42, panel = (width - gap) / 2;
        BufferedImage left = toWidth(bottom, panel), right = toWidth(top, panel), middle = centre == null ? null : toWidth(centre, 300);
        int rows = Math.max(left.getHeight(), right.getHeight());
        int height = title + rows + (middle == null ? 0 : 12 + title + middle.getHeight()) + 44;
        BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        caption(g, "Bottom layer", panel / 2, title - 12, 28);
        caption(g, "Top layer", panel + gap + panel / 2, title - 12, 28);
        g.drawImage(left, 0, title, null);
        g.drawImage(right, panel + gap, title, null);
        int y = title + rows;
        if (middle != null) {
            y += 12 + title;
            caption(g, "Centre", width / 2, y - 12, 28);
            g.drawImage(middle, (width - middle.getWidth()) / 2, y, null);
            y += middle.getHeight();
        }
        int size = 22;
        while (size > 12 && new Font("SansSerif", Font.BOLD, size).createGlyphVector(g.getFontRenderContext(), legend).getVisualBounds().getWidth() > width - 24) {
            --size;
        }
        caption(g, legend, width / 2, y + 30, size);
        g.dispose();
        return out;
    }

    // White bold text with a dark outline, centred on x.
    static void caption(Graphics2D g, String text, int x, int baseline, int size) {
        Font font = new Font("SansSerif", Font.BOLD, size);
        GlyphVector glyphs = font.createGlyphVector(g.getFontRenderContext(), text);
        java.awt.Shape shape = glyphs.getOutline((float) (x - glyphs.getVisualBounds().getWidth() / 2), baseline);
        g.setStroke(new BasicStroke(Math.max(3, size / 5.4f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(20, 14, 30));
        g.draw(shape);
        g.setColor(Color.WHITE);
        g.fill(shape);
    }

    // Saved with straight (not premultiplied) colour, as PNG keeps it.
    static void save(BufferedImage image, File file) throws Exception {
        file.getParentFile().mkdirs();
        BufferedImage straight = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = straight.createGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();
        ImageIO.write(straight, "png", file);
    }
}
