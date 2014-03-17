package cz.vity.freerapid.plugins.services.adf;

import cz.vity.freerapid.plugins.exceptions.BuildMethodException;
import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;

import java.io.IOException;
import java.net.URL;
import java.util.logging.Logger;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 */
class AdfFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(AdfFileRunner.class.getName());

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            final String url = PlugUtils.getStringBetween(getContentAsString(), "var url = '", "';");

            final URL root = new URL(fileURL);
            final String prefix = root.getProtocol() + "://" + root.getHost();
            final String goLink = prefix + url;
            logger.info("Go link: " + goLink);

            downloadTask.sleep(5);

            final String location = getRedirectedLink(goLink);

            httpFile.setNewURL(new URL(location));
            httpFile.setPluginID("");
            httpFile.setState(DownloadState.QUEUED);
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private String getRedirectedLink(String link) throws BuildMethodException, IOException, ServiceConnectionProblemException {
        final MethodBuilder methodBuilder = getMethodBuilder().setBaseURL(link);
        final HttpMethod get = methodBuilder.toHttpMethod();
        int code = client.makeRequest(get, false);

        logger.info("Response: " + code);

        if (code != HttpStatus.SC_MOVED_TEMPORARILY) {
            throw new ServiceConnectionProblemException("Error following link");
        }

        final Header hLocation = get.getResponseHeader("Location");

        if (hLocation == null) {
            throw new ServiceConnectionProblemException("Error following link");
        }

        final String location = hLocation.getValue();
        logger.info("Location: " + location);
        return location;
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        if (getContentAsString().contains("the page you are looking for does not exist")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }
}
