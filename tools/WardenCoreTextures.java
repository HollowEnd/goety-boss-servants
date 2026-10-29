import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Random;

/**
 * Builds the Warden Core's textures from the vanilla warden's, at the warden's own pixel size
 * (one texel per block pixel, nothing stretched):
 * - top: the warden's chest as the warden shows it: its torso front, the souls of its heart, and
 *   its ribcage over both (WardenModel's right_ribcage and left_ribcage, two mirrored planes just in
 *   front of the chest). 16 of the chest's 18 columns, the ring's outer shading column cut on each
 *   side. The souls are faint on the block, since the renderer pulses them on top like the warden's
 *   heart layer; full on the item.
 * - souls: the heart layer on the same 16x16, less what the ribs cover (the renderer's glow).
 * - side and bottom: the warden's skin, its near-black flesh with the sparse lighter specks and the
 *   teal pulsating spots of its torso.
 *
 * Usage: java WardenCoreTextures.java <vanilla textures dir (assets/minecraft/textures)> <resources/assets/goetybossservants>...
 */
public class WardenCoreTextures {
    // The torso's front face in warden.png starts at u 11, v 11 (18x21); the crop is u 12..27, v 14..29.
    static final int CHEST_U = 11, CROP_COL = 1, CROP_V = 14;
    // The ribcage plane's texture (9x21, at the chest's rows), the left one mirrored onto the other half.
    static final int RIBS_U = 90;
    // How much of the souls the block's own texture shows (the renderer's heartbeat does the rest).
    static final float BLOCK_SOULS = 0.3f;

    // The warden torso's skin colours (warden.png and warden_pulsating_spots_*.png).
    static final int FLESH = 0xFF0D1217, SPECK = 0xFF111B21, DARK_SPECK = 0xFF11151A, TEAL_SPECK = 0xFF0B212A, SPOT = 0xFF034150;

    public static void main(String[] args) throws Exception {
        File vanilla = new File(args[0], "entity/warden");
        BufferedImage warden = ImageIO.read(new File(vanilla, "warden.png"));
        BufferedImage heart = ImageIO.read(new File(vanilla, "warden_heart.png"));
        for (int i = 1; i < args.length; ++i) {
            File blocks = new File(args[i], "textures/block");
            File entities = new File(args[i], "textures/entity");
            blocks.mkdirs();
            entities.mkdirs();
            ImageIO.write(chest(warden, heart, BLOCK_SOULS), "png", new File(blocks, "warden_core_top.png"));
            ImageIO.write(chest(warden, heart, 1.0f), "png", new File(blocks, "warden_core_top_lit.png"));
            ImageIO.write(souls(warden, heart), "png", new File(entities, "warden_core_souls.png"));
            ImageIO.write(skin(1), "png", new File(blocks, "warden_core_side.png"));
            ImageIO.write(skin(2), "png", new File(blocks, "warden_core_bottom.png"));
        }
    }

    // The ribcage's pixel over chest column col (0..17): the right plane's 9 columns, then the left's mirrored.
    static int rib(BufferedImage warden, int col, int v) {
        return warden.getRGB(RIBS_U + (col < 9 ? col : 17 - col), v);
    }

    static BufferedImage chest(BufferedImage warden, BufferedImage heart, float souls) {
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 16; ++y) {
            for (int x = 0; x < 16; ++x) {
                int col = CROP_COL + x, v = CROP_V + y;
                int rib = rib(warden, col, v);
                if (rib >>> 24 != 0) {
                    image.setRGB(x, y, rib);
                    continue;
                }
                int soul = heart.getRGB(CHEST_U + col, v);
                image.setRGB(x, y, blend(warden.getRGB(CHEST_U + col, v), soul, (soul >>> 24) / 255.0f * souls));
            }
        }
        return image;
    }

    static BufferedImage souls(BufferedImage warden, BufferedImage heart) {
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 16; ++y) {
            for (int x = 0; x < 16; ++x) {
                int col = CROP_COL + x, v = CROP_V + y;
                image.setRGB(x, y, rib(warden, col, v) >>> 24 != 0 ? 0 : heart.getRGB(CHEST_U + col, v));
            }
        }
        return image;
    }

    /** The warden's skin: flesh, a speck in about one pixel of 25, and a teal spot or two per face. */
    static BufferedImage skin(long seed) {
        Random random = new Random(seed);
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 16; ++y) {
            for (int x = 0; x < 16; ++x) {
                int roll = random.nextInt(100);
                image.setRGB(x, y, roll < 3 ? SPECK : roll < 4 ? DARK_SPECK : roll < 5 ? TEAL_SPECK : FLESH);
            }
        }
        // A slab shows the lower half of its side texture, so a spot in each half.
        image.setRGB(3 + random.nextInt(4), 1 + random.nextInt(5), SPOT);
        image.setRGB(9 + random.nextInt(4), 9 + random.nextInt(5), SPOT);
        return image;
    }

    static int blend(int base, int over, float alpha) {
        if (alpha <= 0.0f) {
            return base | 0xFF000000;
        }
        int r = Math.round((base >> 16 & 255) * (1 - alpha) + (over >> 16 & 255) * alpha);
        int g = Math.round((base >> 8 & 255) * (1 - alpha) + (over >> 8 & 255) * alpha);
        int b = Math.round((base & 255) * (1 - alpha) + (over & 255) * alpha);
        return 0xFF000000 | r << 16 | g << 8 | b;
    }
}
