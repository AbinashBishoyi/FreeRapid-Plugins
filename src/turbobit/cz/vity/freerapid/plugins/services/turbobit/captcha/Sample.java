package cz.vity.freerapid.plugins.services.turbobit.captcha;

import java.awt.image.BufferedImage;

/**
 * @author JPEXS
 */
public class Sample {
    public BufferedImage sampleImage;
    public char letter;
    public int offset;
    public double distance;

    public static int IMAGEWIDTH = 20;
    public static int IMAGEHEIGHT = 25;

    public Sample(BufferedImage sampleImage, int letter, int offset, boolean nochange) {
        this.sampleImage = sampleImage;
        this.offset = offset;
        if (!nochange) {
            this.sampleImage = ImageFunctions.rotateToMinWidth(sampleImage);
            this.sampleImage = ImageFunctions.resizeTo(this.sampleImage, IMAGEWIDTH, IMAGEHEIGHT);
        }
        this.letter = (char) letter;
    }

    public Sample(BufferedImage sampleImage, int letter, int offset) {
        this(sampleImage, letter, offset, false);
    }


    public double distanceTo(Sample smp) {
        return compareImages(sampleImage, offset, IMAGEWIDTH, smp.sampleImage, smp.offset, IMAGEWIDTH);
    }

    public double compareImages(BufferedImage img, int offset1, int width1, BufferedImage img2, int offset2, int width2) {
        long stejne = 0;
        long porovnane = 0;
        int minW = width1 < width2 ? width1 : width2;
        int minH = img.getHeight() < img2.getHeight() ? img.getHeight() : img2.getHeight();
        for (int x = 0; x < minW; x++) {
            for (int y = 0; y < minH; y++) {
                porovnane++;
                if (img.getRGB(x + offset1, y) == img2.getRGB(x + offset2, y)) {
                    stejne++;
                }
            }
        }
        double tmppercent = ((double) stejne) / ((double) porovnane) * 100;
        double percent = ((double) ((int) (tmppercent * 100))) / 100;
        return percent;
    }

}