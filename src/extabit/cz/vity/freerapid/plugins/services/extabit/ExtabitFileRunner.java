package cz.vity.freerapid.plugins.services.extabit;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.io.IOException;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author Thumb, ntoskrnl
 */
class ExtabitFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(ExtabitFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        if (makeFirstPossiblyRedirectedRequest()) {
            checkFileProblems();
            checkNameAndSize();//ok let's extract file name and size from the page
        } else {
            checkFileProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private boolean makeFirstPossiblyRedirectedRequest() throws IOException {
        final GetMethod getMethod = getGetMethod(fileURL);//make first request
        addCookie(new Cookie("extabit.com", "language", "en", "/", 86400, false));
        if (!makeRequest(getMethod)) {
            if (getMethod.getStatusCode() / 100 == 3) {
                fileURL = getMethod.getResponseHeader("Location").getValue();
                logger.info(String.format("Redirected, changing URL to %s", fileURL));
                GetMethod m2 = getGetMethod(fileURL);
                return makeRequest(m2);
            }
            return false;
        }
        return true;
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        final Matcher nameMatcher = getMatcherAgainstContent("<div id=\"download_filename\"[^<>]*?>\\s*(.+?)\\s*</div>");
        if (!nameMatcher.find())
            unimplemented();
        httpFile.setFileName(nameMatcher.group(1));

        final Matcher sizeMatcher = getMatcherAgainstContent("<div class=\"download_filesize_en\">\\s*\\[(.+?)\\]\\s*</div>");
        if (!sizeMatcher.find())
            unimplemented();
        httpFile.setFileSize(PlugUtils.getFileSizeFromString(sizeMatcher.group(1)));

        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        runCheck();
        checkDownloadProblems();

        while (getContentAsString().contains("cmn_form")) {
            final HttpMethod req2 = getMethodBuilder()
                    .setActionFromFormByName("cmn_form", true)
                    .setParameter("capture", getCaptchaCode())
                    .setBaseURL("http://extabit.com/")
                    .toPostMethod();
            if (!makeRequest(req2))
                throw new ServiceConnectionProblemException();
            checkProblems();
        }

        final HttpMethod req4 = getMethodBuilder()
                .setActionFromAHrefWhereATagContains("click here to download")
                .toGetMethod();

        if (!tryDownloadAndSaveFile(req4)) {
            checkProblems();//if downloading failed
            logger.warning(getContentAsString());//log the info

            /* Since the server doesn't properly indicate
             * multiple downloads condition, just assume it here
             */
            throw new ServiceConnectionProblemException("Unknown problem - will try again");
        }
    }

    private String getCaptchaCode() throws ErrorDuringDownloadingException {
        final CaptchaSupport cs = getCaptchaSupport();
        final Matcher captchaMatcher = PlugUtils.matcher("<img src=\"(/cap[^\"]*)\"", getContentAsString());
        if (!captchaMatcher.find())
            unimplemented();

        final String captcha = cs.getCaptcha("http://extabit.com" + captchaMatcher.group(1));
        if (captcha == null)
            throw new CaptchaEntryInputMismatchException();

        return captcha;
    }

    private void unimplemented() throws PluginImplementationException {
        //logger.warning(getContentAsString());//PluginImplementationException automatically logs the page content
        throw new PluginImplementationException();//some unknown problem
    }

    private void checkFileProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("<h1>File not found</h1>")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }

    private void checkDownloadProblems() {
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        checkFileProblems();
        checkDownloadProblems();
    }

}