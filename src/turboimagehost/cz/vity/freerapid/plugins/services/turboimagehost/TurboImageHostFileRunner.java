package cz.vity.freerapid.plugins.services.turboimagehost;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;

/**
 * Class which contains main code
 *
 * @author tong2shot
 * @since 0.9u4
 */
class TurboImageHostFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(TurboImageHostFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            MethodBuilder imageMethodBuilder = getImageMethodBuilder(getContentAsString());
            checkNameAndSize(imageMethodBuilder.getEscapedURI());
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize(String imageUrl) throws ErrorDuringDownloadingException {
        String fname;
        try {
            fname = PlugUtils.suggestFilename(imageUrl);
        } catch (PluginImplementationException e) {
            throw new PluginImplementationException("File name not found");
        }
        logger.info("File name: " + fname);
        httpFile.setFileName(fname);
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            MethodBuilder imageMethodBuilder = getImageMethodBuilder(getContentAsString());
            checkNameAndSize(imageMethodBuilder.getEscapedURI());
            final HttpMethod httpMethod = imageMethodBuilder.toHttpMethod();
            if (!tryDownloadAndSaveFile(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException("Error starting download");
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private MethodBuilder getImageMethodBuilder(String content) throws Exception {
        MethodBuilder ret;
        try {
            ret = getMethodBuilder(content).setActionFromImgSrcWhereTagContains("id=\"imageid\"");
        } catch (BuildMethodException e) {
            throw new PluginImplementationException("Image URL not found");
        }
        return ret;
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("don`t exist on our server")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

}
