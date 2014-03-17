package cz.vity.freerapid.plugins.services.cloudnator;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.recaptcha.ReCaptcha;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class CloudNatorFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(CloudNatorFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        checkURL();
        final GetMethod getMethod = getGetMethod(fileURL);//make first request
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize(getContentAsString());//ok let's extract file name and size from the page
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        final Matcher filenameMatcher = PlugUtils.matcher("<h2>\\s*(.+)<[^>]*>\\((.+)\\)<", content);
        if (!filenameMatcher.find())
            throw new PluginImplementationException("File name not found");
        httpFile.setFileName(filenameMatcher.group(1));
        httpFile.setFileSize(PlugUtils.getFileSizeFromString(filenameMatcher.group(2)));
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        checkURL();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            final String contentAsString = getContentAsString();//check for response
            checkProblems();//check problems
            checkNameAndSize(contentAsString);//extract file name and size from the page
            boolean bReCaptcha = true;
            while (bReCaptcha) {
                bReCaptcha = false;
                if (!makeRedirectedRequest(method)) {
                    checkProblems();
                    throw new ServiceConnectionProblemException();
                }
                checkProblems();
                downloadTask.sleep(PlugUtils.getNumberBetween(getContentAsString(), "var downloadWait = ", ";") + 1);
                MethodBuilder methodB = getMethodBuilder()
                        .setActionFromFormByName("download", true)
                        .setReferer(fileURL);
                if (!tryDownloadAndSaveFile(stepReCaptcha(methodB))) {
                    checkProblems();   //if downloading failed
                    if (getContentAsString().contains("Sie haben den Sicherheitscode falsch eingegeben"))  //You have entered the wrong security code
                        bReCaptcha = true;
                    else
                        throw new ServiceConnectionProblemException("Error starting download");//some unknown problem
                }
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("Selected file not found") || contentAsString.contains("was deleted")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        if (contentAsString.contains("Ihre Session-ID ist ungültig")) //Session ID invalid  ...failed to wait
            throw new ServiceConnectionProblemException("Download Error - Session ID is invalid");
        Matcher match = PlugUtils.matcher("Please wait (\\d+) minutes", contentAsString);
        if (match.find())
            throw new YouHaveToWaitException("You have to wait", Integer.parseInt(match.group(1)));
    }

    private HttpMethod stepReCaptcha(MethodBuilder methodBuilder) throws Exception {
        final Matcher reCaptchaKeyMatcher = getMatcherAgainstContent("recaptcha/api/challenge\\?k=(.*?)\"");
        if (!reCaptchaKeyMatcher.find())
            throw new PluginImplementationException("Recaptcha not found");
        final String reCaptchaKey = reCaptchaKeyMatcher.group(1);
        final ReCaptcha r = new ReCaptcha(reCaptchaKey, client);
        final String captcha = getCaptchaSupport().getCaptcha(r.getImageURL());
        if (captcha == null) {
            throw new CaptchaEntryInputMismatchException();
        }
        r.setRecognized(captcha);

        return r.modifyResponseMethod(methodBuilder).toPostMethod();
    }

    private void checkURL() {
        fileURL = fileURL.replaceFirst("shragle\\.com/files/", "cloudnator.com/files/");
    }

    @Override
    protected String getBaseURL() {
        return "http://www.cloudnator.com/";
    }
}