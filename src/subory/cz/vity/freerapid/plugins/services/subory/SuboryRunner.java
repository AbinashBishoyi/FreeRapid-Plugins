package cz.vity.freerapid.plugins.services.subory;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;

import java.io.IOException;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author Ladislav Vitasek, Ludek Zika
 */
class SuboryRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(SuboryRunner.class.getName());
    private final static String WEB = "http://www.subory.sk";

    public SuboryRunner() {
        super();
    }

    public void runCheck() throws Exception {
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkNameAndSize(getContentAsString());
        } else
            throw new ServiceConnectionProblemException();
    }

    public void run() throws Exception {
        super.run();
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize(getContentAsString());
            client.setReferer(fileURL);

            if (getContentAsString().contains("captcha")) {
                PostMethod method = new PostMethod();

                while (getContentAsString().contains("captcha")) {
                    method = stepCaptcha(getContentAsString());
                    makeRequest(method);
                }

                if (!tryDownloadAndSaveFile(method)) {
                    checkProblems();
                    logger.warning(getContentAsString());
                    throw new IOException("File input stream is empty.");
                }
            } else {
                checkProblems();
                logger.info(getContentAsString());
                throw new PluginImplementationException();
            }
        } else
            throw new ServiceConnectionProblemException();
    }


    private void checkNameAndSize(String content) throws Exception {

        if (!content.contains("subory.sk")) {
            logger.warning(getContentAsString());
            throw new InvalidURLOrServiceProblemException("Invalid URL or unindentified service");
        }

        Matcher matcher = getMatcherAgainstContent("class=\"down-filename\">(?:(?:.|\\s)+?)>\\s*(.+?)<");
        if (matcher.find()) {
            String fn = matcher.group(1).trim();
            logger.info("File name " + fn);
            httpFile.setFileName(fn);
            httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
        } else logger.warning("File name not found");

        matcher = getMatcherAgainstContent("Ve.kos. s.boru:</strong></td><td\\s*class=desc>(.+?)<");
        if (matcher.find()) {
            Long a = PlugUtils.getFileSizeFromString(matcher.group(1));
            logger.info("File size " + a);
            httpFile.setFileSize(a);
            httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
        } else logger.warning("File size not found");
    }


    private PostMethod stepCaptcha(String contentAsString) throws Exception {
        if (contentAsString.contains("captcha")) {
            CaptchaSupport captchaSupport = getCaptchaSupport();
            Matcher matcher = PlugUtils.matcher("class=captcha src=\"(.+?)\"", contentAsString);
            if (matcher.find()) {
                String s = matcher.group(1);
                logger.info("Captcha URL " + s);
                String captcha = captchaSupport.getCaptcha(WEB + s);

                if (captcha == null) {
                    throw new CaptchaEntryInputMismatchException();
                } else {

                    final PostMethod postMethod = getPostMethod(fileURL);
                    postMethod.addParameter("submitted", "1");
                    postMethod.addParameter("str", captcha);
                    return postMethod;

                }
            } else {
                logger.warning(contentAsString);
                throw new PluginImplementationException("Captcha picture was not found");
            }
        }
        return null;
    }

    private void checkProblems() throws ServiceConnectionProblemException, YouHaveToWaitException, URLNotAvailableAnymoreException {
        if (getContentAsString().contains("red;\">Neplatn")) {
            throw new URLNotAvailableAnymoreException(String.format("Neplatný odkaz"));
        }
    }

}