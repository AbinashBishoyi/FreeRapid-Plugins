package cz.vity.freerapid.plugins.services.linkcrypt.captcha;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * @author ntoskrnl
 */
public class CaptchaPreparer {

    public static BufferedImage getPreparedImage(final InputStream input) throws IOException {
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
        final Graphics2D g = output.createGraphics();

        final int maxSegmentWidth = w / 10;
        for (int x0 = 0; x0 < w; x0 += maxSegmentWidth) {
            final int segmentWidth = Math.min(x0 + maxSegmentWidth, w) - x0;
            final List<Frame> list = new LinkedList<Frame>();
            for (final BufferedImage frame : frames) {
                int totalIntensity = 0;
                for (int y = 0; y < h; y++) {
                    for (int x = x0; x < x0 + segmentWidth; x++) {
                        totalIntensity += intensity(frame.getRGB(x, y));
                    }
                }
                list.add(new Frame(frame, totalIntensity / (segmentWidth * h)));
            }
            final BufferedImage frame = Collections.max(list).getImage();
            g.drawImage(frame, x0, 0, x0 + segmentWidth, h, x0, 0, x0 + segmentWidth, h, null);
        }

        g.dispose();

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                final int color = output.getRGB(x, y) & 0xFFFFFF;
                if (color == 0xFCFEFC || color == 0x040204 || color == 0) {
                    output.setRGB(x, y, 0xFFFFFF);
                }
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

    private static class Frame implements Comparable<Frame> {
        private final BufferedImage image;
        private final int intensity;

        public Frame(final BufferedImage image, final int intensity) {
            this.image = image;
            this.intensity = intensity;
        }

        @Override
        public int compareTo(final Frame that) {
            return Integer.valueOf(this.intensity).compareTo(that.intensity);
        }

        public BufferedImage getImage() {
            return image;
        }
    }

}
