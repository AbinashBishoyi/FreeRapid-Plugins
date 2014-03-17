package cz.vity.freerapid.plugins.services.iperupload;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;

import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class IperUploadFileRunner extends XFileSharingRunner {

    @Override
    protected int getWaitTime() throws Exception {
        final Matcher matcher = getMatcherAgainstContent(">\\s*?(\\d+)\\s*?</h2> </p>");
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1)) + 1;
        }
        return 0;
    }

    @Override
    protected void checkDownloadProblems() throws ErrorDuringDownloadingException {
        if (getContentAsString().contains("Link expired")) {
            throw new ServiceConnectionProblemException("Link expired");
        }
        super.checkDownloadProblems();
    }

}