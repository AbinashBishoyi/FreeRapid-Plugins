package cz.vity.freerapid.plugins.services.cramit;

import cz.vity.freerapid.plugins.exceptions.CaptchaEntryInputMismatchException;
import cz.vity.freerapid.plugins.services.xfilesharing.captcha.CaptchaType;
import cz.vity.freerapid.plugins.services.xfilesharing.captcha.CaptchasCaptchaType;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpDownloadClient;

import java.awt.image.BufferedImage;
import java.util.logging.Logger;

public class CramitCaptchaType implements CaptchaType {
    private final static Logger logger = Logger.getLogger(CaptchasCaptchaType.class.getName());
    private final static int CAPTCHA_MAX = 5;
    private int captchaCounter = 0;

    @Override
    public boolean canHandle(final String content) {
        return content.contains("/captchas/");
    }

    @Override
    public void handleCaptcha(final MethodBuilder methodBuilder, final HttpDownloadClient client, final CaptchaSupport captchaSupport) throws Exception {
        final String captchaUrl = new MethodBuilder(client)
                .setActionFromImgSrcWhereTagContains("/captchas/")
                .getEscapedURI();
        String captcha;
        if (captchaCounter < CAPTCHA_MAX) {
            captchaCounter++;
            final BufferedImage captchaImage = captchaSupport.getCaptchaImage(captchaUrl);
            captcha = new CaptchaRecognizer().recognize(captchaImage);
            logger.info("OCR attempt " + captchaCounter + " of " + CAPTCHA_MAX + ", recognized " + captcha);
        } else {
            captcha = captchaSupport.getCaptcha(captchaUrl);
            if (captcha == null) {
                throw new CaptchaEntryInputMismatchException();
            }
        }
        methodBuilder.setParameter("code", captcha);
    }
}
