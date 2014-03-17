package cz.vity.freerapid.plugins.services.gigaup;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadClient;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import cz.vity.freerapid.plugins.webclient.utils.HttpUtils;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 */
class GigaUPFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(GigaUPFileRunner.class.getName());
    private final static int CAPTCHA_MAX = 5;
    private int captchaCounter = 1;

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, getContentAsString(), "<div class=\"text_t\">", "</div>");

        final Matcher size = getMatcherAgainstContent("Taille de (.+?)o\\s*?<");
        if (!size.find()) throw new PluginImplementationException("File size not found");
        httpFile.setFileSize(PlugUtils.getFileSizeFromString(size.group(1) + "B")); //they use "o" instead of "B" for byte in french

        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize();

            if (getContentAsString().contains("bot_sucker")) {
                while (getContentAsString().contains("bot_sucker")) {
                    if (!makeRedirectedRequest(stepCaptcha())) {
                        throw new ServiceConnectionProblemException("Error posting captcha");
                    }
                    if (getContentAsString().contains("Le code de vérification")) {
                        if (!makeRedirectedRequest(method)) {
                            throw new ServiceConnectionProblemException();
                        }
                    }
                }
            } else {
                throw new PluginImplementationException("Captcha not found");
            }

            final Matcher matcher = getMatcherAgainstContent("href=\"(ftp://.+?)\"");
            if (!matcher.find()) throw new PluginImplementationException("Download link not found");

            if (!tryDownloadAndSaveFileFTP(matcher.group(1))) {
                checkProblems();
                throw new ServiceConnectionProblemException("Error starting download");
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        if (content.contains("file_upload.php") || content.contains("<h1>Not Found</h1>")) {
            throw new URLNotAvailableAnymoreException("Page not found");
        }
        if (content.contains("Le fichier que vous tentez de télécharger n'existe pas")) {
            throw new URLNotAvailableAnymoreException("File does not exist");
        }
        if (content.contains("Le fichier a été désigné illégal par les administrateurs et donc supprimé")) {
            throw new URLNotAvailableAnymoreException("File was deleted – illegal content");
        }
        if (content.contains("Fichier supprimé car non utilisé sur une période trop longue")) {
            throw new URLNotAvailableAnymoreException("File was deleted – too long since last download");
        }
    }

    private HttpMethod stepCaptcha() throws ErrorDuringDownloadingException {
        final CaptchaSupport captchaSupport = getCaptchaSupport();
        final String captchaSrc = getMethodBuilder().setActionFromImgSrcWhereTagContains("bot_sucker").getEscapedURI().replace("/..", "");
        logger.info("Captcha URL " + captchaSrc);

        final String captcha;
        if (captchaCounter <= CAPTCHA_MAX) {
            captcha = PlugUtils.recognize(captchaSupport.getCaptchaImage(captchaSrc), "-d -1 -C 0-9");
            logger.info("OCR attempt " + captchaCounter + " of " + CAPTCHA_MAX + ", recognized " + captcha);
            captchaCounter++;
        } else {
            captcha = captchaSupport.getCaptcha(captchaSrc);
            if (captcha == null) throw new CaptchaEntryInputMismatchException();
            logger.info("Manual captcha " + captcha);
        }

        return getMethodBuilder().setReferer(fileURL).setActionFromFormWhereTagContains("bot_sucker", true).setParameter("bot_sucker", captcha).toPostMethod();
    }

    @Override
    protected String getBaseURL() {
        return "http://www.gigaup.fr/";
    }

    private boolean tryDownloadAndSaveFileFTP(final String uri) throws Exception {
        logger.info("Starting download from " + uri);
        httpFile.setState(DownloadState.GETTING);

        prepareForDownload();

        final URL url = new URL(uri);
        final URLConnection connection = url.openConnection();
        InputStream is = null;
        try {
            is = connection.getInputStream();

            final long contentLength = connection.getContentLength();
            if (contentLength < 0) {
                logger.warning("Content-Length not found");
                return false;
            }
            httpFile.setFileSize(contentLength);
            httpFile.getProperties().put(DownloadClient.SUPPOSE_TO_DOWNLOAD, contentLength);

            if (is != null) {
                logger.info("Saving to file");
                downloadTask.saveToFile(is);
                return true;
            } else {
                logger.info("Saving file failed");
                return false;
            }
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }

    private void prepareForDownload() throws IOException {
        httpFile.getProperties().remove(DownloadClient.START_POSITION);
        httpFile.getProperties().remove(DownloadClient.SUPPOSE_TO_DOWNLOAD);

        final String fn = httpFile.getFileName();
        if (fn == null || fn.isEmpty())
            throw new IOException("No defined file name");
        httpFile.setFileName(HttpUtils.replaceInvalidCharsForFileSystem(PlugUtils.unescapeHtml(fn), "_"));
    }

}