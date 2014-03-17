package cz.vity.freerapid.plugins.services.megaupload.captcha;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 *
 * @author JPEXS
 *
 * Based on script by Shaun Friedle 2009
 * http://userscripts.org/scripts/show/38736
 */
public class CaptchaReader {

    private static NeuralNet net=null;

    private static boolean within(BufferedImage image,int x, int y) {
        return 0 <= x && x < image.getWidth() && 0 <= y && y < image.getHeight();
    }

    private static class MyComparator implements Comparator {

        public int compare(Object o1, Object o2) {
            if (o1 instanceof Point) {
                if (o2 instanceof Point) {
                    return ((Point) o1).x - ((Point) o2).x;
                }
            }
            return 0;
        }
    }

    private static int min_size_count(List<List<Point>> blocks, int min_size) {
        int count = 0;
        for (int i = 0; i < blocks.size(); i++) {
            if (blocks.get(i).size() > min_size) {
                count++;
            }
        }
        return count;
    }

    private static List<List<Point>> split_block(BufferedImage image,List<Point> block) {
        int histogram[] = new int[image.getWidth()];
        for(int h=0;h<histogram.length;h++){
            histogram[h]=-1;
        }
        int start = block.get(0).x + 5;
        int end = block.get(block.size() - 1).x - 5;

        for (int i = 0; i < block.size(); i++) {
            if (start <= block.get(i).x && block.get(i).x <= end) {
                histogram[block.get(i).x]++;
            }
        }

        int low = start;
        for (int i = 0; i < histogram.length; i++) {
            if(histogram[i]!=-1)
            if (histogram[i] < histogram[low]) {
                low = i;
            }
        }

        List<Point> left = new ArrayList<Point>();
        List<Point> right = new ArrayList<Point>();

        for (int i = 0; i < block.size(); i++) {
            if (block.get(i).x <= low) {
                left.add(block.get(i));
            }
            if (block.get(i).x >= low) {
                right.add(block.get(i));
            }
        }

        List<List<Point>> ret = new ArrayList<List<Point>>();
        ret.add(left);
        ret.add(right);
        return ret;
    }

    private static List<List<Point>> get_large_blocks(List<List<Point>> blocks, int count) {
        List<List<Point>> large = new ArrayList<List<Point>>();
        for (int i = 0; i < blocks.size(); i++) {
            if (large.size() < count) {
                large.add(blocks.get(i));
            } else {
                int greatest_diff = 0;
                int greatest_diff_index = -1;
                for (int j = 0; j < large.size(); j++) {
                    int diff = blocks.get(i).size() - large.get(j).size();
                    if (diff > 0 && (diff > greatest_diff || greatest_diff_index == -1)) {
                        greatest_diff = diff;
                        greatest_diff_index = j;
                    }
                }

                if (greatest_diff_index != -1) {
                    large.set(greatest_diff_index, blocks.get(i));
                }
            }
        }
        Collections.sort(large, new MyComparator());
        return large;
    }

    private static List<List<List<Point>>> get_blocks(BufferedImage image) {

        List<Point> found = new ArrayList<Point>();

        List<Point> bg = get_colour_block(image,0, 0);
        for (int i = 0; i < bg.size(); i++) {
            found.add(bg.get(i));
        }

        List<List<Point>> black_blocks = new ArrayList<List<Point>>();
        List<List<Point>> white_blocks = new ArrayList<List<Point>>();
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                int colour = image.getRGB(x, y);
                ArrayList<Point> block = new ArrayList<Point>();

                if (found.contains(new Point(x, y))) {
                    continue;
                }

                if (colour == Color.black.getRGB()) {
                    block = get_colour_block(image,x, y);
                    Collections.sort(block, new MyComparator());
                    if (block.size() >= 5) {
                        black_blocks.add(block);
                    }
                }
                if (colour == Color.white.getRGB()) {
                    block = get_colour_block(image,x, y);
                    Collections.sort(block, new MyComparator());
                    if (block.size() >= 5) {
                        white_blocks.add(block);
                    }
                }

                if (block != null) {
                    for (int i = 0; i < block.size(); i++) {
                        found.add(block.get(i));
                    }
                }
            }
        }

        while (min_size_count(black_blocks, 10) < 4) {
            int wide = 0;
            for (int i = 0; i < black_blocks.size(); i++) {
                if (black_blocks.get(i).size() > black_blocks.get(wide).size()) {
                    wide = i;
                }
            }

            List<List<Point>> blocks = split_block(image,black_blocks.get(wide));
            black_blocks.remove(wide);
            black_blocks.add(wide, blocks.get(1));
            black_blocks.add(wide, blocks.get(0));
        }
        List<List<List<Point>>> ret = new ArrayList<List<List<Point>>>();
        ret.add(black_blocks);
        ret.add(white_blocks);
        return ret;
    }

    private static ArrayList<Point> get_colour_block(BufferedImage image,int bx, int by) {
        ArrayList<Point> block = new ArrayList<Point>();
        int colour = image.getRGB(bx, by);
        List<Point> edge = new ArrayList<Point>();
        edge.add(new Point(bx, by));
        block.add(new Point(bx, by));

        while (edge.size() > 0) {
            List<Point> newedge = new ArrayList<Point>();
            for (int i = 0; i < edge.size(); i++) {
                int x = edge.get(i).x;
                int y = edge.get(i).y;
                List<Point> adjacent = new ArrayList<Point>();
                adjacent.add(new Point(x + 1, y));
                adjacent.add(new Point(x - 1, y));
                adjacent.add(new Point(x, y + 1));
                adjacent.add(new Point(x, y - 1));
                for (int j = 0; j < adjacent.size(); j++) {
                    int s = adjacent.get(j).x;
                    int t = adjacent.get(j).y;

                    if (within(image,s, t) && !block.contains(new Point(s, t)) && image.getRGB(s, t) == colour) {
                        block.add(new Point(s, t));
                        newedge.add(new Point(s, t));
                    }
                }
            }
            edge = newedge;
        }
        return block;
    }


    private static char guess_letter(NeuralNet net, List<Integer> receptors, boolean digit)
  {
    char[] output_map = new char[]{'1', '2', '3', '4', '5',
                      '6', '7', '8', '9', 'a',
                      'b', 'c', 'd', 'e', 'f',
                      'g', 'h', 'k', 'm', 'n',
                      'p', 'q', 'r', 's', 't',
                      'u', 'v', 'w', 'x', 'y',
                      'z'};
    double inputs[]=new double[receptors.size()];
    for(int i=0;i<receptors.size();i++){
        inputs[i]=receptors.get(i).doubleValue();
    }
    double[] output = net.test(inputs);

    int highest = 0;
    for (int i = 0; i < output.length; i++)
      {
        if (output[i] > output[highest] && ((!digit && i >= 9) ||
                                            (digit && i < 9)))
            highest = i;
      }

    if (!digit && highest == 0)
        highest = 9;

    return output_map[highest];
  }

    private static String decodeWithNet(BufferedImage image,NeuralNet net) {
        List<List<List<Point>>> blocks = get_blocks(image);
        List<List<Point>> black_blocks = blocks.get(0);
        List<List<Point>> white_blocks = blocks.get(1);
        List<List<Point>> large_black = get_large_blocks(black_blocks, 4);
        List<List<Point>> large_white = get_large_blocks(white_blocks, 3);
        List<List<Point>> small_black = new ArrayList<List<Point>>();
        for (int i = 0; i < black_blocks.size(); i++) {
            boolean in_large = false;
            for (int j = 0; j < large_black.size(); j++) {
                if (large_black.get(j).contains(black_blocks.get(i).get(0))) {
                    in_large = true;
                }
            }
            if (!in_large) {
                small_black.add(black_blocks.get(i));
            }
        }

        List<List<Point>> small_white = new ArrayList<List<Point>>();
        for (int i = 0; i < white_blocks.size(); i++) {
            boolean in_large = false;
            for (int j = 0; j < large_white.size(); j++) {
                if (large_white.get(j).contains(white_blocks.get(i).get(0))) {
                    in_large = true;
                }
            }
            if (!in_large) {
                small_white.add(white_blocks.get(i));
            }
        }
        List<BufferedImage> chars = null;
        try {
            chars = get_chars(image,large_black, large_white, small_black, small_white);
        } catch (Exception e) {
            return null;
        }

        
        String code = "";
        for (int i = 0; i < chars.size(); i++) {
            List<Integer> receptors = check_receptors(chars.get(i));
            code += guess_letter(net, receptors, i == 3);
        }

        return code;
    }

    private static List<Integer> check_receptors(BufferedImage image) {
        List<Integer> receptors = new ArrayList<Integer>();
        for (int x = 0; x < 33; x += 3) {
            for (int y = 0; y < 30; y += 2) {
                if ((image.getRGB(x, y) & 0xff) >= 128) {
                    receptors.add(1);
                } else {
                    receptors.add(0);
                }
            }
        }

        return receptors;
    }

    private static BufferedImage crop_canvas(BufferedImage image, Point start_point) {

        int im_left = 0;
        int im_right = 0;
        int im_top = 0;
        int im_bottom = 0;

        Point char_pos = null;
        for (int interval = 25; interval > 1; interval = (int) Math.floor((double) interval / 2.0)) {
            for (int x = start_point.x; x < start_point.x + 50; x += interval) {
                for (int y = start_point.y; y < start_point.y + 50; y++) {
                    if (image.getRGB(x, y) == Color.white.getRGB()) {
                        char_pos = new Point(x, y);
                    }
                }
            }
            if (char_pos != null) {
                break;
            }
        }

        for (int dir = -1; dir <= 2; dir += 2) {
            for (int x = char_pos.x; 0 <= x && x < image.getWidth(); x += dir) {
                int white_pixels = 0;
                for (int y = Math.max(0, char_pos.y - 50);
                        y < Math.min(image.getHeight(), char_pos.y + 50); y += 4) {
                    if ((image.getRGB(x, y) & 0xff) >= 128) {
                        white_pixels++;
                    }
                }

                if (white_pixels == 0) {
                    for (int y = Math.max(0, char_pos.y - 50);
                            y < Math.min(image.getHeight(), char_pos.y + 50); y++) {
                        if ((image.getRGB(x, y) & 0xff) >= 128) {
                            white_pixels++;
                        }
                    }
                }

                if (white_pixels == 0) {
                    if (dir == -1) {
                        im_left = x + 1;
                        break;
                    } else {
                        im_right = x - 1;
                        break;
                    }
                }
            }
        }

        for (int dir = -1; dir <= 2; dir += 2) {
            for (int y = char_pos.y; 0 <= y && y < image.getHeight(); y += dir) {
                int white_pixels = 0;
                for (int x = Math.max(0, char_pos.x - 50);
                        x < Math.min(image.getWidth(), char_pos.x + 50); x += 4) {
                    if ((image.getRGB(x, y) & 0xff) >= 128) {
                        white_pixels++;
                    }
                }

                if (white_pixels == 0) {
                    for (int x = Math.max(0, char_pos.x - 50);
                            x < Math.min(image.getWidth(), char_pos.x + 50); x++) {
                        if ((image.getRGB(x, y) & 0xff) >= 128) {
                            white_pixels++;
                        }
                    }
                }

                if (white_pixels == 0) {
                    if (dir == -1) {
                        im_top = y + 1;
                        break;
                    } else {
                        im_bottom = y - 1;
                        break;
                    }
                }
            }
        }

        int width = im_right - im_left;
        int height = im_bottom - im_top;

        BufferedImage image2 = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        Graphics g = image2.getGraphics();
        g.setColor(Color.black);
        g.fillRect(0, 0, 100, 100);
        g.drawImage(image, 0, 0, width, height, im_left, im_top, im_left + width, im_top + height, null);
        //g.drawImage(image, 0, 0, null);
        return image2;
    }

    private static List<BufferedImage> get_chars(BufferedImage gimage,List<List<Point>> large_black, List<List<Point>> large_white, List<List<Point>> small_black, List<List<Point>> small_white) {
        List<BufferedImage> chars = new ArrayList<BufferedImage>();
        for (int i = 0; i < large_black.size(); i++) {
            BufferedImage image = new BufferedImage(gimage.getWidth(), gimage.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics g = image.getGraphics();
            g.setColor(Color.black);
            g.fillRect(0, 0, image.getWidth(), image.getHeight());

            for (int j = 0; j < large_black.get(i).size(); j++) {
                image.setRGB(large_black.get(i).get(j).x, large_black.get(i).get(j).y, Color.white.getRGB());
            }

            for (int j = 0; j < small_white.size(); j++) {
                if (i > 0) {
                    if (small_white.get(j).get(0).x <
                            large_black.get(i - 1).get(large_black.get(i - 1).size() - 1).x &&
                            small_white.get(j).get(small_white.get(j).size() - 1).x >
                            large_black.get(i).get(0).x) {
                        for (int k = 0; k < small_white.get(j).size(); k++) {
                            image.setRGB(small_white.get(j).get(k).x,
                                    small_white.get(j).get(k).y, Color.white.getRGB());
                        }
                    }
                }

                if (i < large_black.size() - 1) {
                    if (small_white.get(j).get(0).x <
                            large_black.get(i).get(large_black.get(i).size() - 1).x &&
                            small_white.get(j).get(small_white.get(j).size() - 1).x >
                            large_black.get(i + 1).get(0).x) {
                        for (int k = 0; k < small_white.get(j).size(); k++) {
                            image.setRGB(small_white.get(j).get(k).x,
                                    small_white.get(j).get(k).y, Color.white.getRGB());
                        }
                    }
                }
            }

            for (int j = 0; j < small_black.size(); j++) {
                if (small_black.get(j).size() < 30) {
                    continue;
                }

                int common_columns = 0;
                for (int k = 0; k < small_black.get(j).size(); k++) {
                    if (large_black.get(i).get(0).x <= small_black.get(j).get(k).x &&
                            small_black.get(j).get(k).x <=
                            large_black.get(i).get(large_black.get(i).size() - 1).x) {
                        common_columns++;
                    }
                }

                if (common_columns < 10) {
                    continue;
                }

                if (i > 0) {
                    int common_previous = 0;
                    for (int k = 0; k < small_black.get(j).size(); k++) {
                        if (large_black.get(i - 1).get(0).x <= small_black.get(j).get(k).x &&
                                small_black.get(j).get(k).x <=
                                large_black.get(i - 1).get(large_black.get(i - 1).size() - 1).x) {
                            common_previous++;
                        }
                    }

                    if (common_columns < common_previous) {
                        continue;
                    }
                }

                if (i < large_black.size() - 1) {
                    int common_next = 0;
                    for (int k = 0; k < small_black.get(j).size(); k++) {
                        if (large_black.get(i + 1).get(0).x <= small_black.get(j).get(k).x &&
                                small_black.get(j).get(k).x <=
                                large_black.get(i + 1).get(large_black.get(i + 1).size() - 1).x) {
                            common_next++;
                        }
                    }

                    if (common_columns < common_next) {
                        continue;
                    }
                }

                for (int k = 0; k < small_black.get(j).size(); k++) {
                    image.setRGB(small_black.get(j).get(k).x,
                            small_black.get(j).get(k).y, Color.white.getRGB());
                }
            }

            BufferedImage image2 = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2 = (Graphics2D) image2.getGraphics();
            g2.setColor(Color.black);



            g2.fillRect(0, 0, image2.getWidth(), image2.getHeight());





            if (i % 2 == 1) {
                g2.rotate(0.08726646);
            } else {
                g2.rotate(6.19591884);
            }


            g2.drawImage(image, 50, 75, null);
            g2.setColor(Color.red);


            List<Point> start_points = new ArrayList<Point>();
            start_points.add(new Point(50, 65));
            start_points.add(new Point(50, 70));
            start_points.add(new Point(85, 55));
            start_points.add(new Point(90, 80));


            chars.add(crop_canvas(image2, start_points.get(i)));
        }
        return chars;

    }


    public static String read(BufferedImage image){
        if(net==null) net=new NeuralNet("resources/captcha.nn");
        return decodeWithNet(image, net);
    }   
}
