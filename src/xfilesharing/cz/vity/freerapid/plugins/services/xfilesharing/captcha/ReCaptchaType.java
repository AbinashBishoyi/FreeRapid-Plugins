package cz.vity.freerapid.plugins.services.xfilesharing.captcha;

import cz.vity.freerapid.plugins.exceptions.CaptchaEntryInputMismatchException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.services.recaptcha.ReCaptcha;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpDownloadClient;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;

import java.util.regex.Matcher;

/**
 * @author tong2shot
 * @author ntoskrnl
 */
public class ReCaptchaType implements CaptchaType {

    protected String getReCaptchaKeyRegex() {
        return "recaptcha/api/challenge\\?k=(.+?)\"";
    }

    @Override
    public boolean canHandle(final String content) {
        return PlugUtils.find(getReCaptchaKeyRegex(), content);
    }

    @Override
    public void handleCaptcha(final MethodBuilder methodBuilder, final HttpDownloadClient client, final CaptchaSupport captchaSupport) throws Exception {
        final Matcher reCaptchaKeyMatcher = PlugUtils.matcher(getReCaptchaKeyRegex(), client.getContentAsString());
        if (!reCaptchaKeyMatcher.find()) {
            throw new PluginImplementationException("ReCaptcha key not found");
        }
        final String reCaptchaKey = reCaptchaKeyMatcher.group(1);
        final ReCaptcha r = new ReCaptcha(reCaptchaKey, client);
        final String captcha = captchaSupport.getCaptcha(r.getImageURL());
        if (captcha == null) {
            throw new CaptchaEntryInputMismatchException();
        }
        r.setRecognized(captcha);
        r.modifyResponseMethod(methodBuilder);
    }

}
