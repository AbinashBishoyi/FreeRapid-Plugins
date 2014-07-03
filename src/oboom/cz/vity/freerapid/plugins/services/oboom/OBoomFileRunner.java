package cz.vity.freerapid.plugins.services.oboom;

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
class OBoomFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(OBoomFileRunner.class.getName());
    private final static String baseUrl = "https://www.oboom.com";
    private String file_ID;
    private String sessionToken;

    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        if (fileURL.contains("https:")) fileURL = fileURL.replaceFirst("https:", "http:");
        final GetMethod getMethod = getGetMethod(fileURL);//make first request
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize(getContentAsString());//ok let's extract file name and size from the page
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize(String content) throws Exception {
        final Matcher match = PlugUtils.matcher("oboom.com/#?([^/#]+)([/#].+)?", fileURL);
        if (!match.find())
            throw new InvalidURLOrServiceProblemException("Unable to get file id from url");
        file_ID = match.group(1);
        sessionToken = PlugUtils.getStringBetween(content, "Session : \"", "\"");
        final HttpMethod method = getMethodBuilder().setReferer(baseUrl)
                .setAction("http://api.oboom.com/1/ls")
                .setParameter("token", sessionToken)
                .setParameter("item", file_ID)
                .setParameter("http_errors", "0")
                .toPostMethod();
        method.addRequestHeader("X-Requested-With", "XMLHttpRequest");
        if (!makeRedirectedRequest(method)) {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
        checkProblems();
        httpFile.setFileName(PlugUtils.getStringBetween(getContentAsString(), "name\":\"", "\"").trim());
        httpFile.setFileSize(Long.parseLong(PlugUtils.getStringBetween(getContentAsString(), "size\":", ",")));
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        if (fileURL.contains("https:")) fileURL = fileURL.replaceFirst("https:", "http:");
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            final String contentAsString = getContentAsString();//check for response
            checkProblems();//check problems
            checkNameAndSize(contentAsString);//extract file name and size from the page

            final HttpMethod aMethod = getMethodBuilder().setReferer(baseUrl)
                    .setAction("http://www.oboom.com/1.0/download/config")
                    .setParameter("token", sessionToken)
                    .toPostMethod();
            aMethod.addRequestHeader("X-Requested-With", "XMLHttpRequest");
            if (!makeRedirectedRequest(aMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            checkProblems();
            final String key = PlugUtils.getStringBetween(getContentAsString(), "recaptcha\":\"", "\"");
            //final int wait = PlugUtils.getNumberBetween(getContentAsString(), "waiting\":", ",");
            //if (wait > 0 )
            //    downloadTask.sleep(wait + 1);
            do {
                final HttpMethod bMethod = stepCaptcha(key, stepAppVars(
                        getMethodBuilder().setReferer(baseUrl)
                                .setAction("http://www.oboom.com/1.0/download/ticket")
                                .setParameter("token", sessionToken)
                                .setParameter("download_id", file_ID)
                )).toGetMethod();
                bMethod.addRequestHeader("X-Requested-With", "XMLHttpRequest");
                if (!makeRedirectedRequest(bMethod)) {
                    checkProblems();
                    throw new ServiceConnectionProblemException();
                }
            } while (getContentAsString().contains("incorrect-captcha-sol"));
            checkProblems();
            Matcher match = PlugUtils.matcher(",\"(.+?)\",\"(.+?)\"", getContentAsString());
            if (!match.find())
                throw new PluginImplementationException("Download params not found");
            final HttpMethod cMethod = getMethodBuilder().setReferer(baseUrl)
                    .setAction("http://api.oboom.com/1/dl")
                    .setParameter("token", match.group(1))
                    .setParameter("item", file_ID)
                    .setParameter("auth", match.group(2))
                    .setParameter("http_errors", "0")
                    .toPostMethod();
            cMethod.addRequestHeader("X-Requested-With", "XMLHttpRequest");
            if (!makeRedirectedRequest(cMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            checkProblems();
            match = PlugUtils.matcher(",\"(.+?)\",\"(.+?)\"", getContentAsString());
            if (!match.find())
                throw new PluginImplementationException("Download url not found");
            final HttpMethod httpMethod = getMethodBuilder().setReferer(baseUrl)
                    .setAction("http://" + match.group(1) + "/1.0/dlh")
                    .setParameter("ticket", match.group(2))
                    .toGetMethod();

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
        if (content.contains("404,\"item\"") || content.contains("410,\"abused\"") || content.contains("410,\"expired\"")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
        if (content.contains("400,\"blocked_wait\""))
            throw new YouHaveToWaitException("Your Download limit is consumed", 300);

        if (content.contains("[4"))
            throw new PluginImplementationException("Unrecognised error");
    }

    private MethodBuilder stepAppVars(MethodBuilder builder) throws Exception {
        if (!makeRedirectedRequest(getGetMethod("http://www.oboom.com/assets/js/core/download.js"))) {
            checkProblems();
            throw new ServiceConnectionProblemException("error opening download.js");
        }
        final String appID = PlugUtils.getStringBetween(getContentAsString(), "app_id' : '", "'");
        final String appSession = "2874024834";  //complex javascript generated number, unable to generate here, seems to be constant
        return builder.setParameter("app_id", appID).setParameter("app_session", appSession);
    }

    private MethodBuilder stepCaptcha(String recaptchaKey, MethodBuilder builder) throws Exception {
        final ReCaptcha reCaptcha = new ReCaptcha(recaptchaKey, client);
        final String captcha = getCaptchaSupport().getCaptcha(reCaptcha.getImageURL());
        if (captcha == null)
            throw new CaptchaEntryInputMismatchException();
        reCaptcha.setRecognized(captcha);
        return reCaptcha.modifyResponseMethod(builder);
    }
}