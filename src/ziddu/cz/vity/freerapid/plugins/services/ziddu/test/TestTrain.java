package cz.vity.freerapid.plugins.services.ziddu.test;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.*;
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


        final File testFile = new File("d:\\testfile.jpg");
        BufferedImage image = ImageIO.read(testFile);
        StringBuilder builder = new StringBuilder();

        ImageFilter filter = new RGBGrayFilter(88);
        ImageProducer producer = new FilteredImageSource(image.getSource(), filter);

        Image imge = Toolkit.getDefaultToolkit().createImage(producer);

        //   image = toBufferedImage(imge).getSubimage(12, 8, 77, 29);
        JOptionPane.showConfirmDialog(null, new JLabel(new ImageIcon(imge)));

//        final Object o = JOptionPane.showInputDialog(null, "Insert what you see", "Captcha", JOptionPane.QUESTION_MESSAGE, new ImageIcon(imge), null, null);

        //   testFile.renameTo(new File(testFile.getParent(), o.toString() + ".jpg"));

//
//
//        System.out.println("Result = " + builder);

//        for (Template template : trainedSet) {
//            String s = Arrays.toString(template.data);
//            s = s.replace('[', '{');
//            s = s.replace(']', '}');
//            System.out.println(String.format("private final short[] data t%s = %s;", template.ch, s));
//        }

//        final BufferedImage[] images = separate(image);
//        int counter = 0;
//        for (BufferedImage bufferedImage : images) {
//            final File output = new File(testFile.getParent(), o.toString().toCharArray()[counter++] + ".png");
//            ImageIO.write(bufferedImage, "PNG", output);
//        }
    }

    private BufferedImage[] separate(BufferedImage image) {
        final int w = image.getWidth();
        final int h = image.getHeight();
        boolean leftEdgeSearch = true;
        int leftEdgeX = 0;
        int red;
        int counter = 0;
        final boolean isAlpha = image.getColorModel().hasAlpha();
        BufferedImage[] result = new BufferedImage[5];
        for (int i = 0; i < w; i++) {
            int blacks = 0;
            for (int j = 0; j < h; j++) {
                red = new Color(image.getRGB(i, j), isAlpha).getRed();
                if (red == 0) {
                    ++blacks;
                    if (blacks == 2)
                        break;
                }
            }
            if (blacks >= 2) { //nasli jsme cernou radu
                if (leftEdgeSearch) { //hledali jsme zacatek
                    leftEdgeX = i;
                    leftEdgeSearch = false;
                } else {
                    //do nothing
                }
            } else {//nasli jsme bilou radu
                if (!leftEdgeSearch) {
                    result[counter++] = image.getSubimage(leftEdgeX, 0, i - leftEdgeX, h);
                    if (counter == result.length)
                        break;
                    leftEdgeSearch = true;
                }
            }
        }
        return result;
    }


    private Template findResult(BufferedImage image) {
        final Template test = new Template('X', imageToData(image));
        for (Template template : trainedSet) {
            template.countDistance(test);
            //System.out.println("template = " + template);
        }
        return Collections.min(trainedSet);
    }

    private static class RGBGrayFilter extends RGBImageFilter {
        private final int limit;
        //        private final boolean letGray;
        private static final int whiteRGB = new Color(255, 255, 255, 255).getRGB();
//        private static final int blackRGB = new Color(0, 0, 0, 255).getRGB();

        public RGBGrayFilter(int limit) {
            this.limit = limit;
            canFilterIndexColorModel = true;
        }

        public int filterRGB(int x, int y, int rgb) {
            int a = rgb & 0xff000000;
            int r = (rgb >> 16) & 0xff;
            int g = (rgb >> 8) & 0xff;
            int b = rgb & 0xff;
//		rgb = (r + g + b) / 3;	// simple average
            rgb = (r * 77 + g * 151 + b * 28) >> 8;    // NTSC luma

            if ((rgb & 0xFF) > limit) {
                return whiteRGB;
            } else {
                return a | (rgb << 16) | (rgb << 8) | rgb;
            }
        }
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
                data[offset++] = (short) red;
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