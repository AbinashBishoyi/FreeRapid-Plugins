package cz.vity.freerapid.plugins.services.keycaptcha;

import cz.vity.freerapid.plugins.exceptions.CaptchaEntryInputMismatchException;
import cz.vity.freerapid.plugins.webclient.DownloadClient;
import cz.vity.freerapid.plugins.webclient.interfaces.DialogSupport;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpDownloadClient;
import org.apache.commons.httpclient.HttpMethod;

import java.lang.reflect.Field;
import java.util.logging.Logger;

/**
 * @author ntoskrnl
 */
public class KeyCaptcha {

    private final static Logger logger = Logger.getLogger(KeyCaptcha.class.getName());

    private static Object captchaLock = null;
    private final DialogSupport dialogSupport;
    private final KeyCaptchaClient kc;

    public KeyCaptcha(final DialogSupport dialogSupport, final HttpDownloadClient client) {
        this.dialogSupport = dialogSupport;
        final HttpDownloadClient c = new DownloadClient();
        c.initClient(client.getSettings());
        this.kc = new KeyCaptchaClient(c);
    }

    public HttpMethod recognize(final String pageContent, final String pageUrl) throws Exception {
        final KeyCaptchaImages images = kc.getImages(pageContent, pageUrl);
        final KeyCaptchaPanel panel = new KeyCaptchaPanel(images);
        synchronized (getCaptchaLock(dialogSupport)) {
            if (!dialogSupport.showOKCancelDialog(panel, "Captcha")) {
                throw new CaptchaEntryInputMismatchException();
            }
        }
        return kc.getResult(panel.getPieceLocations(), panel.getMouseLocations());
    }

    private static synchronized Object getCaptchaLock(final DialogSupport dialogSupport) {
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
        return captchaLock;
    }

}
