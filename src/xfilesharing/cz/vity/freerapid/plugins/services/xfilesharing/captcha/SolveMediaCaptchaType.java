package cz.vity.freerapid.plugins.services.xfilesharing.captcha;

import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.services.solvemediacaptcha.SolveMediaCaptcha;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpDownloadClient;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;

import java.util.regex.Matcher;

/**
 * @author birchie
 */
public class SolveMediaCaptchaType implements CaptchaType {

    protected String getReCaptchaKeyRegex() {
        return "src=\".+?solvemedia\\.com/papi/challenge\\.noscript\\?k=(.+?)\"";
    }

    @Override
    public boolean canHandle(final String content) {
        return PlugUtils.find(getReCaptchaKeyRegex(), content);
    }

    @Override
    public void handleCaptcha(final MethodBuilder methodBuilder, final HttpDownloadClient client, final CaptchaSupport captchaSupport) throws Exception {
        final Matcher captchaKeyMatcher = PlugUtils.matcher(getReCaptchaKeyRegex(), client.getContentAsString());
        if (!captchaKeyMatcher.find()) {
            throw new PluginImplementationException("Captcha key not found");
        }
        final String captchaKey = captchaKeyMatcher.group(1);
        final SolveMediaCaptcha solveMediaCaptcha = new SolveMediaCaptcha(captchaKey, client, captchaSupport);
        solveMediaCaptcha.askForCaptcha();
        solveMediaCaptcha.modifyResponseMethod(methodBuilder);
    }

}
