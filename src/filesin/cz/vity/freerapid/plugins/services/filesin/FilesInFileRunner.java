package cz.vity.freerapid.plugins.services.filesin;

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
class FilesInFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(FilesInFileRunner.class.getName());

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
        PlugUtils.checkName(httpFile, content, "<title>", " | FilesIn.com</title>");
        PlugUtils.checkFileSize(httpFile, content, "File size <b>", "</b>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            final String contentAsString = getContentAsString();//check for response
            checkProblems();//check problems
            checkNameAndSize(contentAsString);//extract file name and size from the page
            final HttpMethod httpMethod = getMethodBuilder()
                    .setReferer(fileURL)
                    .setActionFromAHrefWhereATagContains("http://www.filesin.com/images/slow_download.png")
                    .toHttpMethod();
            if (!makeRedirectedRequest(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            checkProblems();
            //final long startTime = System.currentTimeMillis();
            //final int waitTime = 1 + PlugUtils.getNumberBetween(getContentAsString(), "var time_left = ", ";");
            final Matcher match = PlugUtils.matcher("<form(?:\\s|.)+?</form>", getContentAsString());
            while (match.find()) {
                if (match.group(0).contains("recaptcha/api")) {
                    boolean captchaLoop = true;
                    while (captchaLoop) {
                        captchaLoop = false;
                        final HttpMethod cMethod = reCaptcha(getMethodBuilder(match.group(0))
                                .setActionFromFormByIndex(1, true)
                                .setAction(httpMethod.getURI().getURI())
                        ).toPostMethod();
                        //final long endTime = System.currentTimeMillis();
                        //downloadTask.sleep(10+waitTime - (int)((endTime-startTime)/1000));

                        if (!tryDownloadAndSaveFile(cMethod)) {
                            if (getContentAsString().contains("Please Enter Correct Captcha Code"))
                                captchaLoop = true;
                            else {
                                checkProblems();//if downloading failed
                                throw new ServiceConnectionProblemException("Error starting download");//some unknown problem
                            }
                        }
                    }
                }
            }
            throw new PluginImplementationException("reCaptcha not found");
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        if (content.contains("File Not Found") || content.contains("Page Not Found")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
        if (content.contains("Maximum number of FREE downloads per hour exceeded")) {
            final int waitMin = PlugUtils.getNumberBetween(content, "try again after (", ") Minute(s)");
            throw new YouHaveToWaitException("Hourly free download limit reached", waitMin * 60);
        }
    }

    private MethodBuilder reCaptcha(MethodBuilder method) throws Exception {
        final String reCaptchaKey = PlugUtils.getStringBetween(getContentAsString(), "recaptcha/api/challenge?k=", "\">");
        final ReCaptcha r = new ReCaptcha(reCaptchaKey, client);
        final String captcha = getCaptchaSupport().getCaptcha(r.getImageURL());
        if (captcha == null) {
            throw new CaptchaEntryInputMismatchException();
        }
        r.setRecognized(captcha);
        return r.modifyResponseMethod(method);
    }
}