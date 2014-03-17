package cz.vity.freerapid.plugins.services.badongo.captcha;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;

/**
 * @author JPEXS, ntoskrnl (getPreparedImage)
 */
public class CaptchaReader {
    private final static Logger logger = Logger.getLogger(CaptchaReader.class.getName());
    public static List<Sample> samples = new ArrayList<Sample>();
    public static int SPLIT = 2;//space between two letters

    /**
     * Loads samples from resources
     *
     * @throws IOException
     */
    private static void loadSamples() throws IOException {
        logger.info("Loading captcha samples...");
        //BufferedImage img = ImageIO.read(new File("E:\\projects\\captchatest\\samples.png"));
        //InputStream is = new FileInputStream("E:\\projects\\captchatest\\samples.bin");
        BufferedImage img = ImageIO.read((new CaptchaReader()).getClass().getResourceAsStream("/resources/samples.png"));
        InputStream is = (new CaptchaReader()).getClass().getResourceAsStream("/resources/samples.bin");
        int i = 0;
        samples.clear();
        int pos = 0;
        while ((i = is.read()) > -1) {
            samples.add(new Sample(img, i, pos, true));
            pos += Sample.IMAGEWIDTH;
        }
        is.close();
        logger.info("Loading captcha samples finished");
    }

    /**
     * Prepares captcha image for recognition
     *
     * @param originalImage Input image
     * @return Prepared image
     * @throws IOException
     */
    public static BufferedImage getPreparedImage(final BufferedImage originalImage) throws IOException {
        final int colorLimit = 165;

        //final BufferedImage originalImage = ImageIO.read(new FileInputStream("E:\\projects\\captchatest\\captcha.jpg"));
        final int w = originalImage.getWidth();
        final int h = originalImage.getHeight();

        //make the image greyscale
        final BufferedImage greyScale = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);
        greyScale.getGraphics().drawImage(originalImage, 0, 0, Color.WHITE, null);

        //make it black and white according to colorLimit
        final BufferedImage blackAndWhite = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_BINARY);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                final int red = new Color(greyScale.getRGB(x, y)).getRed();
                final int color = red > colorLimit ? Color.WHITE.getRGB() : Color.BLACK.getRGB();
                blackAndWhite.setRGB(x, y, color);
            }
        }

        //remove any possible small dots that are left
        final BufferedImage newImage = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_BINARY);
        newImage.getGraphics().drawImage(blackAndWhite, 0, 0, Color.WHITE, null);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (blackAndWhite.getRGB(x, y) == Color.BLACK.getRGB()) {
                    if (ImageFunctions.getBlock(blackAndWhite, x, y).size() < 5) {
                        newImage.setRGB(x, y, Color.WHITE.getRGB());
                    }
                }
            }
        }

        /*JOptionPane.showConfirmDialog(null, new ImageIcon(originalImage));
        JOptionPane.showConfirmDialog(null, new ImageIcon(greyScale));
        JOptionPane.showConfirmDialog(null, new ImageIcon(blackAndWhite));
        JOptionPane.showConfirmDialog(null, new ImageIcon(newImage));*/

        return newImage;
    }

    /**
     * Recognize text from captcha image
     *
     * @param input Captcha image to recognize
     * @return String witch text or null on error
     * @throws Exception
     */
    public static String recognize(final BufferedImage input) throws Exception {
        if (samples.size() == 0) {
            loadSamples();
        }
        logger.info("Recognizing...");
        final BufferedImage img = getPreparedImage(input);

        List<BufferedImage> tested = ImageFunctions.split(img, SPLIT);
        if (tested.size() != 4) { //cannot split captcha
            return null;
        }

        String ret = "";
        for (int i = 0; i < tested.size(); i++) {
            Sample testovany = new Sample(tested.get(i), -1, 0);
            for (Sample v : samples) {
                v.distance = v.distanceTo(testovany);
            }
            Collections.sort(samples, new Comparator<Sample>() {
                public int compare(Sample o1, Sample o2) {
                    return (int) (o2.distance - o1.distance);
                }
            });
            ret += "" + samples.get(0).letter;
        }
        return ret;
    }

}