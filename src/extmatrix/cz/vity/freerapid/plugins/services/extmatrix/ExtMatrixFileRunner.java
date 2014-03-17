package cz.vity.freerapid.plugins.services.extmatrix;

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
class ExtMatrixFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(ExtMatrixFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
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
        final Matcher filenameMatcher = PlugUtils.matcher("<h1[^>]*>(.+)\\s*\\((.+)\\)\\s*<", content);
        if (!filenameMatcher.find())
            throw new PluginImplementationException("File name/size not found");
        httpFile.setFileName(filenameMatcher.group(1).trim());
        httpFile.setFileSize(PlugUtils.getFileSizeFromString(filenameMatcher.group(2)));
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        GetMethod method = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            final String content = getContentAsString();//check for response
            checkProblems();//check problems
            checkNameAndSize(content);//extract file name and size from the page

            final Matcher matcher = PlugUtils.matcher("document.location='(.*/get/.*)';", content);
            if (!matcher.find())
                throw new PluginImplementationException("Next page link not found");
            method = getGetMethod(matcher.group(1));

            boolean bReCaptcha = true;
            int count = 0;
            while (bReCaptcha) {
                if (count++ > 5)
                    throw new PluginImplementationException("Problem with captcha");
                bReCaptcha = false;

                if (!makeRedirectedRequest(method)) {
                    checkProblems();
                    throw new ServiceConnectionProblemException("Error loading page");
                }
                final MethodBuilder methodB = getMethodBuilder()
                        .setActionFromFormWhereActionContains(httpFile.getFileName(), true);
                try {
                    if (!tryDownloadAndSaveFile(stepReCaptcha(methodB))) {
                        checkProblems();//if downloading failed
                        if (getContentAsString().contains("www.extlocker.com"))  //You have entered the wrong security code
                            bReCaptcha = true;
                        else
                            throw new ServiceConnectionProblemException("Error starting download");//some unknown problem
                    }
                } catch (PluginImplementationException ee) {
                    if (ee.getMessage().equals("ReCaptcha challenge not found"))
                        bReCaptcha = true;
                    else
                        throw ee;
                }
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        if (content.contains("404 Not Found") ||
                content.contains("The file you have requested does not exist")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
        final Matcher matcher = PlugUtils.matcher("Max \\d+ files per \\d+ hours for free users", content);
        if (matcher.find())
            throw new YouHaveToWaitException(matcher.group(0), 600);
    }

    private HttpMethod stepReCaptcha(MethodBuilder methodBuilder) throws Exception {
        final String reCaptchaKey = PlugUtils.getStringBetween(getContentAsString(), "Recaptcha.create(\"", "\",\"");
        final ReCaptcha r = new ReCaptcha(reCaptchaKey, client);
        final String captcha = getCaptchaSupport().getCaptcha(r.getImageURL());
        if (captcha == null) {
            throw new CaptchaEntryInputMismatchException();
        }
        r.setRecognized(captcha);

        return r.modifyResponseMethod(methodBuilder).toPostMethod();
    }
}