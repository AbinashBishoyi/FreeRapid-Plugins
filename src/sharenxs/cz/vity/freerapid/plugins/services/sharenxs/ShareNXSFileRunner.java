package cz.vity.freerapid.plugins.services.sharenxs;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;

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
        final String fileFullSizeURL = fileURL + "&pjk=l";
        logger.info("Starting download in TASK " + fileFullSizeURL);
        final GetMethod method = getGetMethod(fileFullSizeURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            String contentAsString = getContentAsString();//check for response
            if (contentAsString.contains("Click here to continue to image")) {
                if (!makeRedirectedRequest(method)) {
                    checkProblems();
                    throw new ServiceConnectionProblemException();
                }
                contentAsString = getContentAsString();
            }
            checkProblems();//check problems
            //checkNameAndSize(contentAsString);//extract file name and size from the page
            //Matcher matcher = PlugUtils.matcher("(\\w+\\.\\w+)(\" id=img1)", contentAsString);
            Matcher matcher = PlugUtils.matcher("/([^/]+\\.\\w+)(\" id=img1)", contentAsString);
            if (!matcher.find()) {
                logger.warning("File not found");
                throw new URLNotAvailableAnymoreException("File not found");
            }
            httpFile.setFileName(matcher.group(1));

            final HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL).setActionFromImgSrcWhereTagContains("img1").toGetMethod();
            //here is the download link extraction
            if (!tryDownloadAndSaveFile(httpMethod)) {
                checkProblems();//if downloading failed
                logger.warning(getContentAsString());//log the info
                throw new PluginImplementationException();//some unknown problem
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }

    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("File not found")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }

}