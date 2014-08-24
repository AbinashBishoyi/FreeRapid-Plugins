package cz.vity.freerapid.plugins.services.videomega;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.net.URLDecoder;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class VideoMegaFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(VideoMegaFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final Matcher match = PlugUtils.matcher("ref=([\\d\\w]+)", fileURL);
        if (!match.find())
            throw new NotRecoverableDownloadException("No video reference code in url");
        final GetMethod method = getGetMethod("http://videomega.tv/iframe.php?ref=" + match.group(1)); //create GET request
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize(getContentAsString());
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, content, "<title>VideoMega.tv - ", "</title>");
        httpFile.setFileName(httpFile.getFileName() + ".mp4");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final Matcher match = PlugUtils.matcher("ref=([\\d\\w]+)", fileURL);
        if (!match.find())
            throw new NotRecoverableDownloadException("No video reference code in url");
        final GetMethod method = getGetMethod("http://videomega.tv/iframe.php?ref=" + match.group(1)); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            final String contentAsString = getContentAsString();//check for response
            checkProblems();//check problems
            checkNameAndSize(contentAsString);//extract file name and size from the page

            final Matcher matchC = PlugUtils.matcher("\\{\\s*?document\\.write\\(unescape\\(\"(.+?)\"\\)\\)", contentAsString);
            if (!matchC.find())
                throw new PluginImplementationException("Encoded page content not found");

            final Matcher matchU = PlugUtils.matcher(",\\s*?file\\s*?:\\s*?\"([^\"]+?)\"", URLDecoder.decode(matchC.group(1), "UTF-8"));
            if (!matchU.find()) {
                logger.warning(URLDecoder.decode(matchC.group(1), "UTF-8"));
                throw new PluginImplementationException("Download url not found");
            }
            final HttpMethod httpMethod = getGetMethod(matchU.group(1));
            if (!tryDownloadAndSaveFile(httpMethod)) {
                checkProblems();//if downloading failed
                throw new ServiceConnectionProblemException("Error starting download");//some unknown problem
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("<title>VideoMega.tv - Disable adblock</title>")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }

}