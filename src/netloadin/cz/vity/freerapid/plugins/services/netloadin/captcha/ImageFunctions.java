package cz.vity.freerapid.plugins.services.netloadin.captcha;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Functions for manipulating with Black&White images
 *
 * @author JPEXS
 */
public class ImageFunctions {

    /**
     * Crop image so there are no white space on the left/right/up/down.
     * @param img Image to crop
     * @return new cropped image
     */
    public static BufferedImage crop(BufferedImage img) {
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


    private static BufferedImage rotateImage(BufferedImage img, double theta) {
        BufferedImage temp = crop(img);
        BufferedImage ret = new BufferedImage(temp.getWidth() + 200, temp.getHeight() + 200, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = (Graphics2D) ret.getGraphics();
        g2d.setColor(Color.white);
        g2d.fillRect(0, 0, ret.getWidth(), ret.getHeight());
        g2d.rotate(theta, ret.getWidth() / 2, ret.getHeight() / 2);
        g2d.drawImage(temp, 100, 100, null);
        return crop(ret);
    }

   
    private static int getImageBlackWidth(BufferedImage img) {
        int maxright = 0;
        int minleft = img.getWidth() - 1;

        for (int y = 0; y < img.getHeight(); y++) {
            int left = -1;
            for (int x = 0; x < img.getWidth(); x++) {
                if (img.getRGB(x, y) == Color.black.getRGB()) {
                    left = x;
                    break;
                }
            }
            if (left > -1) {
                if (left < minleft) {
                    minleft = left;
                }
                int right = -1;
                for (int x = img.getWidth() - 1; x >= 0; x--) {
                    if (img.getRGB(x, y) == Color.black.getRGB()) {
                        right = x;
                        break;
                    }
                }
                if (right > -1) {
                    if (right > maxright) {
                        maxright = right;
                    }

                }
            }
        }
        return (maxright - minleft) + 1;
    }

    public static BufferedImage rotateToMinWidth(BufferedImage img) {
        int minwidth = getImageBlackWidth(img);
        double minwidththeta = 0.0;
        for (double u = -25; u <= 25; u += 5) {
            double t = u * Math.PI / 180;
            BufferedImage rotated = rotateImage(img, t);
            int w = getImageBlackWidth(rotated);
            if (w < minwidth) {
                minwidth = w;
                minwidththeta = t;
            }
        }
        if (minwidththeta != 0.0) {
            return rotateImage(img, minwidththeta);
        }        
        return img;
    }

    public static List<BufferedImage> split(BufferedImage img, int limit) {
        int whiteCount = 0;
        List<BufferedImage> ret = new ArrayList<BufferedImage>();
        int blackStart = 0;

        for (int x = 0; x < img.getWidth(); x++) {
            boolean isBlack = false;
            for (int y = 0; y < img.getHeight(); y++) {
                if (img.getRGB(x, y) == Color.black.getRGB()) {
                    isBlack = true;
                    break;
                }
            }
            if (!isBlack) {
                whiteCount++;
            } else {
                if (whiteCount >= limit) {
                    if (blackStart > 0) {
                        BufferedImage i = new BufferedImage((x - 1 - blackStart) + 1, img.getHeight(), BufferedImage.TYPE_INT_ARGB);
                        i.getGraphics().drawImage(img, -blackStart, 0, null);
                        i = crop(i);
                        ret.add(i);
                    }
                    blackStart = x;
                }
                whiteCount = 0;
            }

        }

        if (blackStart > 0) {
            BufferedImage i = new BufferedImage((img.getWidth() - 1 - blackStart) + 1, img.getHeight(), BufferedImage.TYPE_INT_ARGB);
            i.getGraphics().drawImage(img, -blackStart, 0, null);
            i = crop(i);
            ret.add(i);
        }
        return ret;
    }

    public static BufferedImage resizeTo(BufferedImage img, int w2, int h2) {
        BufferedImage ret = new BufferedImage(w2, h2, BufferedImage.TYPE_BYTE_BINARY);
        Graphics g = ret.getGraphics();
        g.setColor(Color.white);
        g.fillRect(0, 0, w2, h2);
        int h1 = img.getHeight();
        int w1 = img.getWidth();


        int w;
        int h = h1 * w2 / w1;

        if (h > h2) {
            w = w1 * h2 / h1;
            h = h2;
        } else {
            w = w2;
        }

        ret.getGraphics().drawImage(img, 0, 0,w,h, null);

        return ret;
    }
}
