package cz.vity.freerapid.plugins.services.mega1280;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * @author Vity, ntoskrnl
 */
final class CaptchaRecognizer {
    private final static char[] LETTERS = {'2', '3', '4', '5', '6', '7', '8', '9', 'b', 'c', 'd', 'f', 'g', 'h', 'j', 'k', 'm', 'n', 'p', 'q', 'r', 's', 't', 'v', 'w', 'x', 'y', 'z'};
    private final Collection<Template> trainedSet = new ArrayList<Template>(LETTERS.length);

    public CaptchaRecognizer() throws IOException {
        //final BufferedImage image = ImageIO.read(new File("E:\\projects\\captchatest\\letters.png"));
        final BufferedImage image = ImageIO.read(this.getClass().getResourceAsStream("/resources/letters.png"));
        int pos = 0;
        for (final char c : LETTERS) {
            final BufferedImage subimage = prepareSubimage(image.getSubimage(pos, 0, 20, 30));
            trainedSet.add(new Template(c, imageToData(subimage)));
            pos += 20;
        }
    }

    public String recognize(final BufferedImage image) {
        if (image.getType() != BufferedImage.TYPE_BYTE_BINARY)
            throw new IllegalArgumentException("Image must be of type TYPE_BYTE_BINARY");

        final StringBuilder builder = new StringBuilder(5);

        for (int i = 0, x = 9; i < 5; i++, x += 28) {
            final BufferedImage subimage = prepareSubimage(image.getSubimage(x, 0, 20, 30));
            builder.append(findResult(subimage).getCh());
        }

        return builder.toString();
    }

    private Template findResult(final BufferedImage image) {
        final Template test = new Template('-', imageToData(image));
        for (final Template template : trainedSet) {
            template.countDistance(test);
        }
        return Collections.min(trainedSet);
    }

    private static class Template implements Comparable<Template> {
        private char ch;
        private short[] data;
        private long distance;

        private Template(char ch, short[] data) {
            this.ch = ch;
            this.data = data;
        }

        public void countDistance(final Template otherTemplate) {
            final short[] otherData = otherTemplate.data;
            distance = 0;
            for (int i = 0; i < data.length; i++) {
                final int t = otherData[i] - data[i];
                distance += t * t;
            }
        }

        @Override
        public int compareTo(final Template o) {
            return new Long(distance).compareTo(o.distance);
        }

        public char getCh() {
            return ch;
        }

        @Override
        public String toString() {
            return "'" + String.valueOf(ch) + "' - " + distance;
        }
    }

    private short[] imageToData(final BufferedImage image) {
        final int w = image.getWidth();
        final int h = image.getHeight();

        short[] data = new short[w * h];
        int offset = 0;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                data[offset++] = image.getRGB(x, y) == Color.WHITE.getRGB() ? (short) 255 : (short) 0;
            }
        }

        return data;
    }

    private BufferedImage prepareSubimage(final BufferedImage image) {
        final int fullW = image.getWidth();
        final int fullH = image.getHeight();

        final BufferedImage input = crop(image);

        final int cropW = input.getWidth();
        final int cropH = input.getHeight();

        final BufferedImage output = new BufferedImage(fullW, fullH, BufferedImage.TYPE_BYTE_BINARY);
        final Graphics g = output.getGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, fullW, fullH);
        g.drawImage(input, fullW / 2 - cropW / 2, fullH / 2 - cropH / 2, Color.WHITE, null);//put it in the center

        return output;
    }

    /**
     * Crop image so there are no white space on the left/right/up/down.
     *
     * @param img Image to crop
     * @return new cropped image
     */
    //author JPEXS
    private BufferedImage crop(final BufferedImage img) {
        int bottom = 0;
        int top = img.getHeight() - 1;
        int right = 0;
        int left = img.getWidth() - 1;
        boolean set = false;
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                if (img.getRGB(x, y) == Color.black.getRGB()) {
                    if (x < left) {
                        left = x;
                    }
                    set = true;
                    break;
                }
            }

            for (int x = img.getWidth() - 1; x >= 0; x--) {
                if (img.getRGB(x, y) == Color.black.getRGB()) {
                    if (x > right) {
                        right = x;
                    }
                    set = true;
                    break;
                }
            }
        }

        for (int x = 0; x < img.getWidth(); x++) {

            for (int y = 0; y < img.getHeight(); y++) {
                if (img.getRGB(x, y) == Color.black.getRGB()) {
                    if (y < top) {
                        top = y;
                    }
                    set = true;
                    break;
                }
            }

            for (int y = img.getHeight() - 1; y >= 0; y--) {
                if (img.getRGB(x, y) == Color.black.getRGB()) {
                    if (y > bottom) {
                        bottom = y;
                    }
                    set = true;
                    break;
                }
            }
        }
        if (!set) {
            return img;
        }
        int width = (right - left) + 1;
        int height = (bottom - top) + 1;
        BufferedImage ret = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        ret.getGraphics().drawImage(img, -left, -top, null);
        return ret;
    }

}