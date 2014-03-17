package cz.vity.freerapid.plugins.services.duckload.captcha;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.util.logging.Logger;

/**
 * Provides method for preparing captcha images for recognition
 *
 * @author ntoskrnl
 */
public class CaptchaPreparer {
    private final static Logger logger = Logger.getLogger(CaptchaPreparer.class.getName());
    private final static int LETTERS_IN_CAPTCHA = CaptchaRecognizer.LETTERS_IN_CAPTCHA;

    /**
     * Prepares captcha image for recognition
     *
     * @param image Input image
     * @return Prepared image
     */
    public static List<BufferedImage> getPreparedImage(BufferedImage image) {
        image = image.getSubimage(1, 1, image.getWidth() - 2, image.getHeight() - 2);//get rid of the borders
        image.setRGB(0, 0, Color.WHITE.getRGB());//make sure getAllBlocks() gets the background color right
        final int w = image.getWidth();
        final int h = image.getHeight();

        final List<Block> allBlocks = ImageFunctions.getAllBlocks(image, 20);
        final Set<Block> set = new TreeSet<Block>();
        final Map<Integer, ArrayList<Block>> blocksByColor = new HashMap<Integer, ArrayList<Block>>();

        for (final Block block : allBlocks) {//group the blocks by color
            ArrayList<Block> l = blocksByColor.get(block.color);
            if (l == null)
                l = new ArrayList<Block>();
            l.add(block);
            blocksByColor.put(block.color, l);
        }

        for (final List<Block> l : blocksByColor.values()) {//merge blocks with same color
            set.add(Block.merge(l.toArray(new Block[l.size()])));
        }

        if (set.size() < LETTERS_IN_CAPTCHA) {
            logger.warning("Problem separating characters from image (only " + set.size() + " letters obtained)");
        } else if (set.size() > LETTERS_IN_CAPTCHA) {
            logger.info("Too many blocks separated, clearing up...");

            final ArrayList<Block> l = new ArrayList<Block>();
            l.addAll(set);
            Collections.sort(l, new Comparator<Block>() {//sort in order smallest -> largest

                public int compare(final Block a, final Block b) {
                    return new Integer(a.block.size()).compareTo(b.block.size());
                }
            });
            while (l.size() > LETTERS_IN_CAPTCHA) {//remove the smallest ones
                l.remove(0);
            }
            set.clear();
            set.addAll(l);
        }

        final List<BufferedImage> ret = new ArrayList<BufferedImage>();
        for (final Block b : set) {//convert to images
            ret.add(blockToImage(b, w, h));
        }

        return ret;
    }

    private static BufferedImage blockToImage(final Block block, final int w, final int h) {
        final BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_BINARY);
        final Graphics g = image.getGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, w, h);

        for (final Point p : block.block) {
            image.setRGB(p.x, p.y, Color.BLACK.getRGB());
        }

        return image;
    }

}