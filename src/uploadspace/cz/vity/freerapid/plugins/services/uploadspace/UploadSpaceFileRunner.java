package cz.vity.freerapid.plugins.services.uploadspace;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author JPEXS
 */
class UploadSpaceFileRunner extends AbstractRunner {

    private final static Logger logger = Logger.getLogger(UploadSpaceFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);//make first request
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize(getContentAsString());//ok let's extract file name and size from the page
        } else {
            throw new PluginImplementationException();
        }
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, content, "<b>Filename:</b></td><td nowrap>", "</td>");
        PlugUtils.checkFileSize(httpFile, content, " <small>(", ")</small>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL); //create GET request
        String contentAsString = "";
        boolean repeat=false;
        do{
            if (!makeRedirectedRequest(method)){
                throw new PluginImplementationException();
            }
            contentAsString=getContentAsString();
            repeat=false;
            if(contentAsString.contains("You have reached the download limit for free-users")){
                downloadTask.sleep(PlugUtils.getWaitTimeBetween(getContentAsString(), "You can wait download for ", " minute", TimeUnit.MINUTES));
                repeat=true;
            }
        }while(repeat);
            //check for response
            checkProblems();//check problems
            checkNameAndSize(contentAsString);//extract file name and size from the page
            Matcher matcher = getMatcherAgainstContent("<br>(Your IP address .* is already downloading a file.*)<br>");
            if (matcher.find()) {
                throw new ServiceConnectionProblemException(matcher.group(1));
            }
            MethodBuilder mb = getMethodBuilder().setReferer(fileURL).setActionFromFormByName("F1", true).setAction(fileURL);

            matcher = getMatcherAgainstContent("\"(http://api.recaptcha.net/challenge\\?k=[^\"]+)\"");
            if (matcher.find()) {
                if (makeRedirectedRequest(getGetMethod(matcher.group(1)))) {
                    String challenge = PlugUtils.getStringBetween(getContentAsString(), "challenge : '", "'");
                    mb.setParameter("recaptcha_challenge_field", challenge);
                    CaptchaSupport captchaSupport = getCaptchaSupport();

                    String captchaR = captchaSupport.getCaptcha("http://api.recaptcha.net/image?c=" + challenge);
                    if (captchaR == null) {
                        throw new CaptchaEntryInputMismatchException();
                    }
                    mb.setParameter("recaptcha_response_field", captchaR);
                    final HttpMethod method2 = mb.toPostMethod();
                    //here is the download link extraction
                    if (!tryDownloadAndSaveFile(method2)) {
                        checkProblems();//if downloading failed
                        logger.warning(getContentAsString());//log the info
                        throw new PluginImplementationException();//some unknown problem
                    }
                } else {
                    throw new PluginImplementationException();
                }
            } else {
                throw new PluginImplementationException();
            }
        
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("File Not Found")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }
}
