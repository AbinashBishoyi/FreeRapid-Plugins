package cz.vity.freerapid.plugins.services.bego;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class BegoFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(BegoFileRunner.class.getName());

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
        PlugUtils.checkName(httpFile, content, "file_title\">", "</div>");
        Matcher match = PlugUtils.matcher("file_info\">\\s+?<li>[^：]+?：(.+?)</li>", content);
        if (!match.find())
            throw new PluginImplementationException("File size not found");
        httpFile.setFileSize(PlugUtils.getFileSizeFromString(match.group(1)));
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

            HttpMethod httpMethod = stepCaptcha(getMethodBuilder()
                    .setActionFromFormByName("user_form", true)
                    .setReferer(fileURL)).toPostMethod();
            if (!makeRedirectedRequest(httpMethod)) {
                checkProblems();//if downloading failed
                throw new PluginImplementationException("Error entering captcha");
            }
            Matcher match = PlugUtils.matcher("<a.+?id=\"\\w+?_free\".+?</a>", getContentAsString());
            if (!match.find()) {
                throw new PluginImplementationException("Unable to find download link");
            }
            final String encodedUrl = PlugUtils.getStringBetween(match.group(0), "href=\"", "\" ");
            final String decodedUrl = new String(Base64.decodeBase64(encodedUrl));
            httpMethod = getGetMethod(decodedUrl);

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
        if (content.contains("<li>1. ") && content.contains("<li>2. ") && content.contains("<li>3. ")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }

    private MethodBuilder stepCaptcha(MethodBuilder method) throws Exception {
        final CaptchaSupport captchaSupport = getCaptchaSupport();
        final String captchaImg = "http://www.bego.cc" + PlugUtils.getStringBetween(getContentAsString(), "vfcode' src='", "\",Math") + Math.random();
        final String captchaTxt = captchaSupport.getCaptcha(captchaImg);
        if (captchaTxt == null) throw new CaptchaEntryInputMismatchException("No Input");

        method.setParameter("randcode", captchaTxt);
        return method;
    }


}