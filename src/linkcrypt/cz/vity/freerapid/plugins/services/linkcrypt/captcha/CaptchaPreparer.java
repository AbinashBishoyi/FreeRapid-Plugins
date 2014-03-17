package cz.vity.freerapid.plugins.services.linkcrypt.captcha;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Provides a method for preparing captcha images for recognition.
 *
 * @author ntoskrnl
 */
public class CaptchaPreparer {

    /**
     * Prepares a captcha image for recognition.
     *
     * @param input InputStream containing an image to prepare
     * @return Prepared image
     * @throws IOException If something goes wrong
     */
    public static BufferedImage getPreparedImage(final InputStream input) throws IOException {
        if (input == null) {
            throw new IOException("InputStream cannot be null");
        }

        final ImageReader reader = ImageIO.getImageReadersByFormatName("gif").next();
        reader.setInput(ImageIO.createImageInputStream(input));

        final int w = reader.getWidth(0);
        final int h = reader.getHeight(0);

        final List<BufferedImage> frames = new ArrayList<BufferedImage>();
        try {
            for (int i = 0; ; i++) {
                frames.add(reader.read(i));
            }
        } catch (final IndexOutOfBoundsException e) {
            // ImageReader javadoc recommends doing this,
            // see ImageReader#getNumImages(boolean)
        }
        if (frames.isEmpty()) {
            throw new IOException("No frames in GIF image");
        }

        final BufferedImage output = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int currentColor = 0;
                for (final BufferedImage frame : frames) {
                    final int i = frame.getRGB(x, y);
                    if (brightness(i) > brightness(currentColor)) {
                        currentColor = i;
                    }
                }
                output.setRGB(x, y, currentColor);
            }
        }

        return output;
    }

    private static int brightness(final int i) {
        final int r = (i >> 16) & 0xff;
        final int g = (i >> 8) & 0xff;
        final int b = (i) & 0xff;
        return (r + g + b) / 3;
    }

}