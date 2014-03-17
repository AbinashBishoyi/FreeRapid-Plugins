package cz.vity.freerapid.plugins.services.sharenxs;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.net.URLDecoder;
import java.util.logging.Logger;

/**
 * Class which contains main code
 *
 * @author tong2shot
 */
class ShareNXSFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(ShareNXSFileRunner.class.getName());

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            if (getContentAsString().contains("Click here to continue to image")) {
                if (!makeRedirectedRequest(method)) {
                    checkProblems();
                    throw new ServiceConnectionProblemException();
                }
            }
            checkProblems();

            HttpMethod httpMethod;
            fileURL = method.getURI().toString();
            if (!fileURL.endsWith("original")) {
                try {
                    httpMethod = getMethodBuilder().setReferer(fileURL).setActionFromAHrefWhereATagContains("original").toGetMethod();
                } catch (BuildMethodException e) {
                    throw new PluginImplementationException("Unable to get original image");
                }
                if (!makeRedirectedRequest(httpMethod)) {
                    checkProblems();
                    throw new ServiceConnectionProblemException();
                }
                checkProblems();
                fileURL = httpMethod.getURI().toString();
            }


            try {
                httpMethod = getMethodBuilder().setReferer(fileURL).setActionFromImgSrcWhereTagContains("/images/").toGetMethod();
            } catch (BuildMethodException e) {
                throw new PluginImplementationException("Image not found");
            }
            String path = httpMethod.getPath();
            String filename = URLDecoder.decode(path.substring(path.lastIndexOf("/") + 1), "UTF-8");
            if (!filename.contains(".")) {
                filename += ".jpg";
            }
            httpFile.setFileName(filename);
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
        if (contentAsString.contains("File not found")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

}