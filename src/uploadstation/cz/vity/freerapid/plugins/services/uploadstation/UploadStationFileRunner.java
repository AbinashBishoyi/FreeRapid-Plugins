package cz.vity.freerapid.plugins.services.uploadstation;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.recaptcha.ReCaptcha;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author ntoskrnl, Abinash Bishoyi
 */
class UploadStationFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(UploadStationFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
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
        PlugUtils.checkName(httpFile, getContentAsString(), "<div class=\"download_item\">", "</div>");
        PlugUtils.checkFileSize(httpFile, getContentAsString(), "File size: <b>", "</b>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize();
            final String reCaptchaKey = PlugUtils.getStringBetween(getContentAsString(), "var reCAPTCHA_publickey='", "';");
            final String recaptchaShortencodeField = PlugUtils.getParameter("recaptcha_shortencode_field", getContentAsString());

            method = getMethodBuilder().setReferer(fileURL).setAction(fileURL).setParameter("checkDownload", "check").toPostMethod();
            method.setRequestHeader("X-Requested-With", "XMLHttpRequest");
            if (!makeRedirectedRequest(method)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            checkProblems();

            do {
                final ReCaptcha r = new ReCaptcha(reCaptchaKey, client);
                final String captcha = getCaptchaSupport().getCaptcha(r.getImageURL());
                if (captcha == null) {
                    throw new CaptchaEntryInputMismatchException();
                }
                r.setRecognized(captcha);
                method = r.modifyResponseMethod(getMethodBuilder().setReferer(fileURL).setAction("/checkReCaptcha.php"))
                        .setParameter("recaptcha_shortencode_field", recaptchaShortencodeField).toPostMethod();
                method.setRequestHeader("X-Requested-With", "XMLHttpRequest");
                if (!makeRedirectedRequest(method)) {
                    checkProblems();
                    throw new ServiceConnectionProblemException();
                }
                checkProblems();
            } while (!PlugUtils.find("\"success\"\\s*:\\s*1", getContentAsString()));

            final String data = makeDownloadLinkRequest("wait");
            final Matcher matcher = PlugUtils.matcher("(\\d+)", data);
            if (!matcher.find()) {
                throw new PluginImplementationException("Error parsing wait time");
            }
            downloadTask.sleep(Integer.parseInt(matcher.group(1)) + 1);

            makeDownloadLinkRequest("show");
            method = getMethodBuilder().setReferer(fileURL).setAction(fileURL).setParameter("download", "normal").toPostMethod();
            if (!tryDownloadAndSaveFile(method)) {
                checkProblems();
                throw new ServiceConnectionProblemException("Error starting download");
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private String makeDownloadLinkRequest(final String request) throws Exception {
        final HttpMethod method = getMethodBuilder().setReferer(fileURL).setAction(fileURL).setParameter("downloadLink", request).toPostMethod();
        method.setRequestHeader("X-Requested-With", "XMLHttpRequest");
        if (!makeRedirectedRequest(method)) {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
        checkProblems();
        final String data = getContentAsString().trim();
        if (data.contains("fail")) {
            throw new ServiceConnectionProblemException("Server error");
        }
        return data;
    }

    private void checkProblems() throws Exception {
        final String content = getContentAsString();
        if (content.contains("The file could not be found") || content.contains("Page not found") || content.contains("File not available") || content.contains("File is not available")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        if (PlugUtils.find("\"fail\"\\s*:\\s*\"timeLimit\"", content)) {
            final HttpMethod method = getMethodBuilder().setReferer(fileURL).setAction(fileURL).setParameter("checkDownload", "showError").setParameter("errorType", "timeLimit").toPostMethod();
            if (!makeRedirectedRequest(method)) {
                throw new ServiceConnectionProblemException();
            }
        }
        Matcher matcher = getMatcherAgainstContent("You (?:have|need) to wait (\\d+) seconds to download next file");
        if (matcher.find()) {
            throw new YouHaveToWaitException(matcher.group(), Integer.parseInt(matcher.group(1)) + 5);
        }
    }

}