package cz.vity.freerapid.plugins.services.getapp;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;

import java.awt.image.BufferedImage;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author Eterad
 */
class GetAppFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(GetAppFileRunner.class.getName());


    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);//make first request
        if (makeRedirectedRequest(getMethod)) {
            checkNameAndSize(getContentAsString());//ok let's extract file name and size from the page
        } else
            throw new PluginImplementationException();
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        checkProblems();//check problems
        PlugUtils.checkName(httpFile, content, "File Name:<font color=\"#0088CC\">", "</font><br>");
        PlugUtils.checkFileSize(httpFile, content, "Size:\n" + "<font color=\"#0088CC\">", "</font><br>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            final String contentAsString = getContentAsString();//check for response
            checkNameAndSize(contentAsString);
            checkProblems();//check problems
            if (stepCaptcha(getContentAsString())){
                Matcher matcher = getMatcherAgainstContent("href='(.+)'><");
                if (!matcher.find()){
                    checkProblems();
                    logger.warning(getContentAsString());
                    throw new PluginImplementationException();
                }
                String s = matcher.group(1);
                final HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL).setAction(s).toHttpMethod();
                if (!tryDownloadAndSaveFile(httpMethod)){
                    checkProblems();
                    logger.warning(getContentAsString());
                    throw new PluginImplementationException();
                }
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private boolean stepCaptcha(String contentAsString) throws Exception {
        if (contentAsString.contains("Key in your answer")) {

            Matcher matcher = PlugUtils.matcher("src=\"(.*?/image.php[^\"]*)\"", contentAsString);
            if (matcher.find()) {
                String s;
                s = "http://getapp.info/image.php";
                String captcha;
                final BufferedImage captchaImage = getCaptchaSupport().getCaptchaImage(s);
                captcha = getCaptchaSupport().askForCaptcha(captchaImage);
                if (captcha == null)
                    throw new CaptchaEntryInputMismatchException();

                final PostMethod postMethod = getPostMethod(fileURL);

                PlugUtils.addParameters(postMethod, contentAsString, new String[]{"redirect2"});

                postMethod.addParameter("secure", captcha);

                if (makeRequest(postMethod)) {

                    logger.info("Sent captcha answer: " + captcha);
                    return true;
                }
            } else throw new PluginImplementationException("Captcha picture was not found");
        }
        return false;
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("The file requested is either invalid or may have been claimed by copyright holders.")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }

}
