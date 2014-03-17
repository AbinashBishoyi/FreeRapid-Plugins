package cz.vity.freerapid.plugins.services.youtube;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.io.IOException;

/**
 * @author Kajda
 */
class YouTubeFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(YouTubeFileRunner.class.getName());
    private final static String SERVICE_WEB = "http://www.youtube.com";

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);

        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkName();
        } else {
            throw new InvalidURLOrServiceProblemException("Invalid URL or service problem");
        }
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        GetMethod getMethod = getGetMethod(fileURL);

        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkName();

            Matcher matcher = getMatcherAgainstContent("var fullscreenUrl = '(.+?)'");

            if (matcher.find()) {
                final String matcherAsString = matcher.group(1);

                matcher = PlugUtils.matcher("video_id=(.+?)&.+&t=(.+?)&", matcherAsString);

                if (matcher.find()) {
                    final String finalURL = SERVICE_WEB + "/get_video.php?video_id=" + matcher.group(1) + "&t=" + matcher.group(2);
                    client.setReferer(finalURL);
                    client.getHTTPClient().getParams().setBooleanParameter("dontUseHeaderFilename", true);
                    getMethod = getGetMethod(finalURL);

                    if (!tryDownloadAndSaveFile(getMethod)) {
                        checkProblems();
                        logger.warning(getContentAsString());
                        throw new IOException("File input stream is empty");
                    }
                } else {
                    throw new PluginImplementationException("Download parameters were not found");
                }
            } else {
                throw new PluginImplementationException();
            }
        } else {
            throw new InvalidURLOrServiceProblemException("Invalid URL or service problem");
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final Matcher matcher = getMatcherAgainstContent("class=\"errorBox\">((?:.|\\s)+?)</div");

        if (matcher.find()) {
            throw new URLNotAvailableAnymoreException(matcher.group(1));
        }
    }

    private void checkName() throws ErrorDuringDownloadingException {
        final Matcher matcher = getMatcherAgainstContent("<h1\\s?>(.+?)</h1>");

        if (matcher.find()) {
            final String fileName = matcher.group(1).trim() + ".flv";
            logger.info("File name " + fileName);
            httpFile.setFileName(fileName);
        } else {
            logger.warning("File name was not found");
            throw new PluginImplementationException();
        }

        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }
}