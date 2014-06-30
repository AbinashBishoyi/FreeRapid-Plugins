package cz.vity.freerapid.plugins.services.imageshack;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadClientConsts;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.net.URLDecoder;
import java.util.logging.Logger;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 */
class ImageShackFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(ImageShackFileRunner.class.getName());

    private void checkProblems() throws ErrorDuringDownloadingException {
        if (getContentAsString().contains("404 - Not Found")
                || getContentAsString().contains("<h1>404 Not Found</h1>")
                || getContentAsString().contains("Error 404 while fetching image source")
                || getContentAsString().contains("Welcome to ImageShack")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            final HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL).setActionFromAHrefWhereATagContains("Download").toGetMethod();
            final String path = httpMethod.getURI().getPath();
            final String filename = URLDecoder.decode(path.substring(path.lastIndexOf("/") + 1), "UTF-8");
            httpFile.setFileName(filename);
            setClientParameter(DownloadClientConsts.NO_CONTENT_LENGTH_AVAILABLE, true);
            if (!tryDownloadAndSaveFile(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException("Error starting download");
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

}