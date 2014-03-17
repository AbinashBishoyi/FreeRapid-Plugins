package cz.vity.freerapid.plugins.services.duckload.captcha;

import cz.vity.freerapid.plugins.services.duckload.util.SortedList;

import java.awt.*;
import java.util.Collection;
import java.util.Comparator;

/**
 * Stores a <tt>Collection</tt> of <tt>Point</tt>s resembling an area in an image.
 * Also stores the color of the area.
 * <p/>
 * In addition, a static function {@link #merge} is supported, and
 * the whole class is comparable (according to the leftmost <tt>Point</tt>).
 *
 * @author ntoskrnl
 */
public class Block implements Comparable<Block> {
    public Collection<Point> data;
    public int color;

    /**
     * Constructs a new <tt>Block</tt> with the color black.<p />
     * See {@link #Block(java.util.Collection, int)}.
     *
     * @param data data for the newly created <tt>Block</tt>
     */
    public Block(final Collection<Point> data) {
        this(data, Color.BLACK.getRGB());
    }

    /**
     * Constructs a new <tt>Block</tt> with the specified data,
     * with the specified color.<p />
     * For convenience, the data is sorted according to
     * the <tt>x</tt> parameter of the <tt>Point</tt>s.
     *
     * @param data  data for the newly created <tt>Block</tt>
     * @param color color in RGB format
     */
    public Block(final Collection<Point> data, final int color) {
        this.data = new SortedList<Point>(data, new Comparator<Point>() {
            public int compare(final Point a, final Point b) {
                int i = new Integer(a.x).compareTo(b.x);
                if (i == 0) i = new Integer(a.y).compareTo(b.y);
                return i;
            }
        });
        this.color = color;
    }

    /**
     * Returns size of block.
     *
     * @return size of block data
     */
    public int getSize() {
        return data.size();
    }

    /**
     * Merges the specified <tt>Block</tt>s.
     *
     * @param blocks blocks to merge
     * @return new <tt>Block</tt> with the data from the arguments
     *         and the color from the first one,
     *         or <tt>null</tt> if length of arguments is zero
     */
    public static Block merge(final Block... blocks) {
        if (blocks.length < 1) return null;
        final Collection<Point> tmp = blocks[0].data;
        for (int i = 1; i < blocks.length; i++) {
            tmp.addAll(blocks[i].data);
        }
        return new Block(tmp, blocks[0].color);
    }

    public int compareTo(final Block other) {
        int smallestXa = Integer.MAX_VALUE;
        for (final Point p : this.data) {
            if (p.x < smallestXa) smallestXa = p.x;
        }
        int smallestXb = Integer.MAX_VALUE;
        for (final Point p : other.data) {
            if (p.x < smallestXb) smallestXb = p.x;
        }
        return new Integer(smallestXa).compareTo(smallestXb);
    }

    @Override
    public String toString() {
        return "Data size: " + getSize() + ", color " + color;
    }

}