package cz.vity.freerapid.plugins.services.turbobit.captcha;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;

/**
 * @author JPEXS
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
     * Recognize text from captcha image
     *
     * @param input Captcha image to recognize
     * @return String with text or null on error
     * @throws Exception
     */
    public static String recognize(final BufferedImage input) throws Exception {
        if (samples.size() == 0) {
            loadSamples();
        }
        logger.info("Recognizing...");
        final BufferedImage img = CaptchaPreparer.getPreparedImage(input);

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