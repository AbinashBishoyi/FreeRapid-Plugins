package cz.vity.freerapid.plugins.services.shragle;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.recaptcha.ReCaptcha;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 */
class ShragleFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(ShragleFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        addCookie(new Cookie(".shragle.com", "lang", "en_GB", "/", 86400, false));
        final HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        Matcher matcher = getMatcherAgainstContent("(<h2[^<>]*?>.+?</h2>)");
        if (!matcher.find()) {
            throw new PluginImplementationException("File name/size not found (1)");
        }
        matcher = PlugUtils.matcher(">([^<>]+?)<", matcher.group(1));
        if (!matcher.find()) {
            throw new PluginImplementationException("File name/size not found (2)");
        }
        httpFile.setFileName(matcher.group(1).trim());
        if (!matcher.find()) {
            throw new PluginImplementationException("File name/size not found (3)");
        }
        httpFile.setFileSize(PlugUtils.getFileSizeFromString(matcher.group(1).trim()));
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        addCookie(new Cookie(".shragle.com", "lang", "en_GB", "/", 86400, false));
        HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize();
            final Matcher matcher = getMatcherAgainstContent("Please wait more (\\d+) minutes");
            if (matcher.find()) {
                throw new YouHaveToWaitException("Download limit reached", 60 * Integer.parseInt(matcher.group(1)));
            }
            final String content = getContentAsString();
            downloadTask.sleep(PlugUtils.getNumberBetween(content, "var downloadWait =", ";") + 1);
            while (true) {
                if (tryDownloadAndSaveFile(stepCaptcha(content))) {
                    break;
                } else {
                    if (getContentAsString().contains("Sie haben den Sicherheitscode falsch eingegeben")) {
                        continue;
                    }
                    checkProblems();
                    throw new ServiceConnectionProblemException("Error starting download");
                }
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        if (getContentAsString().contains("The selected file was not found")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    private HttpMethod stepCaptcha(final String content) throws Exception {
        final Matcher reCaptchaKeyMatcher = PlugUtils.matcher("recaptcha/api/challenge\\?k=(.+?)\">", content);
        if (!reCaptchaKeyMatcher.find()) {
            throw new PluginImplementationException("ReCaptcha key not found");
        }
        final String reCaptchaKey = reCaptchaKeyMatcher.group(1);
        final ReCaptcha r = new ReCaptcha(reCaptchaKey, client);
        final String captcha = getCaptchaSupport().getCaptcha(r.getImageURL());
        if (captcha == null) {
            throw new CaptchaEntryInputMismatchException();
        }
        r.setRecognized(captcha);
        return r.modifyResponseMethod(getMethodBuilder(content)
                .setReferer(fileURL)
                .setActionFromFormByName("download", true)
        ).toPostMethod();
    }

}