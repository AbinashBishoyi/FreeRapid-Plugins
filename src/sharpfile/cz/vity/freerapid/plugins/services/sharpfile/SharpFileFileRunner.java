package cz.vity.freerapid.plugins.services.sharpfile;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.recaptcha.ReCaptcha;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.Date;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class SharpFileFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(SharpFileFileRunner.class.getName());

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
        final Matcher filenameMatcher = PlugUtils.matcher("<h2>Download File (.+)</h2>", content);
        if (filenameMatcher.find()) {
            httpFile.setFileName(filenameMatcher.group(1));
        } else {
            throw new PluginImplementationException("File name not found");
        }
        final Matcher fileSizeMatcher = PlugUtils.matcher("</font>\\s*\\((.+)\\)\\s*</font>", content);
        if (fileSizeMatcher.find()) {
            httpFile.setFileSize(PlugUtils.getFileSizeFromString(fileSizeMatcher.group(1)));
        } else {
            throw new PluginImplementationException("File size not found");
        }
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            String contentAsString = getContentAsString();//check for response
            checkProblems();//check problems
            checkNameAndSize(contentAsString);//extract file name and size from the page

            MethodBuilder methodBuilder = getMethodBuilder()
                    .setReferer(fileURL)
                    .setActionFromFormWhereTagContains("method_free", true)
                    .setAction(fileURL)
                    .removeParameter("method_premium");
            if (!makeRedirectedRequest(methodBuilder.toPostMethod())) {
                checkProblems();
                throw new ServiceConnectionProblemException("Error starting download-1");
            }
            checkProblems();

            boolean bReCaptcha = true;
            while (bReCaptcha) {
                bReCaptcha = false;
                contentAsString = getContentAsString();
                long startTime = new Date().getTime();
                Integer iWait = PlugUtils.getNumberBetween(PlugUtils.getStringBetween(contentAsString, "Wait <span", "seconds"), "\">", "</span>");

                HttpMethod httpMethod = stepReCaptcha(getMethodBuilder()
                        .setReferer(fileURL)
                        .setActionFromFormWhereTagContains("method_free", true)
                        .setAction(fileURL)
                        .removeParameter("method_premium")
                );

                long endTime = new Date().getTime();
                int timeDiff = Integer.decode(String.valueOf((endTime - startTime) / 1000));   // time taken to input captcha
                downloadTask.sleep(iWait - timeDiff + 1);

                if (!tryDownloadAndSaveFile(httpMethod)) {
                    checkProblems();//if downloading failed
                    if (getContentAsString().contains("class=\"err\">Wrong captcha"))
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
        if (contentAsString.contains("<b>File Not Found")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
        if (contentAsString.contains("class=\"err\">You have to wait")) {
            String strWaitTime = "X" + PlugUtils.getStringBetween(contentAsString, "You have to wait ", " till next download") + "X";
            int intWaitTime, intWaitHour = 0, intWaitMin = 0, intWaitSec = 0;
            if (strWaitTime.contains("hour")) {
                intWaitMin = PlugUtils.getNumberBetween(strWaitTime, "X", " hour");
                strWaitTime = "X" + PlugUtils.getStringBetween(strWaitTime, ", ", "X") + "X";
            }
            if (strWaitTime.contains("minute")) {
                intWaitMin = PlugUtils.getNumberBetween(strWaitTime, "X", " minute");
                strWaitTime = "X" + PlugUtils.getStringBetween(strWaitTime, ", ", "X") + "X";
            }
            if (strWaitTime.contains("second"))
                intWaitSec = PlugUtils.getNumberBetween(strWaitTime, "X", " second");

            intWaitTime = intWaitHour * 3600 + intWaitMin * 60 + intWaitSec + 1;
            logger.info("You have to wait: " + intWaitTime + " seconds");
            throw new YouHaveToWaitException("You have to wait", intWaitTime);
        }
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
}