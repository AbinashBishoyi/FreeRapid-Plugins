package cz.vity.freerapid.plugins.services.linksavegroup;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;

/**
 * Class which contains main code
 *
 * @author Arthur Gunawan
 */
class LinksaveGroupFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(LinksaveGroupFileRunner.class.getName());
    private static final String SERVICE_WEB = "http://linksave.in";

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            final String contentAsString = getContentAsString();//check for response
            checkProblems();//check problems

            HttpMethod httpMethod = stepCaptcha(fileURL);

            //checkNameAndSize(contentAsString);//extract file name and size from the page
            //final HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL).setAction(fileURL).setActionFromFormByIndex(1,true).toHttpMethod();//TODO

            //here is the download link extraction
//            if (!tryDownloadAndSaveFile(httpMethod)) {
//                checkProblems();//if downloading failed
//                logger.warning(getContentAsString());//log the info
//                throw new PluginImplementationException();//some unknown problem
//            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("File Not Found")) {//TODO
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }
    private HttpMethod stepCaptcha(String redirectURL) throws ErrorDuringDownloadingException {
        final CaptchaSupport captchaSupport = getCaptchaSupport();
        final String captchaSrc = SERVICE_WEB + "/captcha/" + PlugUtils.getStringBetween(getContentAsString(), "src=\"./captcha/", "\" ");
        logger.info("Captcha URL " + captchaSrc);
        final String captcha = captchaSupport.getCaptcha(captchaSrc);

        if (captcha == null) {
            throw new CaptchaEntryInputMismatchException();
        } else {
            return getMethodBuilder().setAction(fileURL).setReferer(redirectURL).setActionFromFormByIndex(1, true).setParameter("code", captcha).toHttpMethod();
        }
    }
}
