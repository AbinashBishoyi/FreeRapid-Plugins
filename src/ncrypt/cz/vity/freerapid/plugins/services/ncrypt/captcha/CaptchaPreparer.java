package cz.vity.freerapid.plugins.services.ncrypt.captcha;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * @author ntoskrnl
 */
public class CaptchaPreparer {

    public static BufferedImage getPreparedCirclecaptchaImage(final BufferedImage image) {
        final int w = image.getWidth() - 2;
        final int h = image.getHeight() - 2;
        final BufferedImage input = image.getSubimage(1, 1, w, h);
        final BufferedImage output = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        final Graphics2D g = output.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, w, h);
        g.dispose();
        for (int y = 1; y < h - 1; y++) {
            for (int x = 1; x < w - 1; x++) {
                final int threshold = 160;
                if (intensity(input.getRGB(x, y)) < threshold
                        || intensity(input.getRGB(x, y - 1)) < threshold
                        || intensity(input.getRGB(x - 1, y)) < threshold
                        || intensity(input.getRGB(x, y + 1)) < threshold
                        || intensity(input.getRGB(x + 1, y)) < threshold) {
                    output.setRGB(x, y, 0);
                }
            }
        }
        return output;
    }

    public static BufferedImage getPreparedAnicaptchaImage(final InputStream input) throws IOException {
        if (input == null) {
            throw new IOException("InputStream cannot be null");
        }

        final ImageReader reader = ImageIO.getImageReadersByFormatName("gif").next();
        reader.setInput(ImageIO.createImageInputStream(input));

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

        final int w = frames.get(0).getWidth();
        final int h = frames.get(0).getHeight();

        final BufferedImage output = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int currentColor = 0xFFFFFF;
                for (final BufferedImage frame : frames) {
                    if (intensity(frame.getRGB(x, y)) < 128) {
                        currentColor = 0;
                        break;
                    }
                }
                output.setRGB(x, y, currentColor);
            }
        }

        return output;
    }

    private static int intensity(final int i) {
        final int r = (i >> 16) & 0xFF;
        final int g = (i >> 8) & 0xFF;
        final int b = (i) & 0xFF;
        return (r + g + b) / 3;
    }

}
