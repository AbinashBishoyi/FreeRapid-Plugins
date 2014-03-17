package cz.vity.freerapid.plugins.services.ddlstorage;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class which contains main code
 *
 * @author tong2shot
 */
class DdlStorageFileRunner extends XFileSharingRunner {

    @Override
    protected int getWaitTime() throws Exception {
        final Matcher matcher = getMatcherAgainstContent("id=\"countdown_str\".*?\\s*.*?<h2.*?>.*?(\\d+).*?</h2");
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1)) + 1;
        }
        return 0;
    }

    @Override
    protected boolean stepCaptcha(final MethodBuilder methodBuilder) throws Exception {
        super.stepCaptcha(methodBuilder);
        return false; // unskip waiting time
    }

    @Override
    protected List<String> getDownloadLinkRegexes() {
        final List<String> downloadLinkRegexes = super.getDownloadLinkRegexes();
        downloadLinkRegexes.add(0, "parent\\.location='(http.+?" + Pattern.quote(httpFile.getFileName()) + ")'");
        return downloadLinkRegexes;
    }

    @Override
    protected void checkFileProblems() throws ErrorDuringDownloadingException {
        if (getContentAsString().contains("temporarily unavailable from your region")) {
            throw new PluginImplementationException("DDLStorage temporarily unavailable from your region");
        }
        super.checkFileProblems();
    }
}