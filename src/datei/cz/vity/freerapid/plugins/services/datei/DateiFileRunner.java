package cz.vity.freerapid.plugins.services.datei;

import cz.vity.freerapid.plugins.exceptions.CaptchaEntryInputMismatchException;
import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.services.recaptcha.ReCaptcha;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;

import java.util.logging.Logger;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 */
class DateiFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(DateiFileRunner.class.getName());

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
        final String content = getContentAsString();
        PlugUtils.checkName(httpFile, content, "<td colspan=\"2\"><strong>", "</strong></td>");
        PlugUtils.checkFileSize(httpFile, getContentAsString(), "<td>Dateigr&ouml;&szlig;e:</td>\n<td colspan=\"2\">", "</td>");
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

            String id = PlugUtils.getStringBetween(getContentAsString(), "onClick=\"free_dl('", "');");
            makeAjaxRequest("I", id);
            final int wait = PlugUtils.getNumberBetween(getContentAsString(), "seconds:", ",");
            downloadTask.sleep(wait + 1);

            makeAjaxRequest("II", null);
            final String rcKey = PlugUtils.getStringBetween(getContentAsString(), "Recaptcha.create(\"", "\"");
            id = PlugUtils.getStringBetween(getContentAsString(), "\"&ID=", "\"");
            do {
                if (!makeRedirectedRequest(stepCaptcha(rcKey, id))) {
                    checkProblems();
                    throw new ServiceConnectionProblemException();
                }
            } while (getContentAsString().contains("Deine Eingabe war leider falsch"));

            makeAjaxRequest("III", null);

            makeAjaxRequest("IV", null);

            method = getMethodBuilder()
                    .setReferer(fileURL)
                    .setAction(getContentAsString().trim())
                    .toGetMethod();
            if (!tryDownloadAndSaveFile(method)) {
                checkProblems();
                throw new ServiceConnectionProblemException("Error starting download");
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        if (getContentAsString().contains("Datei wurde nicht gefunden")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    private void makeAjaxRequest(final String p, String id) throws Exception {
        if (id == null) {
            id = findId(p);
        }
        final HttpMethod method = getMethodBuilder()
                .setReferer(fileURL)
                .setAjax()
                .setAction("/ajax/download.php")
                .setParameter("P", p)
                .setParameter("ID", id)
                .toPostMethod();
        if (!makeRedirectedRequest(method)) {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private String findId(final String p) throws ErrorDuringDownloadingException {
        return PlugUtils.getStringBetween(getContentAsString(), "data: \"P=" + p + "&ID=", "\"");
    }

    private HttpMethod stepCaptcha(final String rcKey, final String id) throws Exception {
        final ReCaptcha rc = new ReCaptcha(rcKey, client);
        final String captcha = getCaptchaSupport().getCaptcha(rc.getImageURL());
        if (captcha == null) {
            throw new CaptchaEntryInputMismatchException();
        }
        rc.setRecognized(captcha);
        return rc.modifyResponseMethod(getMethodBuilder()
                .setReferer(fileURL)
                .setAjax()
                .setAction("/ajax/recaptcha.php")
                .setParameter("Doing", "CheckCaptcha")
                .setParameter("ID", id)
        ).toPostMethod();
    }

}