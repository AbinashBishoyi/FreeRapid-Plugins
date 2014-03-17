package cz.vity.freerapid.plugins.services.hellshare;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author Vity, ntoskrnl
 */
final class CaptchaRecognizer {
    private final static char[] LETTERS = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z'};
    private final Collection<Template> trainedSet = new ArrayList<Template>(LETTERS.length);

    public CaptchaRecognizer() throws IOException {
        //final BufferedImage image = ImageIO.read(new File("E:\\projects\\captchatest\\letters.png"));
        final BufferedImage image = ImageIO.read(this.getClass().getResourceAsStream("/resources/letters.png"));
        final List<BufferedImage> subimages = ImageFunctions.split(image, 1);

        if (subimages.size() != LETTERS.length)
            throw new IOException("Sample image size incorrect");

        int i = 0;
        for (final char c : LETTERS) {
            final BufferedImage t = prepareSubimage(subimages.get(i++));
            trainedSet.add(new Template(c, imageToData(t)));
        }
    }

    public String recognize(final BufferedImage image) {
        if (image.getType() != BufferedImage.TYPE_BYTE_BINARY)
            throw new IllegalArgumentException("Image must be of type TYPE_BYTE_BINARY");

        final List<BufferedImage> subimages = ImageFunctions.split(image, 1);
        final StringBuilder builder = new StringBuilder(5);

        for (int i = 0; i < 5; i++) {
            final BufferedImage subimage = prepareSubimage(subimages.get(i));
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

    private BufferedImage prepareSubimage(final BufferedImage image) {
        final int w = 40;
        final int h = 40;

        final BufferedImage output = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_BINARY);
        final Graphics g = output.getGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, w, h);
        g.drawImage(image, 0, 0, Color.WHITE, null);

        return output;
    }

}