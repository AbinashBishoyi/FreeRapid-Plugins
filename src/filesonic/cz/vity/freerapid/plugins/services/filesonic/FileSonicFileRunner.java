package cz.vity.freerapid.plugins.services.filesonic;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.recaptcha.ReCaptcha;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import cz.vity.freerapid.utilities.LogUtils;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethod;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author JPEXS, ntoskrnl
 */
class FileSonicFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(FileSonicFileRunner.class.getName());

    private void ensureENLanguage() throws Exception {
        final String domain = new URI(getMethodBuilder().getBaseURL()).getHost();
        addCookie(new Cookie(domain, "lang", "en", "/", 86400, false));
    }

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        ensureENLanguage();
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
        if (!isFolder()) {
            final String content = getContentAsString();
            PlugUtils.checkName(httpFile, content, "<title>Download", "for free on Filesonic.com</title>");
            final String size = PlugUtils.getStringBetween(content, "<span class=\"fileSize\">", "</span>");
            httpFile.setFileSize(PlugUtils.getFileSizeFromString(size.replace(",", "")));
        }
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private boolean isFolder() {
        return fileURL.contains("/folder/");
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        ensureENLanguage();
        HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize();
            if (isFolder()) {
                handleFolder();
                return;
            }
            fileURL = method.getURI().toString();
            final String startUrl = fileURL + "?start=1";
            method = getMethodBuilder().setReferer(fileURL).setAction(startUrl).toPostMethod();
            while (true) {
                method.addRequestHeader("X-Requested-With", "XMLHttpRequest");
                if (!makeRedirectedRequest(method)) {
                    checkProblems();
                    throw new ServiceConnectionProblemException();
                }
                final String content = getContentAsString();
                if (content.contains("Recaptcha")) {
                    final String reCaptchaKey = PlugUtils.getStringBetween(content, "Recaptcha.create(\"", "\"");
                    final ReCaptcha r = new ReCaptcha(reCaptchaKey, client);
                    final String captcha = getCaptchaSupport().getCaptcha(r.getImageURL());
                    if (captcha == null) {
                        throw new CaptchaEntryInputMismatchException();
                    }
                    r.setRecognized(captcha);
                    method = r.modifyResponseMethod(getMethodBuilder().setReferer(fileURL).setAction(startUrl)).toPostMethod();
                } else if (content.contains("var countDownDelay =")) {
                    final int waitTime = PlugUtils.getWaitTimeBetween(content, "var countDownDelay =", ";", TimeUnit.SECONDS);
                    downloadTask.sleep(waitTime + 1);
                    try {
                        final String tm = PlugUtils.getParameter("tm", content);
                        final String tm_hash = PlugUtils.getParameter("tm_hash", content);
                        method = getMethodBuilder().setReferer(fileURL).setAction(startUrl).setParameter("tm", tm).setParameter("tm_hash", tm_hash).toPostMethod();
                    } catch (PluginImplementationException e) {
                        method = getMethodBuilder().setReferer(fileURL).setAction(startUrl).toPostMethod();
                    }
                } else if (content.contains("Start Download Now")) {
                    method = getMethodBuilder().setReferer(fileURL).setActionFromAHrefWhereATagContains("Start Download Now").toGetMethod();
                    setFileStreamContentTypes("\"application/octet-stream\"");
                    if (!tryDownloadAndSaveFile(method)) {
                        checkProblems();
                        throw new ServiceConnectionProblemException("Error starting download");
                    }
                    break;
                } else {
                    checkProblems();
                    throw new PluginImplementationException();
                }
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        if (content.contains("for parallel downloads")) {
            throw new YouHaveToWaitException("Already processing a download", 30);
        }
        if (content.contains("File not found") || content.contains("This file was deleted") || content.contains("The requested folder do not exist")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        if (content.contains("The file that you're trying to download is larger than")) {
            throw new NotRecoverableDownloadException("Only premium users can download large files");
        }
    }

    private void handleFolder() throws ErrorDuringDownloadingException {
        final List<URI> list = new LinkedList<URI>();
        final Matcher matcher = getMatcherAgainstContent("<a href=\"(http://(?:www\\.)?filesonic\\.[a-z]{2,3}(?:\\.[a-z]{2,3})?/file/.+?)\">");
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
        getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, list);
        httpFile.getProperties().put("removeCompleted", true);
    }

}
