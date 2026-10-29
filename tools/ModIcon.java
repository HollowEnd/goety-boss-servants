import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Draws the mod's icon as 32x32 pixel art, scaled up without smoothing: a crowned skull (a boss,
 * raised by Goety's necromancy, soul fire in its eyes) chained in a cross behind it (bound to
 * serve), on a dark disc.
 *
 * Usage: java ModIcon.java <out.png> [scale]
 */
public class ModIcon {
    static final int SIZE = 32;
    static final int OUTLINE = 0xFF120C10;
    static final int DISC_IN = 0xFF2A1838, DISC_OUT = 0xFF140A1C, RIM = 0xFF4E2F66, RIM_LIGHT = 0xFF7A4FA0;
    static final int BONE_LIGHT = 0xFFF2EAD3, BONE = 0xFFD2C6A6, BONE_SHADE = 0xFFA39778, BONE_DARK = 0xFF6F654E;
    static final int SOCKET = 0xFF0A0A12, SOUL_HOT = 0xFFE8FFFF, SOUL = 0xFF5FF4FF, SOUL_DIM = 0xFF1A9FB4;
    static final int GOLD_LIGHT = 0xFFFFE27A, GOLD = 0xFFF0B428, GOLD_SHADE = 0xFFB07812, GOLD_DARK = 0xFF6E4708;
    static final int RUBY = 0xFFE0283F, RUBY_LIGHT = 0xFFFF8090, AMETHYST = 0xFFA548F0, AMETHYST_LIGHT = 0xFFD7A6FF;
    static final int IRON_LIGHT = 0xFFC8C8CC, IRON = 0xFF8C8C94, IRON_DARK = 0xFF4C4C56;

    static final int[][] px = new int[SIZE][SIZE];

    public static void main(String[] args) throws Exception {
        disc();
        hangingChain(4);
        hangingChain(25);
        skull();
        crown();
        int scale = args.length > 1 ? Integer.parseInt(args[1]) : 4;
        BufferedImage out = new BufferedImage(SIZE * scale, SIZE * scale, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < SIZE * scale; ++y) {
            for (int x = 0; x < SIZE * scale; ++x) {
                out.setRGB(x, y, px[y / scale][x / scale]);
            }
        }
        ImageIO.write(out, "png", new File(args[0]));
    }

    static void set(int x, int y, int c) {
        if (x >= 0 && y >= 0 && x < SIZE && y < SIZE) {
            px[y][x] = c;
        }
    }

    static int get(int x, int y) {
        return x >= 0 && y >= 0 && x < SIZE && y < SIZE ? px[y][x] : 0;
    }

    // A dark disc with a lit rim (top-left) so the icon reads on light and dark backgrounds.
    static void disc() {
        double c = 15.5;
        for (int y = 0; y < SIZE; ++y) {
            for (int x = 0; x < SIZE; ++x) {
                double d = Math.hypot(x - c, y - c);
                if (d <= 15.6) {
                    double t = Math.min(1.0, d / 15.0);
                    int color = lerp(DISC_IN, DISC_OUT, t * t);
                    if (d > 14.4) {
                        color = (x - c) + (y - c) < -6 ? RIM_LIGHT : RIM;
                    }
                    set(x, y, color);
                }
            }
        }
    }

    // A chain hanging the height of the disc beside the skull, in the vanilla chain's own pattern
    // (its 3-pixel links, face-on and edge-on), in lighter iron so it reads on the dark disc.
    static void hangingChain(int x) {
        String[] links = {"0.0", "010", ".2.", "102", "2.0", "0.0", "010", ".2.", ".0.", "202", "2.0"};
        for (int y = 2, i = 0; y <= 29; ++y, ++i) {
            String row = links[i % links.length];
            for (int k = 0; k < 3; ++k) {
                char c = row.charAt(k);
                if (c != '.' && Math.hypot(x + k - 15.5, y - 15.5) < 14.4) {
                    set(x + k, y, c == '1' ? IRON_LIGHT : c == '2' ? IRON : IRON_DARK);
                }
            }
        }
    }

    // A heavy chain hung from rim to rim, sagging under the jaw: face-on rings with a hole between
    // edge-on links, outlined so they read against the disc.
    static void drapedChain() {
        int[][] ring = {{0, 2, 2, 2, 0}, {2, 1, 1, 1, 2}, {2, 1, 0, 1, 2}, {2, 1, 1, 1, 2}, {0, 2, 2, 2, 0}};
        int[][] bar = {{2, 2, 2, 2}, {1, 1, 1, 1}, {2, 2, 2, 2}};
        boolean face = true;
        for (int x = 1; x <= 27; x += 4) {
            double t = (x + 2 - 15.5) / 14.0;
            int y = (int) Math.round(18 + 9 * (1 - t * t)) - 2;
            int[][] link = face ? ring : bar;
            for (int j = 0; j < link.length; ++j) {
                for (int k = 0; k < link[j].length; ++k) {
                    int px = x + k, py = y + j + (face ? 0 : 1);
                    if (link[j][k] == 2) {
                        set(px, py, OUTLINE);
                    } else if (link[j][k] == 1) {
                        set(px, py, face ? (k + j <= 3 ? IRON_LIGHT : k + j <= 4 ? IRON : IRON_DARK) : IRON_LIGHT);
                    }
                }
            }
            face = !face;
        }
    }

    static boolean inSkull(int x, int y) {
        // Cranium: an ellipse; cheeks and jaw: a rounded box below it.
        double cx = 15.5, cy = 15.0;
        boolean cranium = Math.pow((x - cx) / 7.6, 2) + Math.pow((y - cy) / 7.2, 2) <= 1.0;
        boolean jaw = x >= 11 && x <= 20 && y >= 19 && y <= 26 && !((x == 11 || x == 20) && y == 26);
        boolean cheek = x >= 9 && x <= 22 && y >= 18 && y <= 21;
        return cranium || jaw || cheek;
    }

    static void skull() {
        for (int y = 0; y < SIZE; ++y) {
            for (int x = 0; x < SIZE; ++x) {
                if (!inSkull(x, y)) {
                    continue;
                }
                boolean edge = !inSkull(x - 1, y) || !inSkull(x + 1, y) || !inSkull(x, y - 1) || !inSkull(x, y + 1);
                if (edge) {
                    set(x, y, OUTLINE);
                    continue;
                }
                // Lit from the upper left.
                double light = (x - 15.5) * 0.8 + (y - 14.0);
                set(x, y, light < -6 ? BONE_LIGHT : light < 2 ? BONE : light < 8 ? BONE_SHADE : BONE_DARK);
            }
        }
        // Temples and cheekbones, shaded in.
        for (int y = 19; y <= 21; ++y) {
            set(10, y, BONE_SHADE);
            set(21, y, BONE_DARK);
        }
        // The eye sockets, each with a soul flame rising out of it.
        eye(12, 15);
        eye(18, 15);
        // The nose: an upside-down heart of shadow.
        set(15, 20, SOCKET);
        set(16, 20, SOCKET);
        set(15, 21, SOCKET);
        set(16, 21, OUTLINE);
        // The teeth: a row of bone with dark gaps, and the line of the jaw under them.
        for (int x = 12; x <= 19; ++x) {
            set(x, 23, x % 2 == 0 ? OUTLINE : BONE_LIGHT);
            set(x, 24, x % 2 == 0 ? OUTLINE : BONE);
        }
        for (int x = 12; x <= 19; ++x) {
            set(x, 22, BONE_SHADE);
        }
    }

    static void eye(int x, int y) {
        int[][] socket = {{0, 1, 1, 0}, {1, 1, 1, 1}, {1, 1, 1, 1}, {0, 1, 1, 0}};
        for (int j = 0; j < 4; ++j) {
            for (int k = 0; k < 4; ++k) {
                if (socket[j][k] == 1) {
                    set(x + k - 1, y + j, SOCKET);
                }
            }
        }
        // The flame: white-hot at the core, licking up out of the socket.
        set(x, y + 2, SOUL_HOT);
        set(x + 1, y + 2, SOUL);
        set(x, y + 1, SOUL);
        set(x + 1, y + 1, SOUL_DIM);
        set(x, y, SOUL_DIM);
        set(x + 1, y - 1, SOUL_DIM);
    }

    static void crown() {
        // The band, sitting low on the cranium.
        for (int x = 9; x <= 22; ++x) {
            set(x, 7, OUTLINE);
            set(x, 8, x < 13 ? GOLD_LIGHT : x < 19 ? GOLD : GOLD_SHADE);
            set(x, 9, x < 13 ? GOLD : x < 19 ? GOLD_SHADE : GOLD_DARK);
            set(x, 10, OUTLINE);
        }
        set(8, 8, OUTLINE);
        set(8, 9, OUTLINE);
        set(23, 8, OUTLINE);
        set(23, 9, OUTLINE);
        // Three points, each tipped with a gold orb: tall in the middle.
        point(10.5, 3);
        point(20.5, 3);
        point(15.5, 1);
        // Gems on the band: a ruby in the middle, amethysts either side.
        set(15, 8, RUBY_LIGHT);
        set(16, 8, RUBY);
        set(15, 9, RUBY);
        set(16, 9, RUBY);
        set(11, 8, AMETHYST_LIGHT);
        set(11, 9, AMETHYST);
        set(20, 8, AMETHYST_LIGHT);
        set(20, 9, AMETHYST);
    }

    // A crown point centered on x (a half-pixel x makes it two pixels wide), its orb at top.
    static void point(double x, int top) {
        int left = (int) Math.floor(x), right = (int) Math.ceil(x);
        if (left == right) {
            right = left;
        }
        // The orb.
        set(left, top, GOLD_LIGHT);
        set(right, top, GOLD);
        set(left - 1, top, OUTLINE);
        set(right + 1, top, OUTLINE);
        set(left, top - 1, OUTLINE);
        set(right, top - 1, OUTLINE);
        // The spike, widening down to the band.
        for (int y = top + 1; y <= 6; ++y) {
            int half = (y - top - 1) / 2;
            for (int i = left - half - 1; i <= right + half + 1; ++i) {
                boolean edge = i == left - half - 1 || i == right + half + 1;
                set(i, y, edge ? OUTLINE : i <= left ? GOLD_LIGHT : GOLD);
            }
        }
    }

    static int lerp(int a, int b, double t) {
        int r = (int) Math.round((a >> 16 & 255) * (1 - t) + (b >> 16 & 255) * t);
        int g = (int) Math.round((a >> 8 & 255) * (1 - t) + (b >> 8 & 255) * t);
        int bl = (int) Math.round((a & 255) * (1 - t) + (b & 255) * t);
        return 0xFF000000 | r << 16 | g << 8 | bl;
    }
}
