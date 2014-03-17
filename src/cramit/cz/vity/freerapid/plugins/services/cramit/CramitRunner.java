package cz.vity.freerapid.plugins.services.cramit;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.recaptcha.ReCaptcha;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author RickCL
 */
public class CramitRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(CramitRunner.class.getName());

    @Override
    public void run() throws Exception {
        super.run();

        logger.info("Starting run task " + fileURL);
        HttpMethod httpMethod = getGetMethod(fileURL);

        if (makeRedirectedRequest(httpMethod)) {
            checkProblems();
            checkNameAndSize();

            httpMethod = getMethodBuilder()
                    .setReferer(fileURL)
                    .setActionFromFormWhereTagContains("method_free", true)
                            //this parameter may contains spaces, the above method doesn't work in such cases
                    .setParameter("fname", PlugUtils.getStringBetween(getContentAsString(), "name=\"fname\" value=\"", "\""))
                    .setAction(fileURL)
                    .setEncodePathAndQuery(true)
                    .toPostMethod();
            if (!makeRedirectedRequest(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            checkProblems();

            Matcher matcher = getMatcherAgainstContent("Wait.*\">(\\d+?)</span>");
            if (!matcher.find()) {
                throw new PluginImplementationException("Waiting time not found");
            }
            final int wait = Integer.parseInt(matcher.group(1)) + 1;

            while (getContentAsString().contains("recaptcha")) {
                httpMethod = stepCaptcha();
                downloadTask.sleep(wait);
                if (!makeRedirectedRequest(httpMethod)) {
                    checkProblems();
                    throw new ServiceConnectionProblemException();
                }
                checkProblems();
            }

            httpMethod = getMethodBuilder().setReferer(fileURL).setActionFromFormWhereTagContains("Click to download", true).toGetMethod();
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

    @Override
    public void runCheck() throws Exception {
        super.runCheck();

        final HttpMethod httpMethod = getGetMethod(fileURL);

        if (makeRedirectedRequest(httpMethod)) {
            checkProblems();
            checkNameAndSize();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize() throws Exception {
        final String content = getContentAsString();
        PlugUtils.checkName(httpFile, content, "Download File ", "</h2>");
        PlugUtils.checkFileSize(httpFile, content, "</font> (", ")</font>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        if (content.contains("File Not Found") || content.contains("No such file with this filename")) {
            throw new URLNotAvailableAnymoreException("The requested file was not found");
        }
        final Matcher matcher = getMatcherAgainstContent("You have to wait (?:(\\d+?) hours?)?(?:, )?(?:(\\d+?) minutes?)?(?:, )?(?:(\\d+?) seconds?)? till next download");
        if (matcher.find()) {
            final String hours = matcher.group(1);
            final String minutes = matcher.group(2);
            final String seconds = matcher.group(3);
            throw new YouHaveToWaitException(matcher.group(),
                    (hours == null ? 0 : (Integer.parseInt(hours) * 3600))
                            + (minutes == null ? 0 : (Integer.parseInt(minutes) * 60))
                            + (seconds == null ? 0 : (Integer.parseInt(seconds))));
        }
    }

    private HttpMethod stepCaptcha() throws Exception {
        final Matcher m = getMatcherAgainstContent("api.recaptcha.net/noscript\\?k=([^\"]+)\"");
        if (!m.find()) throw new PluginImplementationException("ReCaptcha key not found");
        final String reCaptchaKey = m.group(1);

        final String content = getContentAsString();
        final ReCaptcha r = new ReCaptcha(reCaptchaKey, client);
        final CaptchaSupport captchaSupport = getCaptchaSupport();

        final String captchaURL = r.getImageURL();
        logger.info("Captcha URL " + captchaURL);

        final String captcha = captchaSupport.getCaptcha(captchaURL);
        if (captcha == null) throw new CaptchaEntryInputMismatchException();
        r.setRecognized(captcha);

        return r.modifyResponseMethod(getMethodBuilder(content).setReferer(fileURL).setActionFromFormWhereTagContains("method_free", true).setAction(fileURL)).toPostMethod();
    }

}
