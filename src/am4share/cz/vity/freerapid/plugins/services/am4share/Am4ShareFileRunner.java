package cz.vity.freerapid.plugins.services.am4share;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class Am4ShareFileRunner extends XFileSharingRunner {
    @Override
    protected void setLanguageCookie() throws Exception {
        super.setLanguageCookie();
        if (fileURL.contains("http://am4share.com"))
            fileURL = fileURL.replaceFirst("http://am4share.com", "http://amshare.co");
    }
}