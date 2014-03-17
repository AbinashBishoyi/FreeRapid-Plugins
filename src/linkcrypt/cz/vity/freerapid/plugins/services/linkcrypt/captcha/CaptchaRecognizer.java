package cz.vity.freerapid.plugins.services.linkcrypt.captcha;

import cz.vity.freerapid.plugins.services.circlecaptcha.Circle;
import cz.vity.freerapid.plugins.services.circlecaptcha.CircleHoughTransform;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author ntoskrnl
 */
public class CaptchaRecognizer {
    private final static Logger logger = Logger.getLogger(CaptchaRecognizer.class.getName());

    public static Point recognizeTextxCaptcha(final BufferedImage image) {
        final String problem = getProblem(image);
        if (problem != null) {
            logger.info("problem = " + problem);
            final String result = solve(problem);
            if (result != null) {
                logger.info("result = " + result);
                final Point point = findPoint(image, result);
                if (point != null) {
                    logger.info("point = " + point);
                    return point;
                } else {
                    logger.info("Point not found");
                }
            } else {
                logger.info("Cannot parse problem");
            }
        } else {
            logger.info("Cannot read problem");
        }
        return null;
    }

    private static String getProblem(final BufferedImage image) {
        final BufferedImage problem = getBinarySubimage(image, 200, 0, 75, 19);
        final String s = PlugUtils.recognize(problem, "-C 0-9x/+--");
        if (s != null) {
            return s.replaceAll("\\s+", "");
        } else {
            return null;
        }
    }

    private static BufferedImage getBinarySubimage(final BufferedImage source, final int x, final int y, final int w, final int h) {
        final BufferedImage result = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_BINARY);
        for (int b = 0; b < h; b++) {
            for (int a = 0; a < w; a++) {
                if ((source.getRGB(a + x, b + y) & 0xFFFFFF) != 0xFFFFFF) {
                    result.setRGB(a, b, 0);
                } else {
                    result.setRGB(a, b, 0xFFFFFF);
                }
            }
        }
        return result;
    }

    private static String solve(final String problem) {
        final Matcher matcher = PlugUtils.matcher("^(\\d+)([x/\\+\\-])(\\d+)$", problem);
        if (matcher.find()) {
            final int n = Integer.parseInt(matcher.group(1));
            final int m = Integer.parseInt(matcher.group(3));
            switch (matcher.group(2).charAt(0)) {
                case 'x':
                    return String.valueOf(n * m);
                case '/':
                    return String.valueOf(n / m);
                case '+':
                    return String.valueOf(n + m);
                case '-':
                    return String.valueOf(n - m);
            }
        }
        return null;
    }

    private static Point findPoint(final BufferedImage image, final String result) {
        final CircleHoughTransform cht = new CircleHoughTransform(image, 0xFFFFFF, 14, 26, 1);
        cht.performHoughTransform();
        final List<Circle> circles = cht.findCircles(0.7);
        if (circles.size() < 20) {
            for (final Circle circle : circles) {
                final String content = getCircleContent(image, circle);
                if (result.equals(content)) {
                    return new Point(circle.x(), circle.y());
                }
            }
        }
        return null;
    }

    private static String getCircleContent(final BufferedImage image, final Circle circle) {
        final int myR = (int) Math.round(circle.r() / Math.sqrt(2));
        final int x = Math.max(0, circle.x() - myR + 1);
        final int y = Math.max(0, circle.y() - myR + 1);
        final int w = Math.min(image.getWidth() - x, 2 * myR + 1 - 2);
        final int h = Math.min(image.getHeight() - y, 2 * myR + 1 - 2);
        final BufferedImage contentImage = getBinarySubimage(image, x, y, w, h);
        final String s = PlugUtils.recognize(contentImage, "-C 0-9--");
        if (s != null) {
            return s.replaceAll("\\s+", "");
        } else {
            return null;
        }
    }

}
