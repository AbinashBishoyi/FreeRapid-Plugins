package cz.vity.freerapid.plugins.services.keycaptcha;

import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

/**
 * @author ntoskrnl
 */
final class KeyCaptchaRecognizer {

    private final static int EXTRA_SPACE = 3;

    public static List<Point> recognize(final KeyCaptchaImages images) throws PluginImplementationException {
        final List<Point> list = new ArrayList<Point>(images.getPieces().size());
        final int width = images.getBackground().getWidth() + 2 * EXTRA_SPACE;
        final int height = images.getBackground().getHeight() + 2 * EXTRA_SPACE;
        final BufferedImage background = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        final Graphics2D g = background.createGraphics();
        drawCroppedBackground(images, g);
        for (final BufferedImage pieceImage : images.getPieces()) {
            final Piece piece = new Piece(pieceImage);
            final int pieceWidth = piece.getWidth();
            final int pieceHeight = piece.getHeight();
            final BitSet locations = getPossibleLocations(background, pieceWidth, pieceHeight);
            for (int y = 1; y < height - pieceHeight - 1; y++) {
                for (int x = 1; x < width - pieceWidth - 1; x++) {
                    if (locations.get(x + width * y)) {
                        piece.calculateDifferenceTo(background, x, y);
                    }
                }
            }
            final Point location = piece.getLocation();
            g.drawImage(pieceImage, location.x, location.y, null);
            location.translate(-EXTRA_SPACE, -EXTRA_SPACE);
            list.add(location);
        }
        g.dispose();
        return list;
    }

    private static BitSet getPossibleLocations(final BufferedImage background, final int pieceWidth, final int pieceHeight) {
        final int width = background.getWidth();
        final int height = background.getHeight();
        final BitSet locations = new BitSet(width * height);
        for (int y = 0; y < height; y++) {
            int n = pieceWidth;
            for (int x = width - 1; x >= 0; x--) {
                if ((background.getRGB(x, y) & 0xFFFFFF) != 0xFFFFFF) {
                    n = pieceWidth;
                } else {
                    if (n > 1) {
                        n--;
                    } else {
                        locations.set(x + width * y);
                        continue;
                    }
                }
                locations.clear(x + width * y);
            }
        }
        for (int x = 0; x < width; x++) {
            int n = pieceHeight;
            for (int y = height - 1; y >= 0; y--) {
                if (!locations.get(x + width * y)
                        || (background.getRGB(x, y) & 0xFFFFFF) != 0xFFFFFF) {
                    n = pieceHeight;
                } else {
                    if (n > 1) {
                        n--;
                    } else {
                        locations.set(x + width * y);
                        continue;
                    }
                }
                locations.clear(x + width * y);
            }
        }
        return locations;
    }

    private static void drawCroppedBackground(final KeyCaptchaImages images, final Graphics2D g) throws PluginImplementationException {
        int pieceWidth = 0;
        int pieceHeight = 0;
        for (final BufferedImage piece : images.getPieces()) {
            if (piece.getWidth() > pieceWidth) {
                pieceWidth = piece.getWidth();
            }
            if (piece.getHeight() > pieceHeight) {
                pieceHeight = piece.getHeight();
            }
        }
        final BufferedImage background = images.getBackground();
        final int width = background.getWidth();
        final int height = background.getHeight();
        int top = -1;
        int bottom = -1;
        int left = -1;
        int right = -1;
        outer:
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if ((background.getRGB(x, y) & 0xFFFFFF) != 0xFFFFFF) {
                    top = y;
                    break outer;
                }
            }
        }
        outer:
        for (int y = height - 1; y >= 0; y--) {
            for (int x = 0; x < width; x++) {
                if ((background.getRGB(x, y) & 0xFFFFFF) != 0xFFFFFF) {
                    bottom = y + 1;
                    break outer;
                }
            }
        }
        outer:
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if ((background.getRGB(x, y) & 0xFFFFFF) != 0xFFFFFF) {
                    left = x;
                    break outer;
                }
            }
        }
        outer:
        for (int x = width - 1; x >= 0; x--) {
            for (int y = 0; y < height; y++) {
                if ((background.getRGB(x, y) & 0xFFFFFF) != 0xFFFFFF) {
                    right = x + 1;
                    break outer;
                }
            }
        }
        if (top == -1 || bottom == -1 || left == -1 || right == -1) {
            throw new PluginImplementationException("Empty background image");
        }
        final int x1 = Math.max(left - pieceWidth, 0);
        final int y1 = Math.max(top - pieceHeight, 0);
        final int x2 = Math.min(right + pieceWidth, width);
        final int y2 = Math.min(bottom + pieceHeight, height);
        final int w = x2 - x1;
        final int h = y2 - y1;
        g.setColor(Color.WHITE);
        g.fillRect(x1, y1, w + 2 * EXTRA_SPACE, h + 2 * EXTRA_SPACE);
        g.drawImage(background, x1 + EXTRA_SPACE, y1 + EXTRA_SPACE, x2 + EXTRA_SPACE, y2 + EXTRA_SPACE, x1, y1, x2, y2, null);
    }

    private static class Piece {

        private final int width, height;
        private final int[] edgeRgb;

        private int x, y;
        private int difference = Integer.MAX_VALUE;

        public Piece(final BufferedImage image) {
            width = image.getWidth();
            height = image.getHeight();
            edgeRgb = new int[2 * (width + height)];
            image.getRGB(0, 0, width, 1, edgeRgb, 0, width);
            image.getRGB(0, height - 1, width, 1, edgeRgb, width, width);
            image.getRGB(0, 0, 1, height, edgeRgb, width + width, 1);
            image.getRGB(width - 1, 0, 1, height, edgeRgb, width + width + height, 1);
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }

        public Point getLocation() {
            return new Point(x, y);
        }

        public void calculateDifferenceTo(final BufferedImage background, final int x, final int y) {
            final int[] backgroundEdgeRgb = new int[2 * (width + height)];
            background.getRGB(x, y - 1, width, 1, backgroundEdgeRgb, 0, width);
            background.getRGB(x, y + height, width, 1, backgroundEdgeRgb, width, width);
            background.getRGB(x - 1, y, 1, height, backgroundEdgeRgb, width + width, 1);
            background.getRGB(x + width, y, 1, height, backgroundEdgeRgb, width + width + height, 1);

            int difference = 0;
            for (int i = 0; i < backgroundEdgeRgb.length; i++) {
                difference += difference(edgeRgb[i], backgroundEdgeRgb[i]);
            }

            if (this.difference > difference) {
                this.difference = difference;
                this.x = x;
                this.y = y;
            }
        }

        private static int difference(final int a, final int b) {
            int difference = 0;
            difference += Math.abs(red(a) - red(b));
            difference += Math.abs(green(a) - green(b));
            difference += Math.abs(blue(a) - blue(b));
            return difference;
        }

        private static int red(final int rgb) {
            return (rgb >> 16) & 0xFF;
        }

        private static int green(final int rgb) {
            return (rgb >> 8) & 0xFF;
        }

        private static int blue(final int rgb) {
            return (rgb) & 0xFF;
        }

    }

    private KeyCaptchaRecognizer() {
    }

}
