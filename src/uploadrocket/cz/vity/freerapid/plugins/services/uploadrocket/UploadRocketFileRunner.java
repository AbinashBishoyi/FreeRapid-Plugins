package cz.vity.freerapid.plugins.services.uploadrocket;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;

import java.util.List;
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
    protected void checkFileProblems() throws ErrorDuringDownloadingException {
        String contentAsString = getContentAsString();
        if (contentAsString.contains("file was deleted by") ||
                contentAsString.contains("Reason for deletion")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }
}