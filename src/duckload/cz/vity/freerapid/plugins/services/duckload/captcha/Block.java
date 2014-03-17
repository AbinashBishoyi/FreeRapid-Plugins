package cz.vity.freerapid.plugins.services.duckload.captcha;

import java.awt.*;
import java.util.ArrayList;

/**
 * Stores a <tt>List</tt> of <tt>Point</tt>s resembling an area in an image.
 * Also stores the color of the area.
 * <p/>
 * In addition, a static function <tt>merge</tt> is supported, and
 * the whole class is comparable (according to the leftmost <tt>Point</tt>).
 *
 * @author ntoskrnl
 */
public class Block implements Comparable<Block> {
    public ArrayList<Point> block;
    public int color;

    public Block(final ArrayList<Point> block, final int color) {
        this.block = block;
        this.color = color;
    }

    public boolean append(final Block other) {
        return this.block.addAll(other.block);
    }

    public static Block merge(final Block... blocks) {
        final ArrayList<Point> tmp = blocks[0].block;
        for (int i = 1; i < blocks.length; i++) {
            tmp.addAll(blocks[i].block);
        }
        return new Block(tmp, Color.BLACK.getRGB());
    }

    public int compareTo(final Block other) {
        int smallestXa = Integer.MAX_VALUE;
        for (final Point p : this.block) {
            if (p.x < smallestXa) smallestXa = p.x;
        }
        int smallestXb = Integer.MAX_VALUE;
        for (final Point p : other.block) {
            if (p.x < smallestXb) smallestXb = p.x;
        }
        return new Integer(smallestXa).compareTo(smallestXb);
    }
}