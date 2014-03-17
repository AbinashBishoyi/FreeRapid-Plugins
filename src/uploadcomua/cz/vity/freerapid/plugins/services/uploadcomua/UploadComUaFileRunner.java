package cz.vity.freerapid.plugins.services.uploadcomua;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 */
class UploadComUaFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(UploadComUaFileRunner.class.getName());
    private final static String SERVICE_WEB = "http://www.upload.com.ua/";
    private final int captchaMax = 0;
    private int captchaCounter = 1;

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
        final Matcher matcher = getMatcherAgainstContent("<div class=\"file\"><img src=\".+?\"><a href=\".+?\" title=\".+?\">(.+?)</a></div>");
        if (!matcher.find()) throw new PluginImplementationException("File name not found");
        httpFile.setFileName(matcher.group(1));

        PlugUtils.checkFileSize(httpFile, getContentAsString(), "<div class=\"file_size\">", "</div>");

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

            //go to captcha page
            HttpMethod httpMethod = getMethodBuilder()
                    .setReferer(fileURL)
                    .setAction(fileURL)
                    .setParameter("mode", "free")
                    .toPostMethod();
            if (!makeRedirectedRequest(httpMethod)) throw new ServiceConnectionProblemException();

            //solve captcha and go to waiting page
            while (getContentAsString().contains("/confirm.php")) {
                downloadTask.sleep(4);//slow down a bit, otherwise captcha may not be accepted
                if (!makeRedirectedRequest(stepCaptcha())) throw new ServiceConnectionProblemException();
            }
            logger.info("Captcha OK");

            //find and parse download link
            Matcher matcher = Pattern.compile("new Array\\((.+?)\\);", Pattern.DOTALL).matcher(getContentAsString());
            if (!matcher.find()) throw new PluginImplementationException("Download link not found");
            String action = matcher.group(1).replaceAll("(?:\\s|\"|'|,)", "");
            if (!action.contains("|")) throw new PluginImplementationException("Problem parsing download link");
            final String[] s = action.split("\\|");
            if (s.length != 2) throw new PluginImplementationException("Problem parsing download link");
            final String server = s[0];
            action = s[1] + "/" + PlugUtils.getStringBetween(getContentAsString(), "id=\"os_filename\" value=\"", "\">");
            action = "http://dl" + server + ".upload.com.ua/" + action;

            logger.info("Extracted download link " + action);
            httpMethod = getMethodBuilder().setReferer(fileURL).setAction(action).toGetMethod();

            //find waiting time, parse it and wait
            matcher = getMatcherAgainstContent("var .*?timer.*? =((?: \\d+?(?: \\+)?)+?);");
            if (!matcher.find()) throw new PluginImplementationException("Waiting time not found");
            final String waitString = matcher.group(1);
            logger.info("Waiting time string to parse " + waitString);
            final String[] waitTimes = waitString.replace(" ", "").split("\\+");
            int finalWaitTime = 0;
            for (final String waitTime : waitTimes) {
                finalWaitTime += Integer.parseInt(waitTime);
            }
            logger.info("Parsed waiting time " + finalWaitTime);
            downloadTask.sleep(finalWaitTime + 1);

            //download
            if (!tryDownloadAndSaveFile(httpMethod)) {
                checkProblems();
                logger.warning(getContentAsString());
                throw new ServiceConnectionProblemException("Error starting download");
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        if (content.contains("\u0421\u0442\u0440\u0430\u043D\u0438\u0446\u0430\u0020\u043D\u0435\u0020\u043D\u0430\u0439\u0434\u0435\u043D\u0430")
                || content.contains("\u0424\u0430\u0439\u043B\u0020\u043D\u0435\u0020\u043D\u0430\u0439\u0434\u0435\u043D")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        if (content.contains("\u0443\u0436\u0435\u0020\u043F\u0440\u043E\u0438\u0437\u0432\u043E\u0434\u044F\u0442\u0441\u044F\u0020" +
                "\u0441\u043A\u0430\u0447\u0438\u0432\u0430\u043D\u0438\u044F\u0020\u0444\u0430\u0439\u043B\u0430")) {
            throw new ServiceConnectionProblemException("Your IP address is already downloading a file");
        }
    }

    private HttpMethod stepCaptcha() throws ErrorDuringDownloadingException {
        final CaptchaSupport captchaSupport = getCaptchaSupport();
        final String captchaSrc = SERVICE_WEB + "confirm.php";
        //logger.info("Captcha URL " + captchaSrc);

        String captcha;
        if (captchaCounter <= captchaMax) {
            captcha = PlugUtils.recognize(captchaSupport.getCaptchaImage(captchaSrc), "-d -1 -C F-0-9");
            logger.info("OCR attempt " + captchaCounter + " of " + captchaMax + ", recognized " + captcha);
            captchaCounter++;
        } else {
            captcha = captchaSupport.getCaptcha(captchaSrc);
            if (captcha == null) throw new CaptchaEntryInputMismatchException();
            logger.info("Manual captcha " + captcha);
        }

        return getMethodBuilder()
                .setReferer(fileURL)
                .setActionFromFormWhereTagContains("src=\"/images/go.gif\"", true)
                .setParameter("code", captcha)
                .toPostMethod();
    }

}