package cz.vity.freerapid.plugins.services.turbobit.captcha;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.util.List;
import java.util.logging.Logger;

/**
 * Utility for creating captcha sample files
 *
 * @author ntoskrnl
 */
public class SamplesCreator {

    private final static String CAPTCHA_URL = "http://www.turbobit.net/captcha/securimg_1/";
    private final static String OUTPUT_PATH = "E:\\projects\\captchatest\\";
    private final static int LETTERS_IN_CAPTCHA = 4;
    private final static int TIMES_TO_LOOP = 200;

    private final static int AMOUNT_OF_CAPTCHA_ENTRIES = TIMES_TO_LOOP * LETTERS_IN_CAPTCHA;
    private final static Logger logger = Logger.getLogger(SamplesCreator.class.getName());

    private void start() {
        try {
            //prepare the output image
            final BufferedImage output = new BufferedImage(AMOUNT_OF_CAPTCHA_ENTRIES * Sample.IMAGEWIDTH, Sample.IMAGEHEIGHT, BufferedImage.TYPE_BYTE_BINARY);
            final Graphics g = output.getGraphics();
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, AMOUNT_OF_CAPTCHA_ENTRIES * Sample.IMAGEWIDTH, Sample.IMAGEHEIGHT);

            final StringBuilder sb = new StringBuilder(AMOUNT_OF_CAPTCHA_ENTRIES);

            //grab the captcha, prepare it and add it to the output, and repeat
            for (int i = 0; i < TIMES_TO_LOOP; i++) {
                final BufferedImage img = CaptchaPreparer.getPreparedImage(ImageIO.read(new URL(CAPTCHA_URL)));
                final List<BufferedImage> split = ImageFunctions.split(img, CaptchaReader.SPLIT);
                if (split.size() != LETTERS_IN_CAPTCHA) {
                    logger.info("#" + i + " split failed");
                    i--;
                    continue;
                }

                //ask the user for input
                final String userInput = JOptionPane.showInputDialog(null, new JLabel(new ImageIcon(img)), "Insert what you see", JOptionPane.PLAIN_MESSAGE);
                if (userInput == null || userInput.length() != LETTERS_IN_CAPTCHA) {
                    logger.info("#" + i + " user canceled");
                    i--;
                    continue;
                }
                sb.append(userInput);

                for (int j = 0; j < LETTERS_IN_CAPTCHA; j++) {
                    final BufferedImage resized = ImageFunctions.resizeTo(ImageFunctions.rotateToMinWidth(split.get(j)), Sample.IMAGEWIDTH, Sample.IMAGEHEIGHT);
                    g.drawImage(resized, i * LETTERS_IN_CAPTCHA * Sample.IMAGEWIDTH + j * Sample.IMAGEWIDTH, 0, null);
                }
                logger.info("#" + i + " OK");
            }

            final String userInput = sb.toString();

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