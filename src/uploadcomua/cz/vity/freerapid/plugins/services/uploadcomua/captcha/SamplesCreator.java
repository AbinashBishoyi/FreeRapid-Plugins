package cz.vity.freerapid.plugins.services.uploadcomua.captcha;

import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.GetMethod;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;
import java.util.logging.Logger;

/**
 * Utility for creating captcha sample files
 *
 * @author ntoskrnl
 */
public class SamplesCreator {

    private final static String CAPTCHA_URL = "http://upload.com.ua/confirm.php";
    private final static String OUTPUT_PATH = "E:\\projects\\captchatest\\";
    private final static int LETTERS_IN_CAPTCHA = 4;
    private final static int TIMES_TO_LOOP = 200;

    private final static int AMOUNT_OF_CAPTCHA_ENTRIES = TIMES_TO_LOOP * LETTERS_IN_CAPTCHA;
    private final static Logger logger = Logger.getLogger(SamplesCreator.class.getName());

    private void start() {
        try {
            //prepare the HTTP client
            final HttpClient client = new HttpClient();
            client.getParams().setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);
            final GetMethod getCaptcha = new GetMethod(CAPTCHA_URL);

            //prepare the output image
            final BufferedImage output = new BufferedImage(AMOUNT_OF_CAPTCHA_ENTRIES * Sample.IMAGEWIDTH, Sample.IMAGEHEIGHT, BufferedImage.TYPE_BYTE_BINARY);
            final Graphics g = output.getGraphics();
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, AMOUNT_OF_CAPTCHA_ENTRIES * Sample.IMAGEWIDTH, Sample.IMAGEHEIGHT);

            final StringBuilder sb = new StringBuilder(AMOUNT_OF_CAPTCHA_ENTRIES);//used for cookie workaround

            //grab the captcha, prepare it and add it to the output, and repeat
            for (int i = 0; i < TIMES_TO_LOOP; i++) {
                client.executeMethod(getCaptcha);
                final InputStream is = getCaptcha.getResponseBodyAsStream();
                final BufferedImage img = CaptchaReader.getPreparedImage(is);
                final List<BufferedImage> split = ImageFunctions.split(img, CaptchaReader.SPLIT);
                if (split.size() != LETTERS_IN_CAPTCHA) {
                    logger.info("#" + i + " split failed");
                    i--;
                    continue;
                }
                for (int j = 0; j < LETTERS_IN_CAPTCHA; j++) {
                    final BufferedImage resized = ImageFunctions.resizeTo(ImageFunctions.rotateToMinWidth(split.get(j)), Sample.IMAGEWIDTH, Sample.IMAGEHEIGHT);
                    g.drawImage(resized, i * LETTERS_IN_CAPTCHA * Sample.IMAGEWIDTH + j * Sample.IMAGEWIDTH, 0, null);
                }
                //utilize the cookie workaround for much quicker recognition
                for (final Cookie cookie : client.getState().getCookies()) {
                    if (cookie.getName().equals("capcha")) {
                        sb.append(cookie.getValue());
                        break;
                    }
                }
                logger.info("#" + i + " OK");
            }

            /*
            //ask the user for input
            final String userInput = JOptionPane.showInputDialog(null, new JLabel(new ImageIcon(output)), "Insert what you see", JOptionPane.PLAIN_MESSAGE);
            if (userInput == null || userInput.isEmpty()) {
                logger.info("User canceled");
                return;
            }
            if (userInput.length() != AMOUNT_OF_CAPTCHA_ENTRIES) {
                logger.warning("User input length not same as amount of captcha entries");
            }
            logger.info("User input: " + userInput);
            */

            final String userInput = sb.toString();//used for cookie workaround

            //write the output files
            ImageIO.write(output, "png", new File(OUTPUT_PATH + "samples.png"));
            new FileOutputStream(OUTPUT_PATH + "samples.bin").write(userInput.getBytes());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new SamplesCreator().start();
    }

}
