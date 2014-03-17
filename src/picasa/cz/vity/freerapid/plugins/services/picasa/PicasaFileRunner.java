package cz.vity.freerapid.plugins.services.picasa;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.YouHaveToWaitException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.utilities.LogUtils;
import org.apache.commons.httpclient.methods.GetMethod;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author Vity
 */
class PicasaFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(PicasaFileRunner.class.getName());

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL); //create GET request
        if (fileURL.indexOf("ggpht.com") > 0) {
            if (!tryDownloadAndSaveFile(method)) {
                checkProblems();
                logger.warning(getContentAsString());
                throw new IOException("File input stream is empty");
            }
        } else {
            if (makeRedirectedRequest(method)) { //we make the main request
                parseWebsite();
            } else {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
        }
    }

    private void parseWebsite() {
        final Matcher matcher = getMatcherAgainstContent("\"media\":\\{\"content\":\\[\\{\"url\":\"(http://.+?)\"");
        int start = 0;
        final List<URI> uriList = new LinkedList<URI>();
        while (matcher.find(start)) {
            String link = matcher.group(1);
            try {
                final int i = link.lastIndexOf('/');
                if (i > 0) {
                    StringBuilder builder = new StringBuilder(link);
                    builder.insert(i + 1, "d/");
                    link = builder.toString();
                    uriList.add(new URI(link));
                }
            } catch (URISyntaxException e) {
                LogUtils.processException(logger, e);
            }
            start = matcher.end();
        }
        getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, uriList);
    }


    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("The server encountered an error and could not complete your request")) {
            throw new YouHaveToWaitException("The server encountered an error and could not complete your request", 10); //let to know user in FRD
        }
    }

}