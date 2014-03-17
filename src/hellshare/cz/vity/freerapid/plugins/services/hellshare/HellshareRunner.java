package cz.vity.freerapid.plugins.services.hellshare;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author Ladislav Vitasek, Ludek Zika, ntoskrnl
 */
class HellshareRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(HellshareRunner.class.getName());
    private final static Map<String, MethodBuilder> methodsMap = new HashMap<String, MethodBuilder>();
    private final static int WAIT_TIME = 20;
    private final static int CAPTCHA_MAX = 5;
    private int captchaCounter = 1;

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final HttpMethod getMethod = getMethodBuilder().setAction(fileURL).toHttpMethod();
        if (makeRedirectedRequest(getMethod)) {
            checkNameAndSize(getContentAsString());
            checkCaptcha();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        if (checkInQueue()) return;
        final HttpMethod getMethod = getMethodBuilder().setAction(fileURL).toHttpMethod();
        if (makeRedirectedRequest(getMethod)) {

            checkNameAndSize(getContentAsString());

            if (getContentAsString().contains("100%,"))
                throw new YouHaveToWaitException("Na serveru jsou vyu\u017Eity v\u0161echny free download sloty", WAIT_TIME);
            final HttpMethod captchaPageMethod = getCaptchaPage();
            if (makeRedirectedRequest(captchaPageMethod)) {
                client.setReferer("http://www.hellshare.com" + captchaPageMethod.getPath());
                HttpMethod method = stepCaptcha();
                if (!tryDownloadAndSaveFile(method)) {
                    checkProblems();
                    if (getContentAsString().contains("We are sorry, but this file is not accessible at this time for this country"))
                        throw new NotRecoverableDownloadException("This file is not available for this country for this moment");
                    boolean finish = false;
                    while (!finish) {
                        client.setReferer("http://www.hellshare.com" + method.getPath());
                        method = stepCaptcha();
                        finish = tryDownloadAndSaveFile(method);
                    }
                }
            } else {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }


        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, content, "<strong id=\"FileName_master\">", "</strong>");
        httpFile.setFileSize(PlugUtils.getFileSizeFromString(PlugUtils.getStringBetween(content, "<strong id=\"FileSize_master\">", "</strong>").replace("&nbsp;", " ")));
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private void checkCaptcha() throws Exception {
        if (getContentAsString().contains("100%,"))
            return;

        final HttpMethod captchaPageMethod = getCaptchaPage();
        if (makeRedirectedRequest(captchaPageMethod)) {
            client.setReferer("http://www.hellshare.com" + captchaPageMethod.getPath());
            stepCaptcha();

        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }


    }

    private boolean checkInQueue() throws Exception {
        if (!methodsMap.containsKey(fileURL))
            return false;

        logger.info("File is in queue");
        final MethodBuilder met = methodsMap.get(fileURL);
        final HttpMethod method = met.toPostMethod();

        logger.info("File is in queue, trying to download");
        if (tryDownloadAndSaveFile(method))
            methodsMap.remove(fileURL);
        else {
            logger.info("Download from queue failed");
            checkProblems();
            if (getContentAsString().contains("We are sorry, but this file is not accessible at this time for this country"))
                throw new NotRecoverableDownloadException("This file is not available for this country for this moment");

            if (getContentAsString().contains("img id=\"captcha-img\""))
                stepCaptcha();

            throw new YouHaveToWaitException("Na serveru jsou vyu\u017Eity v\u0161echny free download sloty", WAIT_TIME);
        }

        return true;
    }

    private HttpMethod stepCaptcha() throws Exception {
        if (!getContentAsString().contains("captcha")) {
            throw new YouHaveToWaitException("Neur\u010Dit\u00E9 omezen\u00ED", 4 * WAIT_TIME);
        }
        final String captchaURL = getMethodBuilder().setActionFromImgSrcWhereTagContains("captcha").getAction();
        final CaptchaSupport captchaSupport = getCaptchaSupport();
        final String captcha;
        if (captchaCounter <= CAPTCHA_MAX) {
            final BufferedImage captchaImage = prepareCaptchaImage(captchaSupport.getCaptchaImage(captchaURL));
            captcha = new CaptchaRecognizer().recognize(captchaImage);
            logger.info("Attempt " + captchaCounter + " of " + CAPTCHA_MAX + ", OCR recognized " + captcha);
            captchaCounter++;
        } else {
            captcha = captchaSupport.getCaptcha(captchaURL);
            if (captcha == null) throw new CaptchaEntryInputMismatchException();
            logger.info("Manual captcha " + captcha);
        }

        final MethodBuilder method = getMethodBuilder().setActionFromFormByIndex(1, true).setParameter("captcha", captcha);

        logger.info("Adding file to map, final URL: " + method.getEscapedURI());
        methodsMap.put(fileURL, method);
        return method.toPostMethod();
    }

    private BufferedImage prepareCaptchaImage(final BufferedImage input) {
        final BufferedImage output = new BufferedImage(input.getWidth(), input.getHeight(), BufferedImage.TYPE_BYTE_BINARY);
        final Graphics g = output.getGraphics();
        g.setXORMode(Color.WHITE);
        g.drawImage(input, 0, 0, null);
        return output;//almost too simple :)
    }

    private HttpMethod getCaptchaPage() throws ErrorDuringDownloadingException {
        Matcher matcher = getMatcherAgainstContent("button-download-free\" href=\"([^\"]+)\">St");
        if (!matcher.find()) {
            checkProblems();
            logger.info(getContentAsString());
            throw new PluginImplementationException();
        }
        String downURL = matcher.group(1);
        return getMethodBuilder().setAction(downURL).toHttpMethod();
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        String content = getContentAsString();
        if (content.contains("Soubor nenalezen")) {
            throw new URLNotAvailableAnymoreException(String.format("<b>Soubor nenalezen</b><br>"));
        }
        if (content.contains(" free download|Na serveri")) {
            throw new YouHaveToWaitException("Na serveru jsou vyu\u017Eity v\u0161echny free download sloty", WAIT_TIME);
        }
        if (content.contains("Stahujete soub")) {
            throw new YouHaveToWaitException("Na serveru jsou vyu\u017Eity v\u0161echny free download sloty", WAIT_TIME);
        }
    }
}