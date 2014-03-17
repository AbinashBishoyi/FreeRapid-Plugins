package cz.vity.freerapid.plugins.services.streamcz;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author Ladislav Vitasek
 * @author Ludek Zika
 * @author ntoskrnl
 */
class StreamCzRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(StreamCzRunner.class.getName());

    public void runCheck() throws Exception {
        super.runCheck();
        final HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkName();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkName();
            method = getGetMethod("http://cdn-dispatcher.stream.cz/?id=" + getId());
            if (!tryDownloadAndSaveFile(method)) {
                checkProblems();
                throw new ServiceConnectionProblemException("Error starting download");
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private String getId() throws ErrorDuringDownloadingException {
        Matcher matcher = getMatcherAgainstContent("cdnHD=(\\d+)");
        if (!matcher.find()) {
            matcher = getMatcherAgainstContent("cdnHQ=(\\d+)");
            if (!matcher.find()) {
                matcher = getMatcherAgainstContent("cdnLQ=(\\d+)");
                if (!matcher.find()) {
                    throw new PluginImplementationException("Video ID not found");
                }
            }
        }
        return matcher.group(1);
    }

    private void checkName() throws Exception {
        final String name = PlugUtils.getStringBetween(getContentAsString(), "<meta name=\"title\" content=\"", "- Video na Stream.cz\"");
        httpFile.setFileName(name + ".flv");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        if (getContentAsString().contains("Stránku nebylo možné nalézt")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

}