package cz.vity.freerapid.plugins.services.turbobit.captcha;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;

/**
 * Provides method for preparing captcha images for recognition
 *
 * @author ntoskrnl
 */
public class CaptchaPreparer {
    private final static Logger logger = Logger.getLogger(CaptchaPreparer.class.getName());
    private final static String[] BACKGROUND_FILES = {"captcha-bg-01.png", "captcha-bg-02.png", "captcha-bg-03.png", "captcha-bg-04.png", "captcha-bg-05.png", "captcha-bg-06.png", "captcha-bg-07.png", "captcha-bg-08.png", "captcha-bg-09.png", "captcha-bg-10.png"};
    private final static Set<BufferedImage> backgrounds = new HashSet<BufferedImage>(BACKGROUND_FILES.length);

    /**
     * Prepares captcha image for recognition
     *
     * @param input Input image
     * @return Prepared image
     * @throws Exception If something goes wrong
     */
    public static BufferedImage getPreparedImage(final BufferedImage input) throws Exception {
        synchronized (CaptchaPreparer.class) {
            //load sample background images
            if (backgrounds.size() == 0) {
                for (final String backgroundFile : BACKGROUND_FILES) {
                    //final BufferedImage bg = ImageIO.read(new FileInputStream("E:\\projects\\captchatest\\" + backgroundFile));
                    final BufferedImage bg = ImageIO.read(CaptchaPreparer.class.getResourceAsStream("/resources/" + backgroundFile));
                    backgrounds.add(bg);
                }
            }
        }

        final TreeSet<MyImage> set = new TreeSet<MyImage>();
        for (final BufferedImage bg : backgrounds) {
            set.add(new MyImage(bg, input));
        }
        if (set.first().similarity < 0.5) {
            logger.warning("Possible problems determining correct sample background");
        }
        final BufferedImage background = set.first().image;

        //subtract the background from the the captcha image, and make the result black and white
        final int w = input.getWidth();
        final int h = input.getHeight();
        final BufferedImage output = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_BINARY);
        for (int i = 0; i < h; i++) {
            for (int j = 0; j < w; j++) {
                final int color = input.getRGB(j, i) == background.getRGB(j, i) ? Color.WHITE.getRGB() : Color.BLACK.getRGB();
                output.setRGB(j, i, color);
            }
        }

        //JOptionPane.showConfirmDialog(null, new ImageIcon(output));

        return output;
    }

    private static class MyImage implements Comparable<MyImage> {
        public BufferedImage image;
        public double similarity;

        public MyImage(final BufferedImage image, final BufferedImage compareTo) {
            this.image = image;
            this.similarity = similarity(compareTo);
        }

        private double similarity(final BufferedImage compareTo) {
            int number = 0;
            int similarity = 0;
            for (int y = 0, n = Math.min(image.getHeight(), compareTo.getHeight()); y < n; y++) {
                for (int x = 0, m = Math.min(image.getWidth(), compareTo.getWidth()); x < m; x++) {
                    number++;
                    if (image.getRGB(x, y) == compareTo.getRGB(x, y)) {
                        similarity++;
                    }
                }
            }
            return ((double) similarity) / ((double) number);
        }

        @Override
        public int compareTo(MyImage that) {
            return Double.compare(that.similarity, this.similarity);
        }
    }

}
