package cz.vity.freerapid.plugins.services.rajce;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
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
class RajceFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(RajceFileRunner.class.getName());


    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL); //create GET request
        if (fileURL.indexOf(".jpg") > 0) {
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
        //photos
        Matcher matcher = getMatcherAgainstContent("href=\"(.+?)\" class=\"photoThumb\"");
        int start = 0;
        final List<URI> uriList = new LinkedList<URI>();
        while (matcher.find(start)) {
            String link = matcher.group(1);
            try {
                uriList.add(new URI(link));
            } catch (URISyntaxException e) {
                LogUtils.processException(logger, e);
            }
            start = matcher.end();
        }

        //albums
        matcher = getMatcherAgainstContent("class=\"albumName\" href=\"(.+?)\"");
        start = 0;
        while (matcher.find(start)) {
            String link = matcher.group(1);
            try {
                uriList.add(new URI(link));
            } catch (URISyntaxException e) {
                LogUtils.processException(logger, e);
            }
            start = matcher.end();
        }

        getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, uriList);
    }


    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("Chyba 404")) {
            throw new URLNotAvailableAnymoreException("stránka nenalezena"); //let to know user in FRD
        }
    }

}