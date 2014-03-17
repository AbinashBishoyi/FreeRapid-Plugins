package cz.vity.freerapid.plugins.services.keycaptcha;

import cz.vity.freerapid.plugins.exceptions.CaptchaEntryInputMismatchException;
import cz.vity.freerapid.plugins.webclient.interfaces.DialogSupport;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpDownloadClient;
import org.apache.commons.httpclient.HttpMethod;

import java.awt.*;
import java.lang.reflect.Field;
import java.util.List;
import java.util.logging.Logger;

/**
 * @author ntoskrnl
 */
public class KeyCaptcha {

    private final static Logger logger = Logger.getLogger(KeyCaptcha.class.getName());

    private static volatile Object captchaLock = null;
    private final DialogSupport dialogSupport;
    private final KeyCaptchaClient kc;

    private final static int CAPTCHA_MAX = 0;//recognition is broken
    private int captchaCounter = 0;

    public KeyCaptcha(final DialogSupport dialogSupport, final HttpDownloadClient client) {
        this.dialogSupport = dialogSupport;
        this.kc = new KeyCaptchaClient(client);
    }

    public HttpMethod recognize(final String pageContent, final String pageUrl) throws Exception {
        final KeyCaptchaImages images = kc.getImages(pageContent, pageUrl);
        final List<Point> result;
        if (captchaCounter < CAPTCHA_MAX) {
            captchaCounter++;
            logger.info("Recognizing...");
            final long startTime = System.currentTimeMillis();

            result = KeyCaptchaRecognizer.recognize(images);

            final long elapsedTime = System.currentTimeMillis() - startTime;
            logger.info("Automatic recognition attempt " + captchaCounter + " of " + CAPTCHA_MAX + " (" + elapsedTime + " ms): " + result);
        } else {
            result = showKeyCaptchaDialog(images);
            logger.info("Manual recognition: " + result);
        }
        return kc.getResult(result);
    }

    private List<Point> showKeyCaptchaDialog(final KeyCaptchaImages images) throws Exception {
        final KeyCaptchaPanel panel = new KeyCaptchaPanel(images);
        setCaptchaLock(dialogSupport);
        synchronized (captchaLock) {
            if (!dialogSupport.showOKCancelDialog(panel, "Captcha")) {
                throw new CaptchaEntryInputMismatchException();
            }
        }
        return panel.getPieceLocations();
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
