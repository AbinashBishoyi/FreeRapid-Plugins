package cz.vity.freerapid.plugins.services.beemp3;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class BeeMP3FileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(BeeMP3FileRunner.class.getName());

    @Override
    public void runCheck() throws Exception { //this method validates file
        if (fileURL.contains("beemp3.com/")) fileURL = fileURL.replaceFirst("beemp3.com/", "beemp3s.org/");
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
        Matcher match = PlugUtils.matcher("<h1.*?>(.+?)<", content);
        if (!match.find()) {
            throw new PluginImplementationException("File size not found");
        }
        httpFile.setFileName(match.group(1).trim());
        match = PlugUtils.matcher("Filesize:</th>\\s+?<td><b.*?>(.+?)<", content);
        if (!match.find()) {
            throw new PluginImplementationException("File size not found");
        }
        httpFile.setFileSize(PlugUtils.getFileSizeFromString(match.group(1).trim()));
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        if (fileURL.contains("beemp3.com/")) fileURL = fileURL.replaceFirst("beemp3.com/", "beemp3s.org/");
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            checkProblems();//check problems
            checkNameAndSize(getContentAsString());
            HttpMethod httpMethod;
            do {
                if (!makeRedirectedRequest(method)) {
                    checkProblems();
                    throw new ServiceConnectionProblemException();
                }
                httpMethod = stepCaptcha(getMethodBuilder().setReferer(fileURL)
                        .setAction("http://beemp3s.org/chk_cd.php")
                        .setParameter("id", PlugUtils.getStringBetween(getContentAsString(), "var idm =\"", "\";"))
                ).toGetMethod();
                if (!makeRedirectedRequest(httpMethod)) {
                    checkProblems();
                    throw new PluginImplementationException("Error processing captcha");
                }
                checkProblems();
            } while (getContentAsString().contains("Error: Wrong Answer"));

            Matcher match = PlugUtils.matcher(".+?(http:.+)", getContentAsString());
            if (!match.find())
                throw new PluginImplementationException("Download link not found");
            httpMethod = getGetMethod(match.group(1));

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
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("Sorry! Something is wrong")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }

    private MethodBuilder stepCaptcha(MethodBuilder method) throws Exception {
        final CaptchaSupport captchaSupport = getCaptchaSupport();
        final String captchaImg = getMethodBuilder().setBaseURL("http://beemp3s.org/").setActionFromImgSrcWhereTagContains("captcha").getEscapedURI();
        String captchaTxt = captchaSupport.getCaptcha(captchaImg);
        if (captchaTxt == null)
            throw new CaptchaEntryInputMismatchException("Captcha responce is required");

        if (captchaTxt.contains("+") || captchaTxt.contains("-") || captchaTxt.contains("*") || captchaTxt.contains("/")) {
            ScriptEngineManager mgr = new ScriptEngineManager();
            ScriptEngine engine = mgr.getEngineByName("JavaScript");
            captchaTxt = "" + engine.eval(captchaTxt);
        }
        method.setParameter("code", captchaTxt);
        return method;
    }

}