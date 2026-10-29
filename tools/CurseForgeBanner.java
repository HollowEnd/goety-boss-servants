import javax.imageio.ImageIO;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Lays the title logo (from CurseForgeLogo) over an in-game shot for the CurseForge page header, the
 * way Marium's Soulslike Weaponry does: the shot fills the banner, darkened toward the edges and
 * behind the logo so it reads, and the logo sits centred in its upper part.
 *
 * Usage: java CurseForgeBanner.java <shot.png> <logo.png> <out.png> [width height logoWidthFraction logoCentreY shotAlignY]
 *   (defaults 1920 1080 0.6 0.27 0.5; logoCentreY is a fraction of the height; shotAlignY picks which part of
 *   the shot a wider banner keeps, 0 its top to 1 its bottom)
 */
public class CurseForgeBanner {
    public static void main(String[] args) throws Exception {
        BufferedImage shot = ImageIO.read(new File(args[0]));
        BufferedImage logo = ImageIO.read(new File(args[1]));
        int w = args.length > 3 ? Integer.parseInt(args[3]) : 1920, h = args.length > 4 ? Integer.parseInt(args[4]) : 1080;
        double logoWidth = args.length > 5 ? Double.parseDouble(args[5]) : 0.6, logoY = args.length > 6 ? Double.parseDouble(args[6]) : 0.27;
        double alignY = args.length > 7 ? Double.parseDouble(args[7]) : 0.5;

        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        // The shot, covering the banner (cropped to fit: centred across, at alignY down).
        double cover = Math.max((double) w / shot.getWidth(), (double) h / shot.getHeight());
        int sw = (int) Math.ceil(shot.getWidth() * cover), sh = (int) Math.ceil(shot.getHeight() * cover);
        g.drawImage(shot, (w - sw) / 2, (int) Math.round((h - sh) * alignY), sw, sh, null);

        // A vignette, and a soft dark pool behind where the logo goes.
        int lw = (int) (w * logoWidth), lh = (int) Math.round((double) logo.getHeight() * lw / logo.getWidth());
        int lx = (w - lw) / 2, ly = (int) (h * logoY) - lh / 2;
        for (int y = 0; y < h; ++y) {
            for (int x = 0; x < w; ++x) {
                double vx = (x - w / 2.0) / (w / 2.0), vy = (y - h / 2.0) / (h / 2.0);
                double vignette = Math.min(1.0, Math.max(0.0, (vx * vx * 0.55 + vy * vy * 0.8) - 0.25)) * 0.55;
                double px = (x - (lx + lw / 2.0)) / (lw * 0.62), py = (y - (ly + lh / 2.0)) / (lh * 0.85);
                double pool = Math.max(0.0, 1.0 - (px * px + py * py)) * 0.42;
                double dark = 1.0 - (1.0 - vignette) * (1.0 - pool);
                int rgb = out.getRGB(x, y);
                int r = (int) ((rgb >> 16 & 255) * (1.0 - dark)), gr = (int) ((rgb >> 8 & 255) * (1.0 - dark)), b = (int) ((rgb & 255) * (1.0 - dark));
                out.setRGB(x, y, r << 16 | gr << 8 | b);
            }
        }

        g.setComposite(AlphaComposite.SrcOver);
        g.drawImage(logo, lx, ly, lw, lh, null);
        g.dispose();
        ImageIO.write(out, "png", new File(args[2]));
    }
}
