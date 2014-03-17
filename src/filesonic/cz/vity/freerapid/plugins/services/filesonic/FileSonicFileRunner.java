package cz.vity.freerapid.plugins.services.filesonic;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.recaptcha.ReCaptcha;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class which contains main code
 *
 * @author JPEXS
 */
class FileSonicFileRunner extends AbstractRunner {

    private final static Logger logger = Logger.getLogger(FileSonicFileRunner.class.getName());

    private String ensureENLanguage(String url) {
        Matcher m = Pattern.compile("http://(www\\.)?filesonic.com/([^/]*/)?(file/.*)").matcher(url);
        if (m.matches()) {
            return "http://www.filesonic.com/en/" + m.group(3);
        }
        return url;
    }

    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        final GetMethod getMethod = getGetMethod(ensureENLanguage(fileURL));//make first request
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize(getContentAsString());//ok let's extract file name and size from the page
        } else {
            throw new PluginImplementationException();
        }
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, content, "<span>Filename: </span> <strong>", "</strong>");
        PlugUtils.checkFileSize(httpFile, content, "<span class=\"size\">", "</span>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(ensureENLanguage(fileURL)); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            final String contentAsString = getContentAsString();//check for response
            checkProblems();//check problems
            checkNameAndSize(contentAsString);//extract file name and size from the page
            String startUrl = fileURL + "?start=1";
            HttpMethod methodFree = getMethodBuilder().setReferer(fileURL).setAction(startUrl).toPostMethod();
            while (true) {
                if (!makeRedirectedRequest(methodFree)) {
                    throw new PluginImplementationException();
                }
                final String content = getContentAsString();

                if (content.contains("captchaForm")) {
                    String reCaptchaKey = PlugUtils.getStringBetween(content, "Recaptcha.create(\"", "\",");
                    ReCaptcha r = new ReCaptcha(reCaptchaKey, client);
                    CaptchaSupport captchaSupport = getCaptchaSupport();
                    String captchaURL = r.getImageURL();
                    String captcha = captchaSupport.getCaptcha(captchaURL);
                    if (captcha == null) {
                        throw new CaptchaEntryInputMismatchException();
                    } else {
                        r.setRecognized(captcha);
                        methodFree = r.modifyResponseMethod(getMethodBuilder().setReferer(fileURL).setAction(startUrl)).toPostMethod();
                    }
                } else if (content.contains("Please Wait")) {
                    final int waitTime = PlugUtils.getWaitTimeBetween(content, "\"countdown\">", "</strong>", TimeUnit.SECONDS);
                    downloadTask.sleep(waitTime);
                    methodFree = getMethodBuilder().setReferer(fileURL).setAction(startUrl).toPostMethod();
                } else if (content.contains("Download Ready")) {
                    final HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL).setActionFromAHrefWhereATagContains("Start download now!").toGetMethod();
                    if (!tryDownloadAndSaveFile(httpMethod)) {
                        logger.warning(getContentAsString());//log the info
                        throw new PluginImplementationException();//some unknown problem
                    }
                } else {
                    checkProblems();
                    throw new PluginImplementationException();
                }
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();


        if(contentAsString.contains("error: already processing a download")){
            throw new YouHaveToWaitException("Error: already processing a download", 30);
        }
        if (contentAsString.contains("File not found")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }
}
