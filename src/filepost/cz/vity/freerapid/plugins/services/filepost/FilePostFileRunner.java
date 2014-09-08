package cz.vity.freerapid.plugins.services.filepost;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.recaptcha.ReCaptcha;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import cz.vity.freerapid.utilities.LogUtils;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.methods.GetMethod;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author CrazyCoder
 * @author ntoskrnl
 */
class FilePostFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(FilePostFileRunner.class.getName());
    private final static Semaphore SEMAPHORE = new Semaphore(2, true);
    private long ajaxCounter = 0;

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        addCookie(new Cookie(".filepost.com", "lang", "1", "/", 86400, false));
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRequestWithSleep(getMethod)) {
            checkProblems();
            checkNameAndSize(getContentAsString());
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        if (!isFolder()) {
            PlugUtils.checkName(httpFile, content, "<title>FilePost.com: Download", "- fast &amp; secure!</title>");
            PlugUtils.checkFileSize(httpFile, content, "<span>Size:</span>", "</li>");
        }
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        addCookie(new Cookie(".filepost.com", "lang", "1", "/", 86400, false));
        HttpMethod method = getGetMethod(fileURL);
        if (makeRequestWithSleep(method)) {
            final String contentAsString = getContentAsString();
            checkProblems();
            checkNameAndSize(contentAsString);

            fileURL = method.getURI().toString();

            if (isFolder()) {
                parseFolder();
                return;
            }

            final Cookie sid = getCookieByName("SID");
            if (sid == null) {
                throw new PluginImplementationException("SID cookie not found");
            }

            final Matcher matcher = PlugUtils.matcher("/files/([^/]+)", fileURL);
            if (!matcher.find()) {
                throw new PluginImplementationException("Error parsing file URL");
            }
            final String code = matcher.group(1);
            logger.info("Code: " + code);

            final String captchaKey = PlugUtils.getStringBetween(getContentAsString(), "key:\t\t\t'", "',");
            logger.info("Captcha key: " + captchaKey);

            final boolean showCaptcha = getContentAsString().contains("show_captcha = true");
            final boolean passworded = getContentAsString().contains("is_pass_exists = true");
            logger.info("showCaptcha = " + showCaptcha);
            logger.info("passworded = " + passworded);
            String password = null;
            HttpMethod ajax = null;

            while (true) {
                if (ajax == null) ajax = ajaxBuilder(sid, code, password, true).toPostMethod();

                if (!makeRequestWithSleep(ajax)) {
                    checkProblems();
                    throw new ServiceConnectionProblemException();
                }
                ajax = null;

                String content = getContentAsString();
                logger.info(content);

                if (content.contains("{\"answer\":{\"link\":\"")) {
                    final String fileUrl = unescape(PlugUtils.getStringBetween(content, "{\"link\":\"", "\"}}"));
                    logger.info("FILE: " + fileUrl);
                    method = getMethodBuilder().setReferer(fileURL).setAction(fileUrl).toGetMethod();
                    setFileStreamContentTypes("\"application/octet-stream\"");
                    if (!tryDownloadAndSaveFile(method)) {
                        checkProblems();
                        throw new ServiceConnectionProblemException("Error starting download");
                    }
                    break;
                } else if (content.contains("wait_time")) {
                    int wait = 60;
                    try {
                        // please don't use PlugUtils.getNumberBetween, as it will throw exception on negative time value
                        // which is perfectly valid, see the logic below
                        wait = Integer.parseInt(PlugUtils.getStringBetween(content, "\"wait_time\":\"", "\"}}"));
                    } catch (NumberFormatException ignored) {
                    }

                    logger.info("wait: " + wait);

                    if (wait > 0) {
                        downloadTask.sleep(wait + 1);
                    }
                    if (passworded && (password == null)) {
                        password = getDialogSupport().askForPassword("FilePost");
                        if (password == null) {
                            throw new NotRecoverableDownloadException("This file is secured with a password");
                        }
                        ajax = ajaxBuilder(sid, code, password, false).toPostMethod();
                    }
                    if (showCaptcha) {
                        final ReCaptcha r = new ReCaptcha(captchaKey, client);
                        final String captcha = getCaptchaSupport().getCaptcha(r.getImageURL());
                        if (captcha == null) {
                            throw new CaptchaEntryInputMismatchException();
                        }
                        r.setRecognized(captcha);
                        ajax = r.modifyResponseMethod(ajaxBuilder(sid, code, password, false)).toPostMethod();
                    }
                    if (!passworded && !showCaptcha) {
                        ajax = ajaxBuilder(sid, code, null, false).toPostMethod();
                    }
                } else {
                    checkProblems();
                    throw new PluginImplementationException("Waiting time and download link not found");
                }
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private String unescape(String string) {
        return string.replaceAll("\\\\", "");
    }

    private MethodBuilder ajaxBuilder(Cookie sid, String code, String password, boolean start) throws URIException, PluginImplementationException {
        final long time = new Date().getTime();
        final String startUrl = "http://filepost.com/files/get/?SID=" + sid.getValue() + "&JsHttpRequest=" + time + ajaxCounter++ + "-xml";
        logger.info("Start URL: " + startUrl);

        MethodBuilder mb = getMethodBuilder()
                .setReferer(fileURL)
                .setAction(startUrl)
                .setParameter("code", code)
                .setParameter("file_pass", password == null ? "" : password);
        if (start) {
            mb.setParameter("action", "set_download");
        }
        return mb;
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("File not found")) {
            throw new URLNotAvailableAnymoreException("File not found");
        } else if (contentAsString.contains("already downloading a file")) {
            throw new YouHaveToWaitException("Your IP address is already downloading a file at the moment", 60);
        } else if (contentAsString.contains("{\"error\":\"")) {
            final String error = unescape(PlugUtils.getStringBetween(contentAsString, "{\"error\":\"", "\"},"));
            logger.warning(error);
            if (error.contains("You entered a wrong CAPTCHA code")
                    || error.contains("Wrong file password")) {
                throw new YouHaveToWaitException(error, 4);
            } else {
                throw new ServiceConnectionProblemException(error);
            }
        }
    }

    private boolean makeRequestWithSleep(final HttpMethod method) throws Exception {
        SEMAPHORE.acquire();
        try {
            return makeRedirectedRequest(method);
        } finally {
            SEMAPHORE.release();
        }
    }

    private boolean isFolder() {
        return fileURL.contains("/folder/");
    }

    private void parseFolder() throws Exception {
        final List<URI> list = new LinkedList<URI>();
        final Matcher matcher = getMatcherAgainstContent("<a class=\"dl\" href=\"(.+?)\"");
        while (matcher.find()) {
            try {
                list.add(new URI(matcher.group(1)));
            } catch (final URISyntaxException e) {
                LogUtils.processException(logger, e);
            }
        }
        if (list.isEmpty()) {
            throw new PluginImplementationException("No links found");
        }
        httpFile.getProperties().put("removeCompleted", true);
        getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, list);
    }

}
