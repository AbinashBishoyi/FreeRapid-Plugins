package cz.vity.freerapid.plugins.services.o2musicstream;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.InvalidURLOrServiceProblemException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.HttpUtils;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;

import java.io.IOException;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author Kajda
 * @since 0.82
 */
class O2MusicStreamFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(O2MusicStreamFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final HttpMethod httpMethod = getMethodBuilder().setAction(fileURL).toHttpMethod();

        if (makeRedirectedRequest(httpMethod)) {
            checkSeriousProblems();
            checkName();
        } else {
            throw new InvalidURLOrServiceProblemException("Invalid URL or service problem");
        }
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        HttpMethod httpMethod = getMethodBuilder().setAction(fileURL).toHttpMethod();

        if (makeRedirectedRequest(httpMethod)) {
            checkAllProblems();
            checkName();
            client.getHTTPClient().getParams().setBooleanParameter("dontUseHeaderFilename", true);
            httpMethod = getMethodBuilder().setAction("http://cdn-dispatcher.stream.cz/?id=" + PlugUtils.getStringBetween(getContentAsString(), "'hdID=", "&'")).toHttpMethod();

            if (!tryDownloadAndSaveFile(httpMethod)) {
                checkAllProblems();
                logger.warning(getContentAsString());
                throw new IOException("File input stream is empty");
            }
        } else {
            throw new InvalidURLOrServiceProblemException("Invalid URL or service problem");
        }
    }

    private void checkSeriousProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();

        if (contentAsString.contains("404 nenalezeno")) {
            throw new URLNotAvailableAnymoreException("404 nenalezeno");
        }
    }

    private void checkAllProblems() throws ErrorDuringDownloadingException {
        checkSeriousProblems();
    }

    private void checkName() throws ErrorDuringDownloadingException {
        final Matcher matcher = getMatcherAgainstContent("<h1 class=\"main_title\">(.+)</h1>");

        if (matcher.find()) {
            final String fileName = matcher.group(1).trim() + ".mp4";
            logger.info("File name " + fileName);
            httpFile.setFileName(HttpUtils.replaceInvalidCharsForFileSystem(PlugUtils.unescapeHtml(fileName), "_"));
        } else {
            logger.warning("File name was not found");
            throw new PluginImplementationException();
        }

        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }
}