
package cz.vity.freerapid.plugins.services.netloadin.captcha;

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
 *
 * @author JPEXS
 */
public class CaptchaReader {
    private final static Logger logger = Logger.getLogger(CaptchaReader.class.getName());
    public static List<Sample> samples = new ArrayList<Sample>();
    /** Space between two letters */
    public static int SPLIT=2;

    /**
     * Loads samples from resources
     * @throws IOException
     */
    private static void loadSamples() throws IOException {
        logger.info("Loading captcha samples...");
        BufferedImage img=ImageIO.read((new CaptchaReader()).getClass().getResourceAsStream("/resources/samples.png"));
        InputStream is=(new CaptchaReader()).getClass().getResourceAsStream("/resources/samples.bin");
        int i=0;
        samples.clear();
        int pos=0;
        while((i=is.read())>-1){
            samples.add(new Sample(img,i,pos,true));
            pos+=Sample.IMAGEWIDTH;
        }
        is.close();
        logger.info("Loading captcha samples finished");
    }   

    /**
     * Recognize text from PNG image read from stream
     * @param is Stream to read image from
     * @return String witch text or null on error
     * @throws Exception
     */
    public static String recognize(InputStream is) throws Exception {
        if(samples.size()==0){
            loadSamples();
        }
        logger.info("Recognizing...");
        BufferedImage img=ImageIO.read(CaptchaImageConvert.convert(is));


        List<BufferedImage> tested = ImageFunctions.split(img, SPLIT);
        if(tested.size()!=4){ //cannot split captcha
            return null;
        }
        String ret = "";
        for (BufferedImage aTested : tested) {
            Sample testovany = new Sample(aTested, -1, 0);
            for (Sample v : samples) {
                v.distance = v.distanceTo(testovany);
            }
            Collections.sort(samples, new Comparator<Sample>() {

                public int compare(Sample o1, Sample o2) {
                    return (int) (o2.distance - o1.distance);
                }
            });
            ret += "" + samples.get(0).cislo;
        }
        return ret;
    }
}
