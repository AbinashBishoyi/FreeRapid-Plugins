package cz.vity.freerapid.plugins.services.oron;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.recaptcha.ReCaptcha;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author Thumb
 */
class OronFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(OronFileRunner.class.getName());


    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        addCookie(new Cookie(".oron.com", "lang", "english", "/", 86400, false));
        final GetMethod getMethod = getGetMethod(fileURL);//make first request
        if (makeRedirectedRequest(getMethod)) {
            checkFileProblems();
            checkNameAndSize();//ok let's extract file name and size from the page
        } else {
            checkFileProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void unimplemented() throws PluginImplementationException {
        logger.warning(getContentAsString());//log the info
        throw new PluginImplementationException();//some unknown problem
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        Matcher name_m = getMatcherAgainstContent("Filename: <[^<>]*>([^<]+)<");
        if (!name_m.find())
            unimplemented();
        httpFile.setFileName(name_m.group(1));

        PlugUtils.checkFileSize(httpFile, getContentAsString(), "ize: ", "<");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);

        runCheck();
        checkDownloadProblems();

        final HttpMethod method = getMethodBuilder()
                .setReferer(fileURL)
                .setActionFromFormWhereTagContains("method_free", true)
                .setAction(fileURL)
                .setParameter("method_free", " Regular Download ")
                .toPostMethod();

        if (!makeRedirectedRequest(method))
            throw new ServiceConnectionProblemException();
        checkProblems();

        while (getContentAsString().contains("recaptcha")) {
            if (!makeRedirectedRequest(stepCaptcha()))
                throw new ServiceConnectionProblemException();
            checkProblems();
        }

        final HttpMethod method3 = getMethodBuilder()
                .setReferer(fileURL)
                .setActionFromAHrefWhereATagContains("Download")
                .toGetMethod();

        if (!tryDownloadAndSaveFile(method3)) {
            checkProblems();//if downloading failed
            unimplemented();
        }
    }

    private void checkFileProblems() throws ErrorDuringDownloadingException {
        if (getContentAsString().contains("Status: <b class=\"ok\">Premium Only</b><br>")
                || getContentAsString().contains("This file can only be downloaded by Premium Users")) {
            throw new URLNotAvailableAnymoreException("This file can only be downloaded by Premium Users");
        }
        Matcher err_m = getMatcherAgainstContent("<p class=\"err\">([^<>]+)<");
        if (err_m.find()) {
            if (err_m.group(1).contains("No such file"))
                throw new URLNotAvailableAnymoreException("File not found");
        }
        if (getContentAsString().contains("<b>File Not Found</b>")
                || getContentAsString().contains("<meta NAME=\"description\" CONTENT=\"ORON.com - File Not Found\">")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        if (getContentAsString().contains("Expired session")) {
            throw new ServiceConnectionProblemException("Expired session");
        }
    }

    private void checkDownloadProblems() throws ErrorDuringDownloadingException {
        Matcher err_m = getMatcherAgainstContent("<p class=\"err\">([^<>]+)<");
        if (err_m.find()) {
            Matcher matcher = getMatcherAgainstContent("You have to wait (?:(\\d+?) hours?)?(?:, )?(?:(\\d+?) minutes?)?(?:, )?(?:(\\d+?) seconds?)? until the next download becomes available");
            if (matcher.find()) {
                final String hours = matcher.group(1);
                final String minutes = matcher.group(2);
                final String seconds = matcher.group(3);
                throw new YouHaveToWaitException(matcher.group(),
                        (hours == null ? 0 : (Integer.parseInt(hours) * 3600))
                                + (minutes == null ? 0 : (Integer.parseInt(minutes) * 60))
                                + (seconds == null ? 0 : (Integer.parseInt(seconds))));
            }
            unimplemented();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        checkFileProblems();
        checkDownloadProblems();
    }

    private HttpMethod stepCaptcha() throws Exception {
        MethodBuilder request = getMethodBuilder()
                .setReferer(fileURL)
                .setActionFromFormByName("F1", true)
                .setAction(fileURL)
                .setParameter("method_free", " Regular Download ");

        Matcher m = getMatcherAgainstContent("/recaptcha/api/challenge\\?k=([^\"]+)\"");
        if (!m.find()) throw new PluginImplementationException("ReCaptcha key not found");

        ReCaptcha r = new ReCaptcha(m.group(1), client);
        String imageURL = r.getImageURL();
        CaptchaSupport cs = getCaptchaSupport();
        String captcha = cs.getCaptcha(imageURL);
        if (captcha == null) throw new CaptchaEntryInputMismatchException();
        r.setRecognized(captcha);

        return r.modifyResponseMethod(request).toPostMethod();
    }

}
