package cz.vity.freerapid.plugins.services.cloudzer;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.recaptcha.ReCaptcha;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class CloudZerFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(CloudZerFileRunner.class.getName());

    final String BaseUrl = "http://cloudzer.net/";

    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        addCookie(new Cookie(".cloudzer.net", "lang", "en", "/", 86400, false));
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
        PlugUtils.checkName(httpFile, content, "id=\"filename\">", "</b>");
        PlugUtils.checkFileSize(httpFile, content, "class=\"size\">", "</span>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        addCookie(new Cookie(".cloudzer.net", "lang", "en", "/", 86400, false));
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            final String content = getContentAsString();//check for response
            checkProblems();//check problems
            checkNameAndSize(content);//extract file name and size from the page

            final Matcher match = PlugUtils.matcher("<meta name=\"auth\" content=\"(.+?)\"><meta name=\"wait\" content=\"(\\d+?)\">", content);
            if (!match.find())
                throw new PluginImplementationException("error locating download details");
            final String sAuth = match.group(1);
            //final int iWait = 1 + Integer.parseInt(match.group(2));

            final HttpMethod slotMethod = getMethodBuilder()
                    .setBaseURL(BaseUrl)
                    .setReferer(fileURL)
                    .setAction("io/ticket/slot/" + sAuth)
                    .toPostMethod();
            if (!makeRedirectedRequest(slotMethod)) {
                throw new PluginImplementationException("error checking for available slot");
            }
            if (!getContentAsString().contains("\"succ\":true")) {
                throw new PluginImplementationException("No free download slot available");
            }
            //downloadTask.sleep(iWait);
            do {
                final HttpMethod downloadMethod = reCaptcha(getMethodBuilder()
                        .setBaseURL(BaseUrl)
                        .setReferer(fileURL)
                        .setAction("io/ticket/captcha/" + sAuth)
                ).toPostMethod();
                if (!makeRedirectedRequest(downloadMethod)) {
                    throw new PluginImplementationException("error getting download link");
                }
            } while (getContentAsString().contains("err\":\"captcha\""));
            checkProblems();
            final HttpMethod httpMethod = getGetMethod(PlugUtils.getStringBetween(getContentAsString(), "\"url\":\"", "\"}").trim().replace("\\/", "/"));
            if (!tryDownloadAndSaveFile(httpMethod)) {
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
        if (content.contains("File Not Found")) {//TODO
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
        if (content.contains("You have reached the max. number of possible free downloads for this hour")) {
            throw new YouHaveToWaitException("Hourly free download limit reached", 600);
        }
    }

    private MethodBuilder reCaptcha(MethodBuilder dMethod) throws Exception {
        HttpMethod jsMethod = getGetMethod(BaseUrl + "js/download.js");
        setFileStreamContentTypes(new String[0], new String[]{"application/x-javascript"});
        if (!makeRequest(jsMethod)) {
            throw new PluginImplementationException("Error loading javascript");
        }
        final String reCaptchaKey = PlugUtils.getStringBetween(getContentAsString(), "Recaptcha.create(\"", "\",").trim();
        final ReCaptcha r = new ReCaptcha(reCaptchaKey, client);
        final String captcha = getCaptchaSupport().getCaptcha(r.getImageURL());
        if (captcha == null) {
            throw new CaptchaEntryInputMismatchException();
        }
        r.setRecognized(captcha);
        return r.modifyResponseMethod(dMethod);
    }
}