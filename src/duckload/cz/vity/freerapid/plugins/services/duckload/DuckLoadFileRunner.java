package cz.vity.freerapid.plugins.services.duckload;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.recaptcha.ReCaptcha;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethod;

import java.net.URLDecoder;
import java.util.Random;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 */
class DuckLoadFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(DuckLoadFileRunner.class.getName());
    private final static Random random = new Random();

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        addCookie(new Cookie(".duckload.com", "dl_set_lang", "en", "/", 86400, false));
        final HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize() throws Exception {
        Matcher matcher = PlugUtils.matcher("/([^/]+)$", fileURL);
        if (!matcher.find()) {
            throw new PluginImplementationException("File name not found");
        }
        httpFile.setFileName(URLDecoder.decode(matcher.group(1), "UTF-8"));
        matcher = getMatcherAgainstContent("\\(<i>(.+?)</i> <strong>(.+?)</strong>\\)");
        if (!matcher.find()) {
            throw new PluginImplementationException("File size not found");
        }
        httpFile.setFileSize(PlugUtils.getFileSizeFromString(matcher.group(1) + matcher.group(2)));
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        addCookie(new Cookie(".duckload.com", "dl_set_lang", "en", "/", 86400, false));
        addGACookies();
        HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize();
            fileURL = method.getURI().toString();
            while (true) {
                final String content = getContentAsString();
                final String reCaptchaKey = PlugUtils.getStringBetween(content, "noscript?k=", "\"");
                final ReCaptcha r = new ReCaptcha(reCaptchaKey, client);
                final String captcha = getCaptchaSupport().getCaptcha(r.getImageURL());
                if (captcha == null) {
                    throw new CaptchaEntryInputMismatchException();
                }
                r.setRecognized(captcha);
                method = r.modifyResponseMethod(
                        getMethodBuilder(content)
                                .setReferer(fileURL)
                                .setAction(fileURL)
                                .setParameter("free_dl", "")
                ).toPostMethod();
                if (tryDownloadAndSaveFile(method)) {
                    break;
                }
                if (!getContentAsString().contains("code entered is incorrect")) {
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
        final String content = getContentAsString();
        if (content.contains("File not found")
                || content.contains("Datei wurde nicht gefunden")
                || content.contains("<h1>404 - Not Found</h1>")
                || content.contains("download.notfound")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    private void addGACookies() {
        addCookie(new Cookie(".duckload.com", "PHPSESSID", String.valueOf(random.nextLong()), "/", 86400, false));
        addCookie(new Cookie(".duckload.com", "__utma", String.valueOf(random.nextLong()), "/", 86400, false));
        addCookie(new Cookie(".duckload.com", "__utmb", String.valueOf(random.nextLong()), "/", 86400, false));
        addCookie(new Cookie(".duckload.com", "__utmc", String.valueOf(random.nextLong()), "/", 86400, false));
        addCookie(new Cookie(".duckload.com", "__utmz", String.valueOf(random.nextLong()), "/", 86400, false));
    }

}