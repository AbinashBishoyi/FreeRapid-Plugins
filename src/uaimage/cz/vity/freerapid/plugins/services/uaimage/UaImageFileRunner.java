package cz.vity.freerapid.plugins.services.uaimage;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import org.apache.commons.httpclient.Cookie;
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
class UaImageFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(UaImageFileRunner.class.getName());

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        addCookie(new Cookie(".uaimage.com", "lang", "en", "/", 86400, false));
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
        if (contentAsString.contains("Изображение отсутствует в базе")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

}