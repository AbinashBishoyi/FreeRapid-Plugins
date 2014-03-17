package cz.vity.freerapid.plugins.services.megaupload;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

/**
 * @author Ludek Zika
 */

class EditImage {
    //private final static Logger logger = Logger.getLogger(EditImage.class.getName());
    private BufferedImage in;
    private BufferedImage out;
    private final static int WIDER = 30;
    private final static int NEIGHBOURDOOD = 25;
    private boolean use_similar_color = false;

    public void setUse_similar_color(boolean use_similar_color) {
        this.use_similar_color = use_similar_color;
    }


    EditImage(BufferedImage in) {
        this.in = in;
        out = new BufferedImage(in.getWidth() + WIDER, in.getHeight(), BufferedImage.TYPE_INT_RGB);

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
        for (int x = width; x < width + WIDER; x++) {
            for (int y = 0; y < height; y++) {
                out.setRGB(x, y, 0xFFFFFF);
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
                if (use_similar_color && !mostColor.contains(color)) {
                    int closed = closeColor(color, mostColor);
                    if (closed != color) color = closed;
                }
                if (mostColor.contains(color)) {
                    if (shiftMap.containsKey(color)) {
                        int shift = shiftMap.get(color);
                        int shiftX = x + shift;
                        if (shiftX >= 0 && shiftX < width + WIDER) out.setRGB(shiftX, y, color);

                    } else shiftMap.put(color, countShift(shiftMap.size()));

                }

            }

        }
        return out;
    }

    private static Integer countShift(int size) {
        if (size == 0) return 0;

        if (size == 1) return 10;
        if (size == 2) return 20;

        return 0;
    }

    private static int closeColor(int color, ArrayList<Integer> pc) {
        double dist = Double.MAX_VALUE;
        int closest = color;
        for (int one : pc) {
            double cur_dist = distance(color, one);
            if (cur_dist < NEIGHBOURDOOD && cur_dist < dist) {
                dist = cur_dist;
                closest = one;
            }
        }
        return closest;

    }

    private static double distance(int c1, int c2) {
        Color col1 = new Color(c1);
        Color col2 = new Color(c2);

        double d = Math.pow(col1.getRed() - col2.getRed(), 2) + Math.pow(col1.getBlue() - col2.getBlue(), 2) + Math.pow(col1.getGreen() - col2.getGreen(), 2);

        return Math.sqrt(d);
    }
}
