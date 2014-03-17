package cz.vity.freerapid.plugins.services.filejungle;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.recaptcha.ReCaptcha;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadClientConsts;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.interfaces.FileStreamRecognizer;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * @author CrazyCoder
 */
class FileJungleFileRunner extends AbstractRunner implements FileStreamRecognizer {
    private final static Logger logger = Logger.getLogger(FileJungleFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize(getContentAsString());
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, content, "<div id=\"file_name\">", "<span class=\"filename_normal\">");
        PlugUtils.checkFileSize(httpFile, content, "<span class=\"filename_normal\">(", ")</span></div>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        setClientParameter(DownloadClientConsts.FILE_STREAM_RECOGNIZER, this);
        logger.info("Starting download in TASK " + fileURL);
        HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            String content = getContentAsString();
            checkProblems();
            checkNameAndSize(content);

            final String ref = fileURL;

            final String code = PlugUtils.getStringBetween(content, "\"regularForm\" action=\"/f/", "\"");
            final String capUrl = getBaseURL() + "/checkReCaptcha.php";
            final String startUrl = getBaseURL() + "/f/" + code;
            final String captchaKey = PlugUtils.getStringBetween(content, "reCAPTCHA_publickey='", "';");

            logger.info("code: " + code);
            logger.info("start: " + startUrl);
            logger.info("referer: " + ref);
            logger.info("captcha key: " + captchaKey);

            while (true) {
                method = getMethodBuilder().setReferer(ref).setAction(fileURL).setParameter("checkDownload", "check").toPostMethod();
                setAjaxHeaders(method);
                if (!makeRedirectedRequest(method)) {
                    checkProblems();
                    throw new ServiceConnectionProblemException();
                }
                content = getContentAsString();

                if (content.contains("showCaptcha")) {
                    // show captcha
                    final ReCaptcha r = new ReCaptcha(captchaKey, client);
                    final String captcha = getCaptchaSupport().getCaptcha(r.getImageURL());
                    if (captcha == null) {
                        throw new CaptchaEntryInputMismatchException();
                    }
                    r.setRecognized(captcha);

                    method = r.modifyResponseMethod(getMethodBuilder()
                            .setReferer(ref)
                            .setAction(capUrl)
                            .setParameter("recaptcha_shortencode_field", code))
                            .toPostMethod();

                    setAjaxHeaders(method);

                    if (!makeRedirectedRequest(method)) {
                        checkProblems();
                        throw new ServiceConnectionProblemException();
                    }

                    content = getContentAsString();

                    if (content.contains("\"success\":1")) {
                        method = getMethodBuilder().setReferer(ref).setAction(startUrl).setParameter("downloadLink", "wait").toPostMethod();
                        setAjaxHeaders(method);

                        if (!makeRedirectedRequest(method)) {
                            checkProblems();
                            throw new ServiceConnectionProblemException();
                        }

                        content = getContentAsString();

                        if (content.contains("waitTime")) {
                            final int wait = PlugUtils.getWaitTimeBetween(content, "waitTime\":", ",", TimeUnit.SECONDS);
                            logger.info("wait: " + wait);
                            downloadTask.sleep(wait + 1);


                            method = getMethodBuilder().setParameter("downloadLink", "show").setReferer(ref).setAction(startUrl).toPostMethod();
                            setAjaxHeaders(method);
                            if (!makeRedirectedRequest(method)) {
                                checkProblems();
                                throw new ServiceConnectionProblemException();
                            }

                            content = getContentAsString();

                            if (content.contains("forcePremiumDownload")) {
                                throw new NotSupportedDownloadByServiceException("Premium account is required for this file download");
                            }

                            method = getMethodBuilder().setParameter("download", "normal").setReferer(ref).setAction(startUrl).toPostMethod();
                            setAjaxHeaders(method);

                            if (client.makeRequest(method, false) == HttpStatus.SC_MOVED_TEMPORARILY) {
                                final Header location = method.getResponseHeader("Location");
                                if (location != null) {
                                    final String downloadUrl = location.getValue();
                                    logger.info("Download URL: " + downloadUrl);
                                    method = getMethodBuilder().setReferer(ref).setAction(downloadUrl).toGetMethod();
                                    setFileStreamContentTypes("\"application/octet-stream\"");
                                    if (!tryDownloadAndSaveFile(method)) {
                                        checkProblems();
                                        throw new ServiceConnectionProblemException("Error starting download");
                                    }
                                    break;
                                }
                            }
                        }
                    }
                } else if (content.contains("captchaFail")) {
                    throw new YouHaveToWaitException("Your IP has failed the captcha too many times", 60);
                } else if (content.contains("timeLimit")) {
                    throw new YouHaveToWaitException("Please wait...", 60);
                } else {
                    throw new PluginImplementationException("Unexpected response");
                }
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void setAjaxHeaders(HttpMethod method) {
        method.setRequestHeader("Accept", "application/json, text/javascript, */*");
        method.setRequestHeader("Content-Type", "application/x-www-form-urlencoded");
        method.setRequestHeader("X-Requested-With", "XMLHttpRequest");
        method.removeRequestHeader("Keep-Alive");
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("File not available") ||
                contentAsString.contains("This file is no longer available")) {
            throw new URLNotAvailableAnymoreException("File not available");
        }
    }

    @Override
    public boolean isStream(HttpMethod method, boolean showWarnings) {
        final Header h = method.getResponseHeader("Content-Type");
        if (h == null) return false;
        final String contentType = h.getValue().toLowerCase(Locale.ENGLISH);
        return (!contentType.startsWith("text/") && !contentType.contains("xml") && !contentType.startsWith("application/json"));
    }
}
