package cz.vity.freerapid.plugins.services.fileparadox;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;
import cz.vity.freerapid.plugins.services.xfilesharing.captcha.CaptchaType;

import java.util.List;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class FileParadoxFileRunner extends XFileSharingRunner {

    @Override
    protected void setLanguageCookie() throws Exception {
        if (fileURL.startsWith("http:"))
            fileURL = fileURL.replaceFirst("http:", "https:");
        super.setLanguageCookie();
    }

    @Override
    protected List<CaptchaType> getCaptchaTypes() {
        final List<CaptchaType> captchaTypes = super.getCaptchaTypes();
        captchaTypes.add(0, new SolveMediaCaptchaType());
        return captchaTypes;
    }

    @Override
    protected int getWaitTime() throws Exception {
        final Matcher matcher = getMatcherAgainstContent("id=\"countno\".*?<span id=\".*?\">.*?(\\d+).*?</span");
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1)) + 1;
        }
        return 0;
    }


}