package cz.vity.freerapid.plugins.services.loadto;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.PlugUtils;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFileDownloader;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;

import java.io.IOException;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author Ladislav Vitasek, Ludek Zika
 */
class LoadToRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(LoadToRunner.class.getName());


    public void runCheck(HttpFileDownloader downloader) throws Exception {
        super.runCheck(downloader);

        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRequest(getMethod)) {
            checkNameAndSize(getContentAsString());
        } else
            throw new PluginImplementationException("Problem with a connection to service.\nCannot find requested page content");
    }


    public void run(HttpFileDownloader downloader) throws Exception {
        super.run(downloader);
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod getMethod = getGetMethod(fileURL);
        getMethod.setFollowRedirects(true);
        if (makeRequest(getMethod)) {
            checkNameAndSize(getContentAsString());
            downloader.sleep(5);
            Matcher matcher = PlugUtils.matcher("<form method=\"post\" action=\"(http[^\"]*)\"", getContentAsString());
            if (!matcher.find()) {
                checkProblems();
                throw new ServiceConnectionProblemException("Problem with a connection to service.\nCannot find requested page content");
            }
            final PostMethod method = getPostMethod(matcher.group(1));
            if (!tryDownload(method)) {
                checkProblems();
                logger.info(getContentAsString());
                throw new IOException("File input stream is empty.");
            }

        } else
            throw new PluginImplementationException("Problem with a connection to service.\nCannot find requested page content");
    }

    private void checkNameAndSize(String content) throws Exception {

        if (!content.contains("Load.to")) {
            logger.warning(getContentAsString());
            throw new InvalidURLOrServiceProblemException("Invalid URL or unindentified service");
        }

        Matcher matcher = PlugUtils.matcher("Can't find file.", content);
        if (matcher.find()) {
            throw new URLNotAvailableAnymoreException(String.format("<b>Can't find file. Please check URL.</b><br>"));
        }
        matcher = PlugUtils.matcher("<title>([^/]*) //", content);
        if (matcher.find()) {
            String fn = matcher.group(1);
            logger.info("File name " + fn);
            httpFile.setFileName(fn);

        }
        matcher = PlugUtils.matcher("([0-9.]+ Bytes)", content);
        if (matcher.find()) {
            Long a = PlugUtils.getFileSizeFromString(matcher.group(1));
            logger.info("File size " + a);
            httpFile.setFileSize(a);
        }
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);

    }

    private void checkProblems() throws ServiceConnectionProblemException, YouHaveToWaitException, URLNotAvailableAnymoreException {
        Matcher matcher;
        matcher = getMatcherAgainstContent("Can't find file.");
        if (matcher.find()) {
            throw new URLNotAvailableAnymoreException(String.format("<b>Can't find file. Please check URL.</b><br>"));
        }
    }

}