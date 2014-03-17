package cz.vity.freerapid.plugins.services.megaupload;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.megaupload.captcha.CaptchaReader;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import cz.vity.freerapid.utilities.LogUtils;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethod;

import java.awt.image.BufferedImage;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author Ladislav Vitasek, Ludek Zika, JPEXS, ntoskrnl
 */
class MegauploadRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(MegauploadRunner.class.getName());
    private final static int CAPTCHA_MAX = 5;
    private int captchaCount = 0;

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        checkURL();
        addCookie(new Cookie(".megaupload.com", "l", "en", "/", 86400, false));
        addCookie(new Cookie(".megaporn.com", "l", "en", "/", 86400, false));
        final HttpMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            if (!isFolder()) {
                checkNameAndSize();
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    @Override
    public void run() throws Exception {
        super.run();
        checkURL();
        logger.info("Starting download in TASK " + fileURL);
        addCookie(new Cookie(".megaupload.com", "l", "en", "/", 86400, false));
        addCookie(new Cookie(".megaporn.com", "l", "en", "/", 86400, false));

        final boolean loggedIn = login();

        HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();

            if (isFolder()) {
                stepFolder();
                return;
            }

            checkNameAndSize();

            if (loggedIn) {
                if (tryManagerDownload(fileURL)) {
                    return;
                }
            }

            if (getContentAsString().contains("download is password protected")) {
                stepPasswordPage();
            }
            while (getContentAsString().contains("Enter this")) {
                stepCaptcha();
                checkProblems();
            }

            final Matcher matcher = getMatcherAgainstContent("id=\"downloadlink\"><a href=\"(http.+?)\"");
            if (!matcher.find()) {
                if (loggedIn && makeRedirectedRequest(getGetMethod("/?c=account"))) {
                    if (getContentAsString().contains("<b>Premium</b>")) {
                        throw new NotRecoverableDownloadException("Premium account detected, please use premium plugin instead");
                    }
                }
                throw new PluginImplementationException("Download link not found");
            }
            final String url = matcher.group(1);

            final int index = url.lastIndexOf('/');
            if (index > 0) {
                final String name = url.substring(index + 1);
                httpFile.setFileName(PlugUtils.unescapeHtml(URLDecoder.decode(name, "UTF-8")));
            }

            method = getMethodBuilder().setReferer(fileURL).setAction(url).toGetMethod();
            downloadTask.sleep(PlugUtils.getNumberBetween(getContentAsString(), "count=", ";") + 1);
            if (!tryDownloadAndSaveFile(method)) {
                checkProblems();
                throw new ServiceConnectionProblemException("Error starting download");
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize() throws Exception {
        if (getContentAsString().contains("link you have clicked is not available")) {
            throw new URLNotAvailableAnymoreException("The file is not available");
        }
        PlugUtils.checkName(httpFile, getContentAsString(), "<span class=\"down_txt2\">", "</span>");
        PlugUtils.checkFileSize(httpFile, getContentAsString(), "ize:</strong>", "<");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }


    private void checkProblems() throws Exception {
        final String content = getContentAsString();
        if (content.contains("trying to access is temporarily unavailable")) {
            throw new ServiceConnectionProblemException("The file you are trying to access is temporarily unavailable");
        }
        if (content.contains("Download limit exceeded")) {
            final HttpMethod getMethod = getGetMethod("/premium/???????????????");
            if (makeRequest(getMethod)) {
                Matcher matcher = getMatcherAgainstContent("Please wait ([0-9]+)");
                if (matcher.find()) {
                    throw new YouHaveToWaitException("You used up your limit for file downloading!", 1 + 60 * Integer.parseInt(matcher.group(1)));
                }
            }
            throw new ServiceConnectionProblemException("Download limit exceeded.");
        }
        if (content.contains("All download slots")) {
            throw new ServiceConnectionProblemException("No free slot for your country.");
        }
        if (content.contains("to download is larger than")) {
            throw new NotRecoverableDownloadException("Only premium users are entitled to download files larger than 1 GB from Megaupload.");
        }
        if (content.contains("the link you have clicked is not available")) {
            throw new URLNotAvailableAnymoreException("The file is not available");
        }
        if (content.contains("We have detected an elevated number of requests")) {
            final int wait = PlugUtils.getWaitTimeBetween(content, "check back in", "minute", TimeUnit.MINUTES);
            throw new YouHaveToWaitException("We have detected an elevated number of requests", wait);
        }
    }

    private void checkURL() {
        final String host = httpFile.getFileUrl().getHost();
        if (host.contains("megarotic") || host.contains("sexuploader") || host.contains("megaporn")) {
            fileURL = fileURL.replace("megarotic.com", "megaporn.com").replace("sexuploader.com", "megaporn.com");
        }
    }

    private void stepCaptcha() throws Exception {
        final String captchaUrl = getMethodBuilder().setActionFromImgSrcWhereTagContains("gencap.php").getEscapedURI();
        logger.info("Captcha URL: " + captchaUrl);
        final BufferedImage captchaImage = getCaptchaSupport().getCaptchaImage(captchaUrl);

        String captcha;
        if (captchaCount++ < CAPTCHA_MAX) {
            captcha = CaptchaReader.read(captchaImage);
            if (captcha == null) {
                logger.warning("Cant read captcha");
                captcha = "aaaa";
            } else {
                logger.info("Read captcha: " + captcha);
            }
        } else {
            captcha = getCaptchaSupport().askForCaptcha(captchaImage);
        }
        if (captcha == null)
            throw new CaptchaEntryInputMismatchException();

        final HttpMethod postMethod = getMethodBuilder()
                .setReferer(fileURL)
                .setActionFromFormByName("captchaform", true)
                .setParameter("captcha", captcha)
                .setAction(fileURL)
                .toPostMethod();
        if (!makeRedirectedRequest(postMethod)) {
            throw new ServiceConnectionProblemException();
        }
    }

    private void stepPasswordPage() throws Exception {
        while (getContentAsString().contains("Please enter the password")) {
            final String password = getDialogSupport().askForPassword("MegaUpload");
            if (password == null) {
                throw new NotRecoverableDownloadException("This file is secured with a password");
            }
            final HttpMethod method = getMethodBuilder()
                    .setReferer(fileURL)
                    .setAction(fileURL)
                    .setParameter("filepassword", password)
                    .toPostMethod();
            if (!makeRedirectedRequest(method)) {
                throw new ServiceConnectionProblemException();
            }
        }
    }

    private boolean isFolder() {
        return getContentAsString().contains("folderid = \"");
    }

    private void stepFolder() throws Exception {
        final String folderid = PlugUtils.getStringBetween(getContentAsString(), "folderid = \"", "\";");
        final String xmlURL = "/xml/folderfiles.php?folderid=" + folderid + "&uniq=1";
        final HttpMethod folderHttpMethod = getMethodBuilder().setReferer(fileURL).setAction(xmlURL).toGetMethod();
        if (makeRedirectedRequest(folderHttpMethod)) {
            if (getContentAsString().contains("<FILES></FILES>"))
                throw new URLNotAvailableAnymoreException("No files in folder. Invalid link?");

            final Matcher matcher = getMatcherAgainstContent("url=\"(.+?)\"");
            final List<URI> uriList = new LinkedList<URI>();
            while (matcher.find()) {
                try {
                    uriList.add(new URI(matcher.group(1)));
                } catch (URISyntaxException e) {
                    LogUtils.processException(logger, e);
                }
            }
            getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, uriList);
            httpFile.getProperties().put("removeCompleted", true);
        } else {
            throw new ServiceConnectionProblemException();
        }
    }

    private boolean login() throws Exception {
        synchronized (MegauploadRunner.class) {
            final MegauploadShareServiceImpl service = (MegauploadShareServiceImpl) getPluginService();
            final PremiumAccount pa = service.getConfig();
            if (pa == null || !pa.isSet()) {
                logger.info("No account data set, skipping login");
                return false;
            }

            final HttpMethod httpMethod = getMethodBuilder()
                    .setAction("/?c=login")
                    .setParameter("login", "1")
                    .setParameter("username", pa.getUsername())
                    .setParameter("password", pa.getPassword())
                    .toPostMethod();
            if (!makeRedirectedRequest(httpMethod))
                throw new ServiceConnectionProblemException("Error posting login info");

            if (getContentAsString().contains("Username and password do not match"))
                throw new NotRecoverableDownloadException("Invalid MegaUpload account login information!");

            return true;
        }
    }

    private String getManagerURL(final String url) {
        final Cookie user = getCookieByName("user");
        if (user != null) {
            final Matcher matcher = PlugUtils.matcher("[\\?&][df]=([^&=]+)", url);
            if (matcher.find()) {
                return "/mgr_dl.php?d=" + matcher.group(1) + "&u=" + user.getValue();
            }
        }
        return url;
    }

    private void setManagerRequestHeaders(final HttpMethod method) {
        method.setRequestHeader("Accept", "text/plain,text/html,*/*;q=0.3");
        method.setRequestHeader("Accept-Encoding", "identity");
        method.setRequestHeader("TE", "trailers");
        method.setRequestHeader("Connection", "TE");
        method.setRequestHeader("User-Agent", "Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 5.1; Trident/4.0)");
    }

    private boolean tryManagerDownload(final String url) throws Exception {
        HttpMethod method = getMethodBuilder().setAction(getManagerURL(url)).setReferer(null).toGetMethod();
        setManagerRequestHeaders(method);
        if (client.makeRequest(method, false) == 302) {
            final String downloadURL = method.getResponseHeader("Location").getValue();
            logger.info("Found redirect location " + downloadURL);
            if (downloadURL.contains("files")) {
                final int i = downloadURL.lastIndexOf('/');
                if (i > 0) {
                    final String toEncode = downloadURL.substring(i + 1);
                    httpFile.setFileName(PlugUtils.unescapeHtml(toEncode));
                }
                method = getMethodBuilder().setAction(downloadURL).setReferer(null).toGetMethod();
                try {
                    return tryDownloadAndSaveFile(method);
                } catch (Exception e) {
                    return false;
                }
            } else {
                if (!makeRedirectedRequest(getGetMethod(fileURL))) {
                    throw new ServiceConnectionProblemException();
                }
                return false;
            }
        } else {
            if (!makeRedirectedRequest(getGetMethod(fileURL))) {
                throw new ServiceConnectionProblemException();
            }
            return false;
        }
    }

}
