package cz.vity.freerapid.plugins.services.uploadrocket;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class UploadRocketFileRunner extends XFileSharingRunner {

    @Override
    protected List<String> getDownloadLinkRegexes() {
        final List<String> downloadLinkRegexes = super.getDownloadLinkRegexes();
        downloadLinkRegexes.add("var download_url\\s*=\\s*'(http.+?" + Pattern.quote(httpFile.getFileName()) + ")'");
        return downloadLinkRegexes;
    }

    @Override
    protected boolean handleDirectDownload(final HttpMethod method) throws Exception {
        fileURL = method.getResponseHeader("Location").getValue();
        if (!makeRedirectedRequest(redirectToLocation(method))) {
            checkFileProblems();
            throw new ServiceConnectionProblemException();
        }
        return false;
    }

    @Override
    protected void checkFileProblems() throws ErrorDuringDownloadingException {
        String contentAsString = getContentAsString();
        try {
            if (contentAsString.contains("file was deleted by") ||
                    contentAsString.contains("Reason for deletion")) {
                throw new URLNotAvailableAnymoreException("File not found");
            }
            super.checkFileProblems();
        } catch (URLNotAvailableAnymoreException e) {
            final Matcher match = PlugUtils.matcher("(visibility:hidden|font-size:0).+?>(File Not Found|Reason for deletion)", contentAsString);
            if (!match.find())  throw e;
        }
    }
}