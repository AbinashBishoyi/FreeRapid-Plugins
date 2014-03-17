package cz.vity.freerapid.plugins.services.circlecaptcha;

import cz.vity.freerapid.plugins.exceptions.CaptchaEntryInputMismatchException;
import cz.vity.freerapid.plugins.webclient.interfaces.DialogSupport;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.util.logging.Logger;

/**
 * @author ntoskrnl
 */
public class CircleCaptcha {

    private final static Logger logger = Logger.getLogger(CircleCaptcha.class.getName());

    private static volatile Object captchaLock = null;
    private final DialogSupport dialogSupport;

    private final int minRadius, maxRadius;
    private final int backgroundColor;
    private final double threshold;

    private final static int CAPTCHA_MAX = 5;
    private int captchaCounter = 0;

    public CircleCaptcha(DialogSupport dialogSupport, int minRadius, int maxRadius, int backgroundColor, double threshold) {
        this.dialogSupport = dialogSupport;
        this.minRadius = minRadius;
        this.maxRadius = maxRadius;
        this.backgroundColor = backgroundColor;
        this.threshold = threshold;
    }

    public Point recognize(final BufferedImage image) throws Exception {
        if (captchaCounter < CAPTCHA_MAX) {
            captchaCounter++;
            final Point result = autoRecognize(image);
            logger.info("Automatic recognition attempt " + captchaCounter + " of " + CAPTCHA_MAX + ": " + result);
            return result;
        } else {
            final Point result = showClickLocationDialog(image, "Please click on the open circle", dialogSupport);
            logger.info("Manual recognition: " + result);
            return result;
        }
    }

    private Point autoRecognize(final BufferedImage image) {
        final CircleHoughTransform cht = new CircleHoughTransform(image, backgroundColor, minRadius, maxRadius, 1);
        cht.performHoughTransform();
        final Circle circle = cht.findOpenCircle(threshold);
        if (circle != null) {
            return new Point(circle.x(), circle.y());
        } else {
            return null;
        }
    }

    public static Point showClickLocationDialog(final BufferedImage image, final String message, final DialogSupport dialogSupport) throws Exception {
        final ClickLocationCaptchaPanel panel = new ClickLocationCaptchaPanel(image, message);
        setCaptchaLock(dialogSupport);
        synchronized (captchaLock) {
            if (!dialogSupport.showOKCancelDialog(panel, "Captcha")) {
                throw new CaptchaEntryInputMismatchException();
            }
        }
        return panel.getClickLocation();
    }

    private static synchronized void setCaptchaLock(final DialogSupport dialogSupport) {
        if (captchaLock == null) {
            try {
                final Field field = dialogSupport.getClass().getDeclaredField("captchaLock");
                field.setAccessible(true);
                captchaLock = field.get(null);
            } catch (final Exception e) {
                //ignore
            }
            if (captchaLock == null) {
                captchaLock = new Object();
            }
        }
    }

}
