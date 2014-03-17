package cz.vity.freerapid.plugins.services.yunfile;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.net.URLEncoder;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Class which contains main code
 *
 * @author Stan
 */
class YunFileFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(YunFileFileRunner.class.getName());
    private Random random = new Random();

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        checkFileURL();
        addCookie(new Cookie(".yunfile.com", "language", "en_us", "/", 86400, false));
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
        PlugUtils.checkName(httpFile, content, "&nbsp;&nbsp;", "</h2>");
        PlugUtils.checkFileSize(httpFile, content, "<b>", "</b>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        checkFileURL();
        addCookie(new Cookie(".yunfile.com", "language", "en_us", "/", 86400, false));
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            final String contentAsString = getContentAsString();
            checkProblems();
            checkNameAndSize(contentAsString);
            //final Matcher matcher = getMatcherAgainstContent("Please wait <span.+?>(.+?)</span>");
            //final int waitTime = !matcher.find() ? 30 : Integer.parseInt(matcher.group(1));
            //downloadTask.sleep(waitTime + 1); //skip wait time
            String baseURL = "http://" + method.getURI().getAuthority();
            HttpMethod httpMethod;
            int captchaCounter = 1;
            do {
                String downloadPageLink = PlugUtils.getStringBetween(getContentAsString(), "downpage_link\" href=\"", "\"");
                if (getContentAsString().contains("vcode")) {
                    final String captcha;
                    //avoid to input captcha twice, throw random number at first attempt
                    if (1 == captchaCounter) {
                        captcha = String.valueOf(random.nextInt(8000) + 1000);
                    } else {
                        captcha = getCaptchaSupport().getCaptcha(baseURL + "/verifyimg/getPcv.html");
                        if (captcha == null) {
                            throw new CaptchaEntryInputMismatchException();
                        }
                    }
                    downloadPageLink = downloadPageLink.replaceAll("\\.html$", "/" + captcha + ".html");
                }
                httpMethod = getMethodBuilder()
                        .setReferer(fileURL)
                        .setBaseURL(baseURL)
                        .setAction(downloadPageLink)
                        .toGetMethod();
                if (!makeRedirectedRequest(httpMethod)) {
                    checkProblems();
                    throw new ServiceConnectionProblemException();
                }
                checkProblems();
                baseURL = "http://" + httpMethod.getURI().getAuthority();
                ++captchaCounter;
            }
            while (getContentAsString().contains("vcode"));
            final String downloadPageUrl = httpMethod.getURI().toString();
            boolean cookieVidSet = false;
            if (getContentAsString().contains("setCookie(\"vid1\", \"")) {
                final String vid1CookieValue = PlugUtils.getStringBetween(getContentAsString(), "setCookie(\"vid1\", \"", "\"");
                addCookie(new Cookie(".yunfile.com", "vid1", vid1CookieValue, "/", 86400, false));
                cookieVidSet = true;
            }
            if (getContentAsString().contains("setCookie(\"vid2\", \"")) {
                final String vid2CookieValue = PlugUtils.getStringBetween(getContentAsString(), "setCookie(\"vid2\", \"", "\"");
                addCookie(new Cookie(".yunfile.com", "vid2", vid2CookieValue, "/", 86400, false));
                cookieVidSet = true;
            }
            if (cookieVidSet) {
                httpMethod = getMethodBuilder()
                        .setReferer(fileURL)
                        .setActionFromFormByName("down_from", true)
                        .toPostMethod();
            } else {
                final String fileId = PlugUtils.getStringBetween(getContentAsString(), "fileId.value = \"", "\"");
                final String vid = PlugUtils.getStringBetween(getContentAsString(), "vid.value = \"", "\"");
                httpMethod = getMethodBuilder()
                        .setReferer(downloadPageUrl)
                        .setActionFromFormWhereTagContains("d_down_from", true)
                        .setParameter("fileId", fileId)
                        .setParameter("vid", vid)
                        .toPostMethod();
                addCookie(new Cookie(".yunfile.com", "referer", URLEncoder.encode(downloadPageUrl, "UTF-8"), "/", 86400, false));
            }

            if (!tryDownloadAndSaveFile(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException("Error starting download");
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("\u6587\u4EF6\u4E0D\u5B58\u5728") || contentAsString.contains("Been deleted")) { // 文件不存在 {@see http://www.snible.org/java2/uni2java.html}
            throw new URLNotAvailableAnymoreException("File not found");
        }
        if (contentAsString.contains("down_interval")) {
            throw new YouHaveToWaitException("Waiting for next file.",
                    PlugUtils.getWaitTimeBetween(contentAsString,
                            "down_interval_tag\" style=\" color: green; font-size: 28px; \">", "</span>",
                            TimeUnit.MINUTES));
        }
        if (contentAsString.contains("Web Server may be down")) {
            throw new ServiceConnectionProblemException("A communication error occurred: \"Operation timed out\"");
        }
    }

    private void checkFileURL() {
        fileURL = fileURL.replaceFirst("yfdisk\\.com", "yunfile.com").replaceFirst("filemarkets\\.com", "yunfile.com").replaceFirst("www\\.yunfile\\.com", "yunfile.com"); //apparently they redirect www.yunfile.com to yunfile.com
    }

    @Override
    protected String getBaseURL() {
        return "http://yunfile.com";
    }
}