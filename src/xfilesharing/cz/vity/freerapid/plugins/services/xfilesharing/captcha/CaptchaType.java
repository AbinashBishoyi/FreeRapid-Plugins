package cz.vity.freerapid.plugins.services.xfilesharing.captcha;

import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpDownloadClient;

/**
 * @author ntoskrnl
 */
public interface CaptchaType {

    public boolean canHandle(final String content);

    public void handleCaptcha(final MethodBuilder methodBuilder, final HttpDownloadClient client, final CaptchaSupport captchaSupport) throws Exception;

}
