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
        //final BufferedImage image = ImageIO.read(new File("E:\\projects\\captchatest\\letters\\letters.png"));
        final BufferedImage image = ImageIO.read(this.getClass().getResourceAsStream("/resources/letters.png"));
        int pos = 0;
        for (final char c : LETTERS) {
            final BufferedImage subimage = image.getSubimage(pos, 0, 20, 28);
            trainedSet.add(new Template(c, imageToData(subimage)));
            pos += 20;
        }
    }

    public String recognize(final BufferedImage image) {
        if (image.getType() != BufferedImage.TYPE_BYTE_BINARY)
            throw new IllegalArgumentException("Image must be of type TYPE_BYTE_BINARY");

        final StringBuilder builder = new StringBuilder(4);

        for (int i = 0, x = 9; i < 5; i++, x += 28) {
            final BufferedImage subimage = image.getSubimage(x, 0, 20, 28);
            builder.append(findResult(subimage).ch);
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

    private class Template implements Comparable<Template> {
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

}