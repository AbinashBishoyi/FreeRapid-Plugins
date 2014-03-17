package cz.vity.freerapid.plugins.services.shareflare;

import cz.vity.freerapid.plugins.services.letitbit.LetitbitRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.Cookie;


/**
 * @author RickCL, ntoskrnl
 */
class ShareflareRunner extends LetitbitRunner {

    @Override
    protected void setLanguageCookie() {
        addCookie(new Cookie(".shareflare.net", "lang", "en", "/", 86400, false));
    }

    @Override
    protected void checkNameAndSize() throws Exception {
        PlugUtils.checkName(httpFile, getContentAsString(), "File: <span>", "</span>");
        PlugUtils.checkFileSize(httpFile, getContentAsString(), "[<span>", "</span>]");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

}