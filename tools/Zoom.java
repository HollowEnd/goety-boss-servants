import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

/** Dev helper: java Zoom.java in.png out.png x y w h scale  (crops and scales up with a checker background). */
public class Zoom {
    public static void main(String[] args) throws Exception {
        BufferedImage in = ImageIO.read(new File(args[0]));
        int x = Integer.parseInt(args[2]), y = Integer.parseInt(args[3]), w = Integer.parseInt(args[4]), h = Integer.parseInt(args[5]), s = Integer.parseInt(args[6]);
        BufferedImage out = new BufferedImage(w * s, h * s, BufferedImage.TYPE_INT_RGB);
        for (int j = 0; j < h * s; ++j) {
            for (int i = 0; i < w * s; ++i) {
                int px = x + i / s, py = y + j / s;
                int argb = px < in.getWidth() && py < in.getHeight() ? in.getRGB(px, py) : 0;
                int a = argb >>> 24;
                int bg = ((i / 8 + j / 8) % 2 == 0) ? 0xFF00FF : 0xCC00CC;
                int r = ((argb >> 16 & 255) * a + (bg >> 16 & 255) * (255 - a)) / 255;
                int g = ((argb >> 8 & 255) * a + (bg >> 8 & 255) * (255 - a)) / 255;
                int b = ((argb & 255) * a + (bg & 255) * (255 - a)) / 255;
                out.setRGB(i, j, (r << 16) | (g << 8) | b);
            }
        }
        ImageIO.write(out, "png", new File(args[1]));
    }
}
