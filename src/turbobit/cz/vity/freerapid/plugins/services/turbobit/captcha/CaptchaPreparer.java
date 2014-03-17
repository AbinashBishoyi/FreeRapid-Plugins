package cz.vity.freerapid.plugins.services.turbobit.captcha;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;

/**
 * Provides method for preparing captcha images for recognition
 *
 * @author ntoskrnl
 */
public class CaptchaPreparer {
    private final static Logger logger = Logger.getLogger(CaptchaPreparer.class.getName());
    private final static String[] BACKGROUND_FILES = {"captcha-bg-01.png", "captcha-bg-02.png", "captcha-bg-03.png", "captcha-bg-04.png", "captcha-bg-05.png", "captcha-bg-06.png", "captcha-bg-07.png", "captcha-bg-08.png", "captcha-bg-09.png", "captcha-bg-10.png"};
    private final static List<BackgroundImage> backgrounds = new ArrayList<BackgroundImage>(BACKGROUND_FILES.length);

    /**
     * Prepares captcha image for recognition
     *
     * @param input Input image
     * @return Prepared image
     * @throws Exception If something goes wrong
     */
    public static BufferedImage getPreparedImage(final BufferedImage input) throws Exception {
        //final BufferedImage input = ImageIO.read(new FileInputStream("E:\\projects\\captchatest\\captcha.png"));
        final int w = input.getWidth();
        final int h = input.getHeight();

        //load sample background images
        if (backgrounds.size() == 0) {
            backgrounds.clear();
            for (final String backgroundFile : BACKGROUND_FILES) {
                //final BufferedImage bg = ImageIO.read(new FileInputStream("E:\\projects\\captchatest\\" + backgroundFile));
                final BufferedImage bg = ImageIO.read((new CaptchaPreparer()).getClass().getResourceAsStream("/resources/" + backgroundFile));
                backgrounds.add(new BackgroundImage(bg));
            }
        }

        //determine correct sample background by comparing amounts of differences to the captcha image
        for (final BackgroundImage bg : backgrounds) {
            bg.similarity = bg.differenceTo(input);
        }
        Collections.sort(backgrounds, new Comparator<BackgroundImage>() {
            public int compare(BackgroundImage o1, BackgroundImage o2) {
                return o2.similarity - o1.similarity;
            }
        });
        if (backgrounds.get(0).similarity < 50) {
            logger.warning("Possible problems determining correct sample background");
        }
        final BufferedImage background = backgrounds.get(0).image;

        //subtract the background from the the captcha image, and make the result black and white
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

    private static class BackgroundImage {
        public BufferedImage image;
        public int similarity;

        public BackgroundImage(final BufferedImage image) {
            this.image = image;
        }

        public int differenceTo(final BackgroundImage other) {
            return differenceTo(other.image);
        }

        public int differenceTo(final BufferedImage other) {
            int number = 0;
            int similarity = 0;
            for (int y = 0; y < this.image.getHeight(); y++) {
                for (int x = 0; x < this.image.getWidth(); x++) {
                    number++;
                    if (this.image.getRGB(x, y) == other.getRGB(x, y)) {
                        similarity++;
                    }
                }
            }
            return (int) (((double) similarity) / ((double) number) * 100);//percentage
        }
    }

    /*
    public static void main(String[] args) throws Exception {
        CaptchaPreparer.getPreparedImage();
    }
    */
}
