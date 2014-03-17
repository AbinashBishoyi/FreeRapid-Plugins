package cz.vity.freerapid.plugins.services.linksave.captcha;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Provides method for preparing captcha images for recognition
 *
 * @author ntoskrnl
 */
public class CaptchaPreparer {
    private final static Logger logger = Logger.getLogger(CaptchaPreparer.class.getName());

    /**
     * Prepares captcha image for recognition
     *
     * @param input InputStream containing image to prepare
     * @return Prepared image
     * @throws Exception If something goes wrong
     */
    public static BufferedImage getPreparedImage(final InputStream input) throws Exception {
        if (input == null)
            throw new IOException("InputStream cannot be null");

        final GifDecoder decoder = new GifDecoder();
        int err;
        if ((err = decoder.read(input)) != GifDecoder.STATUS_OK)
            logger.warning("Error reading image from InputStream (" + err + ")");

        final int n = decoder.getFrameCount();
        final List<BufferedImage> frames = new ArrayList<BufferedImage>(n);
        for (int i = 0; i < n; i++) {
            frames.add(decoder.getFrame(i));
        }

        final int w = (int) decoder.getFrameSize().getWidth();
        final int h = (int) decoder.getFrameSize().getHeight();

        final BufferedImage output = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int color = 0;
                for (final BufferedImage frame : frames) {
                    int i = frame.getRGB(x, y);
                    if (luminosity(i) > luminosity(color)) color = i;
                }
                output.setRGB(x, y, color);
            }
        }

        return output;
    }

    private static double luminosity(int i) {
        final float[] rgb = new Color(i).getRGBColorComponents(null);
        return (rgb[0] + rgb[1] + rgb[2]) / 3d;
    }

}
