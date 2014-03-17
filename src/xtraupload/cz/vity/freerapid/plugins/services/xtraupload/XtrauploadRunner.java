package cz.vity.freerapid.plugins.services.xtraupload;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;

import java.io.IOException;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author Ladislav Vitasek, Ludek Zika
 */
class XtrauploadRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(XtrauploadRunner.class.getName());
    private String initURL;

    public void runCheck() throws Exception {
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRequest(getMethod)) {
            checkNameAndSize(getContentAsString());

        } else
            throw new PluginImplementationException();
    }

    public void run() throws Exception {
        super.run();
        initURL = fileURL;
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod getMethod = getGetMethod(fileURL);
        getMethod.setFollowRedirects(true);
        if (makeRequest(getMethod)) {
            checkNameAndSize(getContentAsString());
            do {
                checkProblems();
                if (!getContentAsString().contains("captchacode")) {
                    logger.info(getContentAsString());
                    throw new PluginImplementationException("No captcha.\nCannot find requested page content");
                }
                stepCaptcha(getContentAsString());
            } while (getContentAsString().contains("Captcha number error or expired"));
            Matcher matcher = getMatcherAgainstContent("document.location=\"([^\"]*)\"");
            if (matcher.find()) {
                String s = matcher.group(1);
                logger.info("Found File URL - " + s);

                final GetMethod method = getGetMethod(s);
                if (!tryDownloadAndSaveFile(method)) {
                    checkProblems();
                    throw new IOException("File input stream is empty.");
                }
            } else {
                checkProblems();
                logger.info(getContentAsString());
                throw new PluginImplementationException();
            }
        } else
            throw new PluginImplementationException();
    }

    private void checkNameAndSize(String content) throws Exception {
        Matcher matcher = PlugUtils.matcher("File size:((<[^>]*>)|\\s)*([^<]+)<", content);
        if (matcher.find()) {
            Long a = PlugUtils.getFileSizeFromString(matcher.group(matcher.groupCount()));
            logger.info("File size " + a);
            httpFile.setFileSize(a);
        }
        matcher = PlugUtils.matcher("File name:((<[^>]*>)|\\s)*([^<]+)<", content);
        if (matcher.find()) {
            final String fn = matcher.group(matcher.groupCount());
            logger.info("File name " + fn);
            httpFile.setFileName(fn);
        } else logger.warning("File name was not found" + content);
        checkProblems();
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private boolean stepCaptcha(String contentAsString) throws Exception {
        if (contentAsString.contains("captchacode")) {
            Matcher matcher = PlugUtils.matcher("captcha", contentAsString);
            if (matcher.find()) {
                String s = "http://www.xtraupload.de/captcha.php";
                String captcha = getCaptchaSupport().getCaptcha(s);
                if (captcha == null) {
                    throw new CaptchaEntryInputMismatchException();
                } else {

                    matcher = PlugUtils.matcher("name=myform action\\=\"([^\"]*)\"", contentAsString);
                    if (!matcher.find()) {
                        throw new PluginImplementationException("Captcha form action was not found");
                    }
                    s = matcher.group(1);
                    client.setReferer(initURL);
                    final PostMethod postMethod = getPostMethod(s);
                    postMethod.addParameter("captchacode", captcha);

                    if (makeRequest(postMethod)) {
                        return true;
                    }
                }
            } else {
                logger.warning(contentAsString);
                throw new PluginImplementationException("Captcha picture was not found");
            }
        }
        return false;
    }

    private void checkProblems() throws ServiceConnectionProblemException, YouHaveToWaitException, URLNotAvailableAnymoreException {
        String content = getContentAsString();
        if (content.contains("You have got max allowed download sessions from the same IP")) {
            throw new YouHaveToWaitException("You have got max allowed download sessions from the same IP!", 5 * 60);
        }
        if (content.contains("max allowed bandwidth size per hour")) {
            throw new YouHaveToWaitException("You have got max allowed bandwidth size per hour", 10 * 60);
        }
        if (content.contains("Your requested file is not found")) {
            throw new URLNotAvailableAnymoreException("The requested file is not available");
        }

    }

}
