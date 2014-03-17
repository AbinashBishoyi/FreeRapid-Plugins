package cz.vity.freerapid.plugins.services.picscrazy;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.logging.Logger;

/**
 * Class which contains main code
 *
 * @author tong2shot
 * @since 0.9u2
 */
class PicsCrazyFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(PicsCrazyFileRunner.class.getName());

    private void checkNameAndSize() throws URISyntaxException, UnsupportedEncodingException {
        final String path = new URI(fileURL).getPath();
        final String fileName = URLDecoder.decode(path.substring(path.lastIndexOf("/") + 1), "UTF-8");
        httpFile.setFileName(fileName);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        checkNameAndSize();
        final GetMethod method = getGetMethod(fileURL);
        if (isPageView()) { //page view
            if (makeRedirectedRequest(method)) {
                checkProblems();
                final HttpMethod httpMethod = getMethodBuilder()
                        .setReferer(fileURL)
                        .setAction(PlugUtils.getStringBetween(getContentAsString(),"<div id=\"content\"><img src=\"","\""))
                        .toHttpMethod();
                if (!tryDownloadAndSaveFile(httpMethod)) {
                    checkProblems();
                    throw new ServiceConnectionProblemException("Error starting download");
                }
            } else {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
        } else { //direct image
            if (!tryDownloadAndSaveFile(method)) {
                checkProblems();
                throw new ServiceConnectionProblemException("Error starting download");
            }
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("file not found")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    private boolean isPageView() {
        return fileURL.contains("/v/");
    }

}