package cz.vity.freerapid.plugins.services.megaupload;

import java.awt.image.BufferedImage;
import java.util.*;

/**
 * @author Ludek Zika
 */

class EditImage {
    //private final static Logger logger = Logger.getLogger(EditImage.class.getName());
    private BufferedImage in;
    private BufferedImage out;

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
        final HashMap<Integer, Integer> histogram = new HashMap<Integer, Integer>();
        int rgb;
        int count;
        final int width = in.getWidth();
        final int height = in.getHeight();
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
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
        Collections.sort(keys,
                new Comparator<Integer>() {
                    public int compare(Integer left, Integer right) {
                        Integer leftValue = histogram.get(left);
                        Integer rightValue = histogram.get(right);

                        return rightValue.compareTo(leftValue);
                    }

                });
        final ArrayList<Integer> mostColor = new ArrayList<Integer>();

        mostColor.add(keys.get(1));
        mostColor.add(keys.get(2));
        mostColor.add(keys.get(3));

        final HashMap<Integer, Integer> shiftMap = new HashMap<Integer, Integer>();

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int color = in.getRGB(x, y);
                if (mostColor.contains(color)) {
                    if (shiftMap.containsKey(color)) {
                        int shift = shiftMap.get(color);
                        int shiftX = x + shift;
                        if (shiftX >= 0 && shiftX < width) out.setRGB(shiftX, y, color);

                    } else shiftMap.put(color, countShift(shiftMap.size(), x));

                }

            }

        }
        return out;
    }

    private static Integer countShift(int size, int x) {
        if (size == 0) {
            if (x < 5) return -x;
            return -5;
        }
        if (size == 1) return 0;
        if (size == 2) return 5;

        return 0;
    }


}
