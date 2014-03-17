package cz.vity.freerapid.plugins.services.mega1280;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.recaptcha.ReCaptcha;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author Vity, ntoskrnl
 */
class Mega1280FileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(Mega1280FileRunner.class.getName());
    private String reCaptchaKey;
    private String file_id;

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        if (!checkIsFolder()) {

            final HttpMethod getMethod = getGetMethod(fileURL);
            if (makeRedirectedRequest(getMethod)) {
                checkProblems();
                checkNameAndSize();
            } else {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
        }
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, getContentAsString(), "file: <span class=\"color_red\">", "</span>");
        PlugUtils.checkFileSize(httpFile, getContentAsString(), "Dung l\u01B0\u1EE3ng: </b><span>", "</span>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        if (checkIsFolder()) {
            runFolder();
            return;
        } else {

            final HttpMethod method = getGetMethod(fileURL);
            if (makeRedirectedRequest(method)) {
                checkProblems();
                checkNameAndSize();

                do {
                    if (!makeRedirectedRequest(stepCaptcha())) {
                        throw new ServiceConnectionProblemException();
                    }
                } while (getContentAsString().contains("recaptcha"));

                final String url = PlugUtils.getStringBetween(getContentAsString(), "onclick=\"window.location='", "'\"/>");
                final Integer time = PlugUtils.getNumberBetween(getContentAsString(), "var count = ", ";");
                downloadTask.sleep(time + 1);
                HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL).setAction(url).toGetMethod();


                if (!tryDownloadAndSaveFile(httpMethod)) {
                    checkProblems();
                    throw new ServiceConnectionProblemException("Error starting download");
                }
            } else {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("File Not Found")
                || contentAsString.contains("File not found")
                || contentAsString.contains("Li\u00EAn k\u1EBFt b\u1EA1n v\u1EEBa ch\u1ECDn kh\u00F4ng t\u1ED3n t\u1EA1i tr\u00EAn h\u1EC7 th\u1ED1ng")
                || contentAsString.contains("Y\u00EAu c\u1EA7u kh\u00F4ng \u0111\u01B0\u1EE3c t\u00ECm th\u1EA5y")
                || contentAsString.contains("Li\u00EAn k\u1EBFt b\u1EA1n ch\u1ECDn kh\u00F4ng t\u1ED3n")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        if (contentAsString.contains("Vui l\u00F2ng ch\u1EDD l\u01B0\u1EE3t download k\u1EBF ti\u1EBFp"))
            throw new ServiceConnectionProblemException("Please wait for your previous download to finish");
        if (contentAsString.contains("Limit download xx !")) {
            throw new ServiceConnectionProblemException("Limit download xx ! - unknown error message from server");
        }
    }

    private HttpMethod stepCaptcha() throws Exception {
        if (reCaptchaKey == null)
            reCaptchaKey = PlugUtils.getStringBetween(getContentAsString(), "/api/noscript?k=", "\"");
        if (file_id == null)
            file_id = PlugUtils.getStringBetween(getContentAsString(), "file_id\" value=\"", "\"");
        final ReCaptcha r = new ReCaptcha(reCaptchaKey, client);
        final CaptchaSupport captchaSupport = getCaptchaSupport();

        final String captchaURL = r.getImageURL();
        logger.info("Captcha URL " + captchaURL);

        final String captcha = captchaSupport.getCaptcha(captchaURL);
        if (captcha == null) throw new CaptchaEntryInputMismatchException();
        r.setRecognized(captcha);

        final HttpMethod httpMethod = r.modifyResponseMethod(
                getMethodBuilder()
                        .setReferer(fileURL)
                        .setAction(fileURL)
                        .setParameter("btn_download", "Download")
                        .setParameter("action", "download_file")
                        .setParameter("file_id", file_id)
                //
        ).toPostMethod();
        //httpMethod.addRequestHeader("X-Requested-With", "XMLHttpRequest");//use AJAX
        return httpMethod;
    }

    private boolean checkIsFolder() {
        return fileURL.contains("/folder/");
    }


    public void runFolder() throws Exception {

        HashSet<URI> queye = new HashSet<URI>();
        final String REGEX = "class=\"w_80pc\"><a href=\"(http://(?:www\\.)?mega\\.1280\\.com/file/[^\"]+)\"";

        final HttpMethod getMethod = getGetMethod(fileURL);
        getMethod.setFollowRedirects(true);
        if (makeRedirectedRequest(getMethod)) {
            Matcher matcher = getMatcherAgainstContent(REGEX);
            while (matcher.find()) {
                queye.add(new URI(matcher.group(1)));
            }

        } else
            throw new PluginImplementationException("Folder Can't be Loaded !!");


        synchronized (getPluginService().getPluginContext().getQueueSupport()) {
            getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, new ArrayList<URI>(queye));
        }
        httpFile.getProperties().put("removeCompleted", true);

    }

}