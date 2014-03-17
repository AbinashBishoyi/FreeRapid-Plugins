package cz.vity.freerapid.plugins.services.relink.captcha;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * @author ntoskrnl
 */
public class CaptchaPreparer {

    public static BufferedImage getPreparedImage(final BufferedImage image) {
        final int w = image.getWidth() - 2;
        final int h = image.getHeight() - 2;
        final BufferedImage input = image.getSubimage(1, 1, w, h);
        final BufferedImage output = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        final Graphics2D g = output.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, w, h);
        g.dispose();
        for (int y = 1; y < h - 1; y++) {
            for (int x = 1; x < w - 1; x++) {
                if (!isWhite(input.getRGB(x, y))
                        || !isWhite(input.getRGB(x, y - 1))
                        || !isWhite(input.getRGB(x - 1, y))
                        || !isWhite(input.getRGB(x, y + 1))
                        || !isWhite(input.getRGB(x + 1, y))) {
                    output.setRGB(x, y, 0);
                }
            }
        }
        return output;
    }

    private static boolean isWhite(final int i) {
        return (i & 0xFFFFFF) == 0xFFFFFF;
    }

}
