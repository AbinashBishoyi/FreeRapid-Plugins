package testcaptcha;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.PixelGrabber;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * @author Vity
 */
public class TestTrain {
    private Collection<Template> trainedSet = new ArrayList<Template>(10);

    private final short[] datat0 = {255, 255, 255, 0, 0, 255, 255, 255, 255, 255, 0, 0, 0, 0, 255, 255, 255, 0, 0, 255, 255, 0, 0, 255, 0, 0, 255, 255, 255, 255, 0, 0, 0, 0, 255, 255, 255, 255, 0, 0, 0, 0, 255, 255, 255, 255, 0, 0, 0, 0, 255, 255, 255, 255, 0, 0, 255, 0, 0, 255, 255, 0, 0, 255, 255, 255, 0, 0, 0, 0, 255, 255, 255, 255, 255, 0, 0, 255, 255, 255};
    private final short[] datat1 = {255, 255, 255, 0, 0, 255, 255, 255, 255, 255, 0, 0, 0, 255, 255, 255, 255, 0, 0, 0, 0, 255, 255, 255, 255, 255, 255, 0, 0, 255, 255, 255, 255, 255, 255, 0, 0, 255, 255, 255, 255, 255, 255, 0, 0, 255, 255, 255, 255, 255, 255, 0, 0, 255, 255, 255, 255, 255, 255, 0, 0, 255, 255, 255, 255, 255, 255, 0, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 255};
    private final short[] datat2 = {255, 255, 0, 0, 0, 0, 255, 255, 255, 0, 0, 255, 255, 0, 0, 255, 0, 0, 0, 255, 255, 0, 0, 0, 255, 255, 255, 255, 255, 0, 0, 0, 255, 255, 255, 255, 255, 0, 0, 255, 255, 255, 255, 255, 0, 0, 255, 255, 255, 255, 255, 0, 0, 255, 255, 255, 255, 255, 0, 0, 255, 255, 255, 255, 255, 0, 0, 255, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0};
    private final short[] datat3 = {255, 0, 0, 0, 0, 0, 255, 255, 0, 0, 255, 255, 255, 0, 0, 255, 255, 255, 255, 255, 255, 255, 0, 0, 255, 255, 255, 255, 255, 0, 0, 255, 255, 255, 255, 0, 0, 0, 255, 255, 255, 255, 255, 255, 255, 0, 0, 255, 255, 255, 255, 255, 255, 255, 0, 0, 255, 255, 255, 255, 255, 255, 0, 0, 0, 0, 255, 255, 255, 0, 0, 255, 255, 0, 0, 0, 0, 0, 255, 255};
    private final short[] datat4 = {255, 255, 255, 255, 255, 0, 0, 255, 255, 255, 255, 255, 0, 0, 0, 255, 255, 255, 255, 0, 0, 0, 0, 255, 255, 255, 0, 0, 255, 0, 0, 255, 255, 0, 0, 255, 255, 0, 0, 255, 0, 0, 255, 255, 255, 0, 0, 255, 0, 0, 0, 0, 0, 0, 0, 0, 255, 255, 255, 255, 255, 0, 0, 255, 255, 255, 255, 255, 255, 0, 0, 255, 255, 255, 255, 255, 255, 0, 0, 255};
    private final short[] datat5 = {0, 0, 0, 0, 0, 0, 0, 255, 0, 0, 255, 255, 255, 255, 255, 255, 0, 0, 255, 255, 255, 255, 255, 255, 0, 0, 255, 0, 0, 0, 255, 255, 0, 0, 0, 255, 255, 0, 0, 255, 255, 255, 255, 255, 255, 255, 0, 0, 255, 255, 255, 255, 255, 255, 0, 0, 0, 0, 255, 255, 255, 255, 0, 0, 255, 0, 0, 255, 255, 0, 0, 255, 255, 255, 0, 0, 0, 0, 255, 255};
    private final short[] datat6 = {255, 255, 0, 0, 0, 0, 255, 255, 255, 0, 0, 255, 255, 0, 0, 255, 0, 0, 255, 255, 255, 255, 0, 255, 0, 0, 255, 255, 255, 255, 255, 255, 0, 0, 255, 0, 0, 0, 255, 255, 0, 0, 0, 255, 255, 0, 0, 255, 0, 0, 255, 255, 255, 255, 0, 0, 0, 0, 255, 255, 255, 255, 0, 0, 255, 0, 0, 255, 255, 0, 0, 255, 255, 255, 0, 0, 0, 0, 255, 255};
    private final short[] datat7 = {0, 0, 0, 0, 0, 0, 0, 0, 255, 255, 255, 255, 255, 255, 0, 0, 255, 255, 255, 255, 255, 255, 0, 0, 255, 255, 255, 255, 255, 0, 0, 255, 255, 255, 255, 255, 0, 0, 255, 255, 255, 255, 255, 0, 0, 255, 255, 255, 255, 255, 0, 0, 255, 255, 255, 255, 255, 0, 0, 255, 255, 255, 255, 255, 0, 0, 255, 255, 255, 255, 255, 255, 0, 0, 255, 255, 255, 255, 255, 255};
    private final short[] datat8 = {255, 255, 0, 0, 0, 0, 255, 255, 255, 0, 0, 255, 255, 0, 0, 255, 0, 0, 255, 255, 255, 255, 0, 0, 255, 0, 0, 255, 255, 0, 0, 255, 255, 255, 0, 0, 0, 0, 255, 255, 255, 0, 0, 255, 255, 0, 0, 255, 0, 0, 255, 255, 255, 255, 0, 0, 0, 0, 255, 255, 255, 255, 0, 0, 255, 0, 0, 255, 255, 0, 0, 255, 255, 255, 0, 0, 0, 0, 255, 255};
    private final short[] datat9 = {255, 255, 0, 0, 0, 0, 255, 255, 255, 0, 0, 255, 255, 0, 0, 255, 0, 0, 255, 255, 255, 255, 0, 0, 0, 0, 255, 255, 255, 255, 0, 0, 255, 0, 0, 255, 255, 0, 0, 0, 255, 255, 0, 0, 0, 255, 0, 0, 255, 255, 255, 255, 255, 255, 0, 0, 0, 0, 255, 255, 255, 255, 0, 0, 255, 0, 0, 255, 255, 0, 0, 255, 255, 255, 0, 0, 0, 0, 255, 255};


    public static void main(String[] args) throws IOException {
        new TestTrain().start();
    }

    private void start() throws IOException {
//        final File dir = new File("C:\\develope\\freerapid-plugintools\\src\\bagruj\\captcha");
//        final File[] files = dir.listFiles();
//        for (File file : files) {
//            if (!file.getName().endsWith(".gif"))
//                continue;
//            final char c = Utils.getPureFilename(file).toCharArray()[1];
//            final BufferedImage image = ImageIO.read(file);
//            trainedSet.add(new Template(c, imageToData(image)));
//        }
        trainedSet.add(new Template('0', datat0));
        trainedSet.add(new Template('1', datat1));
        trainedSet.add(new Template('2', datat2));
        trainedSet.add(new Template('3', datat3));
        trainedSet.add(new Template('4', datat4));
        trainedSet.add(new Template('5', datat5));
        trainedSet.add(new Template('6', datat6));
        trainedSet.add(new Template('7', datat7));
        trainedSet.add(new Template('8', datat8));
        trainedSet.add(new Template('9', datat9));


        final File testFile = new File("d:\\testfile.jpg");
        final BufferedImage image = ImageIO.read(testFile);
        StringBuilder builder = new StringBuilder();

        for (int i = 0, x = 22; i < 4; i++) {
            final BufferedImage subimage = image.getSubimage(x, 8, 8, 10);
            builder.append(findResult(subimage).ch);
            x += 9;
        }

        System.out.println("Result = " + builder);
//        for (Template template : trainedSet) {
//            String s = Arrays.toString(template.data);
//            s = s.replace('[', '{');
//            s = s.replace(']', '}');
//            System.out.println(String.format("private final short[] data t%s = %s;", template.ch, s));
//        }
    }

    private Template findResult(BufferedImage image) {
        final Template test = new Template('X', imageToData(image));
        for (Template template : trainedSet) {
            template.countDistance(test);
            //System.out.println("template = " + template);
        }
        return Collections.min(trainedSet);
    }


    private short[] imageToData(BufferedImage image) {
        //  image = toBufferedImage(image);
        //   JOptionPane.showConfirmDialog(null, new JLabel(new ImageIcon(image)));
        final int w = image.getWidth();
        final int h = image.getHeight();

        short[] data = new short[w * h];
        int offset = 0;
        int red;
        final WritableRaster raster = image.getRaster();

        final boolean isAlpha = image.getColorModel().hasAlpha();
        int[] c = new int[raster.getNumBands()];
        for (int i = 0; i < h; i++) {
            for (int j = 0; j < w; j++) {
                red = new Color(image.getRGB(j, i), isAlpha).getRed();
                data[offset++] = (red > 75) ? 255 : (short) red;
            }
        }
        return data;
    }

    private class Template implements Comparable<Template> {
        private char ch;
        private short[] data;
        private long distance;

        private Template(char ch, short[] data) {
            this.ch = ch;
            this.data = data;
        }

        public void countDistance(Template otherTemplate) {
            final short[] otherData = otherTemplate.data;
            distance = 0;
            for (int i = 0; i < data.length; i++) {
                final int t = otherData[i] - data[i];
                distance += t * t;
            }
        }

        @Override
        public int compareTo(Template o) {
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

    // This method returns a buffered image with the contents of an image
    public static BufferedImage toBufferedImage(Image image) {

        // This code ensures that all the pixels in the image are loaded
        image = new ImageIcon(image).getImage();

        // Determine if the image has transparent pixels; for this method's
        // implementation, see e661 Determining If an Image Has Transparent Pixels
        boolean hasAlpha = hasAlpha(image);

        // Create a buffered image with a format that's compatible with the screen
        BufferedImage bimage = null;
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        try {
            // Determine the type of transparency of the new buffered image
            int transparency = Transparency.OPAQUE;
            if (hasAlpha) {
                transparency = Transparency.BITMASK;
            }

            // Create the buffered image
            GraphicsDevice gs = ge.getDefaultScreenDevice();
            GraphicsConfiguration gc = gs.getDefaultConfiguration();
            bimage = gc.createCompatibleImage(
                    image.getWidth(null), image.getHeight(null), transparency);
        } catch (HeadlessException e) {
            // The system does not have a screen
        }

        if (bimage == null) {
            // Create a buffered image using the default color model
            int type = BufferedImage.TYPE_INT_RGB;
            if (hasAlpha) {
                type = BufferedImage.TYPE_INT_ARGB;
            }
            bimage = new BufferedImage(image.getWidth(null), image.getHeight(null), type);
        }

        // Copy image to buffered image
        Graphics g = bimage.createGraphics();

        // Paint the image onto the buffered image
        g.drawImage(image, 0, 0, null);
        g.dispose();

        return bimage;
    }

    // This method returns true if the specified image has transparent pixels
    public static boolean hasAlpha(Image image) {
        // If buffered image, the color model is readily available
        if (image instanceof BufferedImage) {
            BufferedImage bimage = (BufferedImage) image;
            return bimage.getColorModel().hasAlpha();
        }

        // Use a pixel grabber to retrieve the image's color model;
        // grabbing a single pixel is usually sufficient
        PixelGrabber pg = new PixelGrabber(image, 0, 0, 1, 1, false);
        try {
            pg.grabPixels();
        } catch (InterruptedException e) {
        }

        // Get the image's color model
        ColorModel cm = pg.getColorModel();
        return cm.hasAlpha();
    }


}
