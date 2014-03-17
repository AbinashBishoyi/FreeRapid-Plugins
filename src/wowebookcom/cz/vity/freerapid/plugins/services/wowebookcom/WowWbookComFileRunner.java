package cz.vity.freerapid.plugins.services.wowebookcom;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;

import java.net.URL;
import java.util.logging.Logger;

/**
 * Class which contains main code
 *
 * @author CrazyCoder
 */
class WowWbookComFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(WowWbookComFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();

            final String code = PlugUtils.getStringBetween(getContentAsString(), "download/", "/");
            final String directURL = "http://www.wowebook.be/download/" + code + "/";

            final HttpMethod get = getMethodBuilder().setBaseURL(directURL).toGetMethod();

            if (client.makeRequest(get, false) == HttpStatus.SC_MOVED_TEMPORARILY) {
                final Header hLocation = get.getResponseHeader("Location");
                if (hLocation != null) {
                    httpFile.setNewURL(new URL(hLocation.getValue()));
                    httpFile.setPluginID("");
                    httpFile.setState(DownloadState.QUEUED);
                }
            } else {
                throw new ServiceConnectionProblemException();
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("File Not Found")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }
}
