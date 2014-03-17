package cz.vity.freerapid.plugins.services.hellshare;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author Ladislav Vitasek, Ludek Zika
 */
class HellshareRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(HellshareRunner.class.getName());
    private final static Map<String, MethodBuilder> methodsMap = new HashMap<String, MethodBuilder>();
    private final static int WAIT_TIME = 20;


    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final HttpMethod getMethod = getMethodBuilder().setAction(fileURL).toHttpMethod();
        if (makeRedirectedRequest(getMethod)) {
            checkNameAndSize(getContentAsString());
            checkCaptcha();
        } else
            throw new PluginImplementationException();
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


        } else
            throw new ServiceConnectionProblemException();
    }

    private void checkNameAndSize(String content) throws Exception {
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
        String img = getMethodBuilder().setActionFromImgSrcWhereTagContains("captcha").getAction();
        boolean emptyCaptcha;
        String captcha;
        do {
            logger.info("Captcha image " + img);
            captcha = getCaptchaSupport().getCaptcha(img);
            if (captcha == null) {
                throw new CaptchaEntryInputMismatchException();
            }
            if (captcha.equals("")) {
                emptyCaptcha = true;
                img = img + "1";
            } else emptyCaptcha = false;
        } while (emptyCaptcha);


        final MethodBuilder method = getMethodBuilder().setActionFromFormByIndex(1, true).setParameter("captcha", captcha);

        logger.info("Adding file to map, final URL: " + method.getEscapedURI());
        methodsMap.put(fileURL, method);
        return method.toPostMethod();
    }

    private HttpMethod getCaptchaPage() throws ServiceConnectionProblemException, YouHaveToWaitException, URLNotAvailableAnymoreException, PluginImplementationException {
        Matcher matcher = getMatcherAgainstContent("button-download-free\" href=\"([^\"]+)\">St");
        if (!matcher.find()) {
            checkProblems();
            logger.info(getContentAsString());
            throw new PluginImplementationException();
        }
        String downURL = matcher.group(1);
        return getMethodBuilder().setAction(downURL).toHttpMethod();
    }

    private void checkProblems() throws ServiceConnectionProblemException, YouHaveToWaitException, URLNotAvailableAnymoreException {
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