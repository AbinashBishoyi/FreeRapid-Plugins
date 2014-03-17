package cz.vity.freerapid.plugins.services.circlecaptcha;

import java.awt.image.BufferedImage;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * See <a href=http://en.wikipedia.org/wiki/Hough_transform>this</a>
 * for an overview of Hough transform.
 *
 * @author ntoskrnl
 */
public class CircleHoughTransform {

    private final BufferedImage input;
    private final int backgroundColor;
    private final int[][][] houghValues;
    private int[][][] lookupTable = null;
    private int maxHough = 0;
    private final int width, height, depth;
    private final int radiusMin, radiusMax, radiusIncrement;

    /**
     * @param input           Image to perform Hough transform on
     * @param backgroundColor Background color of the image; alpha value is discarded
     * @param radiusMin       Minimum radius of circles to find
     * @param radiusMax       Maximum radius of circles to find
     * @param radiusIncrement Increment radius
     */
    public CircleHoughTransform(final BufferedImage input, final int backgroundColor, final int radiusMin, final int radiusMax, final int radiusIncrement) {
        this.input = input;
        this.width = input.getWidth();
        this.height = input.getHeight();
        this.backgroundColor = backgroundColor & 0xFFFFFF;
        this.radiusMin = radiusMin;
        this.radiusMax = radiusMax;
        this.radiusIncrement = radiusIncrement;
        this.depth = (radiusMax - radiusMin) / radiusIncrement + 1;
        this.houghValues = new int[width][height][depth];
    }

    private int getRadiusIndex(final int radius) {
        return (radius - radiusMin) / radiusIncrement;
    }

    /**
     * Perform Hough transform on the input image. The result is stored in an internal state.
     */
    public void performHoughTransform() {
        if (lookupTable == null) {
            lookupTable = generateLookupTable();
        }
        final int tableLength = lookupTable[0].length;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                for (int radius = radiusMin; radius <= radiusMax; radius += radiusIncrement) {
                    if ((input.getRGB(x, y) & 0xFFFFFF) != backgroundColor) {
                        final int r = getRadiusIndex(radius);
                        for (int i = 0; i < tableLength; i++) {
                            final int a = x + lookupTable[0][i][r];
                            final int b = y + lookupTable[1][i][r];
                            if (a >= 0 && a < width && b >= 0 && b < height) {
                                final int value = (houghValues[a][b][r] += 1);
                                if (value > maxHough) {
                                    maxHough = value;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private int[][][] generateLookupTable() {
        final int numSectors = 8 * radiusMin;
        final int[][][] table = new int[2][numSectors][depth];
        for (int radius = radiusMin; radius <= radiusMax; radius += radiusIncrement) {
            final int r = getRadiusIndex(radius);
            for (int i = 0; i < numSectors; i++) {
                final double angle = 2 * Math.PI * i / numSectors;
                final int x = (int) Math.round(radius * Math.cos(angle));
                final int y = (int) Math.round(radius * Math.sin(angle));
                table[0][i][r] = x;
                table[1][i][r] = y;
            }
        }
        return table;
    }

    /**
     * Generates a visual representation of the Hough transform.
     * {@link #performHoughTransform()} should be invoked prior to this method.
     *
     * @param radius The radius to generate the picture of
     * @return Visual representation of the Hough transform
     */
    public BufferedImage generateHoughImage(final int radius) {
        if (maxHough == 0) {
            throw new IllegalStateException("Hough transform not performed yet or input image is empty");
        }
        final BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        final int r = getRadiusIndex(radius);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                final int color = (int) Math.round(255d * houghValues[x][y][r] / maxHough);
                image.setRGB(x, y, (color << 16) | (color << 8) | (color));
            }
        }
        return image;
    }

    /**
     * Finds circles from the input image using Hough transform.
     * {@link #performHoughTransform()} should be invoked prior to this method.
     * Additionally, this method can only be invoked once.
     *
     * @param threshold Threshold of certainty; should be between 0 and 1
     * @return List of circles found
     */
    public List<Circle> findCircles(final double threshold) {
        if (maxHough == 0) {
            throw new IllegalStateException("Hough transform not performed yet or input image is empty");
        }
        final List<Circle> list = new LinkedList<Circle>();
        for (int radius = radiusMin; radius <= radiusMax; radius += radiusIncrement) {
            final int r = getRadiusIndex(radius);
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    if ((double) houghValues[x][y][r] / (double) maxHough >= threshold) {
                        list.add(findMaximumNear(x, y));
                    }
                }
            }
        }
        maxHough = 0;
        return list;
    }

    private Circle findMaximumNear(final int x, final int y) {
        final int searchRadius = 5;
        final int x1 = Math.max(0, x - searchRadius);
        final int x2 = Math.min(width, x + searchRadius + 1);
        final int y1 = Math.max(0, y - searchRadius);
        final int y2 = Math.min(height, y + searchRadius + 1);
        int maximumValue = 0, maximumX = 0, maximumY = 0, maximumRadius = 0;
        for (int radius = radiusMin; radius <= radiusMax; radius += radiusIncrement) {
            final int r = getRadiusIndex(radius);
            for (int b = y1; b < y2; b++) {
                for (int a = x1; a < x2; a++) {
                    if (Math.hypot(a - x, b - y) <= searchRadius) {
                        final int value = houghValues[a][b][r];
                        if (value > maximumValue) {
                            maximumValue = value;
                            maximumX = a;
                            maximumY = b;
                            maximumRadius = radius;
                        }
                        houghValues[a][b][r] = 0;
                    }
                }
            }
        }
        return new Circle(maximumX, maximumY, maximumRadius);
    }

    /**
     * @param threshold Threshold of certainty; should be between 0 and 1
     * @return The circle with the most background colored pixels in its edge, or null if none found
     */
    public Circle findOpenCircle(final double threshold) {
        final SortedMap<Integer, Circle> map = new TreeMap<Integer, Circle>();
        for (final Circle circle : findCircles(threshold)) {
            final int emptyPixels = numberOfEmptyPixels(circle);
            if (emptyPixels > 0) {
                map.put(emptyPixels, circle);
            }
        }
        if (!map.isEmpty()) {
            return map.get(map.lastKey());
        } else {
            return null;
        }
    }

    private int numberOfEmptyPixels(final Circle circle) {
        int emptyPixels = 0;
        final int tableLength = lookupTable[0].length;
        final int r = getRadiusIndex(circle.r());
        for (int i = 0; i < tableLength; i++) {
            final int x = circle.x() + lookupTable[0][i][r];
            final int y = circle.y() + lookupTable[1][i][r];
            if (x >= 0 && x < width && y >= 0 && y < height
                    && (input.getRGB(x, y) & 0xFFFFFF) == backgroundColor) {
                emptyPixels++;
            }
        }
        return emptyPixels;
    }

}
