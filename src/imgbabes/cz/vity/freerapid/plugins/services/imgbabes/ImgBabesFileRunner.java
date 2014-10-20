package cz.vity.freerapid.plugins.services.imgbabes;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class ImgBabesFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(ImgBabesFileRunner.class.getName());

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            checkProblems();//check problems
            final String imgUrl = getMethodBuilder().setReferer(fileURL)
                    .setActionFromImgSrcWhereTagContains("this_image").getEscapedURI();
            httpFile.setFileName(PlugUtils.suggestFilename(imgUrl));
            if (!tryDownloadAndSaveFile(getGetMethod(imgUrl))) {
                checkProblems();//if downloading failed
                throw new ServiceConnectionProblemException("Error starting download");//some unknown problem
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        if (content.contains("file you were looking for could not be found") ||
                content.contains("file was deleted") || content.contains("Nothing Found")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }

}