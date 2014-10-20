package cz.vity.freerapid.plugins.services.filestorm;

import cz.vity.freerapid.plugins.services.xfilesharing.captcha.FourTokensCaptchaType;

/**
 * @author birchie
 */
public class FourTokensCaptchaType2 extends FourTokensCaptchaType {

    protected String getFourTokensCaptchaRegex() {
        return "position:absolute;padding\\-left:(\\d+)px.*?>(.+?)</";
    }
}
