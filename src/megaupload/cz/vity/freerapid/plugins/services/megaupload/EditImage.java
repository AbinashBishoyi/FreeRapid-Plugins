package cz.vity.freerapid.plugins.services.megaupload;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.logging.Logger;
import java.io.File;
import java.io.IOException;

/**
 * @author Ludek Zika
 */

class EditImage {
    private final static Logger logger = Logger.getLogger(EditImage.class.getName());
    BufferedImage in;
    BufferedImage out;

    EditImage(BufferedImage in) {
        this.in = in;
        out = new BufferedImage(in.getWidth(), in.getHeight(), BufferedImage.TYPE_INT_RGB);

    /*        File fo = new File("D:\\capgenIn.bmp");
       try {
           ImageIO.write(in, "bmp", fo);
       } catch (IOException e) {
           e.printStackTrace();
       }
              */
    }

    BufferedImage separate() {
        HashMap<Integer, Integer> histogram = new HashMap<Integer, Integer>();
        int rgb;
        int count;
        for (int x = 0; x < in.getWidth(); x++) {
            for (int y = 0; y < in.getHeight(); y++) {
                out.setRGB(x, y, 0xFFFFFF);
                rgb = in.getRGB(x, y);
                if (histogram.get(rgb) == null) {
                    count = 1;
                } else {
                    count = 1 + histogram.get(rgb);
                }
                histogram.put(rgb, count);
            }
        }
        List<Integer> keys = new ArrayList<Integer>(histogram.keySet());
        final Map<Integer, Integer> mapForComp = histogram;
        Collections.sort(keys,
                new Comparator<Integer>() {
                    public int compare(Integer left, Integer right) {
                        Integer leftValue = mapForComp.get(left);
                        Integer rightValue = mapForComp.get(right);

                        return rightValue.compareTo(leftValue);
                    }

                });
        ArrayList<Integer> mostColor = new ArrayList<Integer>();

        mostColor.add(keys.get(1));
        mostColor.add(keys.get(2));
        mostColor.add(keys.get(3));

        HashMap<Integer, Integer> shiftMap = new HashMap<Integer, Integer>();

        for (int x = 0; x < in.getWidth(); x++) {
            for (int y = 0; y < in.getHeight(); y++) {
                int color = in.getRGB(x, y);
                if (mostColor.contains(color)) {
                    if (shiftMap.containsKey(color)) {
                        int shift = shiftMap.get(color);
                        int shiftX = x + shift;
                        if (shiftX >= 0 && shiftX < in.getWidth()) out.setRGB(shiftX, y, color);

                    } else shiftMap.put(color, countShift(shiftMap.size(), x));

                }

            }

        }
        return out;
    }

    private Integer countShift(int size, int x) {
        if (size == 0) {
            if (x < 5) return -x;
            return -5;
        }
        if (size == 1) return 0;
        if (size == 2) return 5;

        return 0;
    }


}
