package cz.vity.freerapid.plugins.services.getthebit;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 */
class GetTheBitFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(GetTheBitFileRunner.class.getName());
    private int captchaCounter = 1, captchaMax = 5;

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        final Matcher matcher = getMatcherAgainstContent("<title>GettheBit.Com - (.+?) \\((.+?)\\)</title>");
        if (!matcher.find()) throw new PluginImplementationException("File name/size not found");
        httpFile.setFileName(matcher.group(1));
        httpFile.setFileSize(PlugUtils.getFileSizeFromString(matcher.group(2)));
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize();

            Matcher matcher = getMatcherAgainstContent("<p class=\"free\"><a href=\"(http://.+?)\">[^<>]+?</a>");
            if (!matcher.find()) throw new PluginImplementationException("First download link not found");
            HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL).setAction(matcher.group(1)).toGetMethod();
            if (makeRedirectedRequest(httpMethod)) {

                final MethodBuilder mb = getMethodBuilder().setReferer(fileURL).setActionFromIFrameSrcWhereTagContains("main_frame");
                final String frameURL = mb.getAction();//we need this later as referer
                logger.info("Frame URL " + frameURL);
                httpMethod = mb.toGetMethod();
                if (!makeRedirectedRequest(httpMethod)) {
                    throw new ServiceConnectionProblemException();
                }

                if (getContentAsString().contains("<a href\"" + fileURL + "\" target=\"_top\">")) {
                    throw new YouHaveToWaitException("You are already downloading a file. Retrying...", 60);
                }

                while (getContentAsString().contains("var wtime")) {
                    downloadTask.sleep(PlugUtils.getNumberBetween(getContentAsString(), "var wtime = ", ";") + 1);
                    if (!makeRedirectedRequest(httpMethod)) { //refresh page after waiting
                        throw new ServiceConnectionProblemException();
                    }
                }

                if (getContentAsString().contains("kcapcha")) {
                    while (getContentAsString().contains("kcapcha")) {
                        httpMethod = stepCaptcha(frameURL);
                        if (!makeRedirectedRequest(httpMethod)) {
                            throw new ServiceConnectionProblemException("Error posting captcha");
                        }
                    }
                } else {
                    throw new PluginImplementationException("Captcha not found");
                }
                logger.info("Captcha OK");

                matcher = getMatcherAgainstContent("<p>[^<>]+?<a href=\"(http://.+?)\" target=\"_top\"");
                if (!matcher.find()) throw new PluginImplementationException("Final download link not found");
                httpMethod = getMethodBuilder().setReferer(frameURL).setAction(matcher.group(1)).toGetMethod();
                if (!tryDownloadAndSaveFile(httpMethod)) {
                    throw new ServiceConnectionProblemException("Error downloading file");
                }

            } else {
                throw new ServiceConnectionProblemException();
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        if (content.contains("<title>Getthebit Easy File-Share system...</title>")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    private HttpMethod stepCaptcha(final String referer) throws ErrorDuringDownloadingException {
        final CaptchaSupport captchaSupport = getCaptchaSupport();
        final String captchaSrc = getMethodBuilder().setActionFromImgSrcWhereTagContains("kcapcha").getAction();
        logger.info("Captcha URL " + captchaSrc);

        String captcha;
        if (captchaCounter <= captchaMax) {
            //the captchas are really tough, but might just leave this in anyway...
            captcha = PlugUtils.recognize(captchaSupport.getCaptchaImage(captchaSrc), "-d -1 -C a-z-0-9");
            logger.info("OCR attempt " + captchaCounter + " of " + captchaMax + ", recognized " + captcha);
            captchaCounter++;
        } else {
            captcha = captchaSupport.getCaptcha(captchaSrc);
            if (captcha == null) throw new CaptchaEntryInputMismatchException();
            logger.info("Manual captcha " + captcha);
        }

        return getMethodBuilder().setReferer(referer).setActionFromFormWhereTagContains("kcapcha", true).setParameter("kcapcha", captcha).toPostMethod();
    }

}