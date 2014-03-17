package cz.vity.freerapid.plugins.services.imagedoza;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadClientConsts;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.net.URLDecoder;
import java.util.logging.Logger;

/**
 * Class which contains main code
 *
 * @author tong2shot
 * @since 0.9u2
 */
class ImageDozaFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(ImageDozaFileRunner.class.getName());

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            final HttpMethod httpMethod;
            try {
                httpMethod = getMethodBuilder().setReferer(fileURL).setActionFromImgSrcWhereTagContains("id=\"im\"").toHttpMethod();
            } catch (BuildMethodException e) {
                throw new PluginImplementationException("Image url not found");
            }
            String filename = URLDecoder.decode(httpMethod.getPath().substring(httpMethod.getPath().lastIndexOf("/") + 1), "UTF-8");
            if (!filename.contains(".")) filename += ".jpg";
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

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("File Not Found")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

}