package cz.vity.freerapid.plugins.services.depositfiles;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.net.URI;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Ladislav Vitasek, Ludek Zika, RubinX
 */
class DepositFilesRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(DepositFilesRunner.class.getName());

    private void setLanguageEN() {
        addCookie(new Cookie(".depositfiles.com", "lang_current", "en", "/", 86400, false));
        addCookie(new Cookie(".depositfiles.org", "lang_current", "en", "/", 86400, false));
        addCookie(new Cookie(".dfiles.eu", "lang_current", "en", "/", 86400, false));
        addCookie(new Cookie(".dfiles.ru", "lang_current", "en", "/", 86400, false));
        fileURL = fileURL.replaceFirst("/[^/]{2}/(files|folders)/", "/$1/"); // remove language id from URL
    }

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        setLanguageEN();
        if (!checkIsFolder()) {
            final HttpMethod method = getGetMethod(fileURL);
            if (makeRedirectedRequest(method)) {
                checkNameAndSize(getContentAsString());
            } else {
                throw new ServiceConnectionProblemException();
            }
        }
    }

    @Override
    public void run() throws Exception {
        super.run();
        setLanguageEN();

        if (checkIsFolder()) {
            runFolder();
            return;
        }

        HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkNameAndSize(getContentAsString());
            checkProblems();

            method = getMethodBuilder().setReferer(fileURL).setAction(fileURL).setParameter("free_btn", "Regular downloading").toPostMethod();
            if (!makeRedirectedRequest(method)) {
                throw new ServiceConnectionProblemException();
            }
            checkProblems();

            Matcher matcher = getMatcherAgainstContent("setTimeout\\s*\\(\\s*'load_form.*?'\\s*,\\s*(\\d+)[^\\d]");
            if (!matcher.find()) {
                throw new PluginImplementationException("Cannot find wait time");
            }
            int seconds = Integer.parseInt(matcher.group(1));
            if (seconds >= 1000) seconds = seconds / 1000;
            logger.info("wait - " + seconds);

            matcher = getMatcherAgainstContent("<a href=\"(/get_file\\.php\\?fid=([^&]+)).*?\"[^>]*>");
            if (!matcher.find()) {
                throw new PluginImplementationException();
            }
            final String getFileUrl = matcher.group(1);
            final String fid = matcher.group(2);

            downloadTask.sleep(seconds + 1);

            String reCaptchaUrl = "";
            matcher = getMatcherAgainstContent("Recaptcha\\.create\\s*\\(\\s*'([^']+)'");
            if (matcher.find())
                reCaptchaUrl = "http://www.google.com/recaptcha/api/challenge?k=" + matcher.group(1) + "&ajax=1&cachestop=0." + System.currentTimeMillis();

            method = getMethodBuilder().setReferer(fileURL).setAction(getFileUrl).toGetMethod();
            if (!makeRedirectedRequest(method)) {
                throw new ServiceConnectionProblemException();
            }

            while (PlugUtils.find("(?s)check_recaptcha\\s*\\(\\s*'" + fid + "'", getContentAsString())) {
                logger.info("Captcha URL: " + reCaptchaUrl);
                method = getGetMethod(reCaptchaUrl);
                if (!makeRedirectedRequest(method))
                    throw new ServiceConnectionProblemException();

                matcher = getMatcherAgainstContent("(?s)challenge\\s*:\\s*'([\\w-]+)'.*?server\\s*:\\s*'([^']+)'");
                if (!matcher.find())
                    throw new PluginImplementationException("Error parsing ReCaptcha response");
                String reCaptcha_challenge = matcher.group(1);
                String captchaImg = matcher.group(2) + "image?c=" + reCaptcha_challenge;
                logger.info("Captcha URL: " + captchaImg);

                String reCaptcha_response = getCaptchaSupport().getCaptcha(captchaImg);
                if (reCaptcha_response == null)
                    throw new CaptchaEntryInputMismatchException();

                method = getMethodBuilder()
                        .setAction("/get_file.php")
                        .setReferer(fileURL)
                        .setEncodePathAndQuery(true)
                        .setParameter("fid", fid)
                        .setParameter("challenge", reCaptcha_challenge)
                        .setParameter("response", reCaptcha_response)
                        .toGetMethod();

                if (!makeRedirectedRequest(method))
                    throw new ServiceConnectionProblemException();
            }

            matcher = getMatcherAgainstContent("\\('fastdownload'\\s*,\\s*'(.+?)'\\)");
            if (matcher.find()) {
                logger.info("Fast download");
                method = getMethodBuilder()
                        .setAction("/get_file.php")
                        .setReferer(fileURL)
                        .setParameter("fd", matcher.group(1))
                        .toGetMethod();
                if (!makeRedirectedRequest(method)) {
                    checkProblems();
                    throw new ServiceConnectionProblemException();
                }
                matcher = getMatcherAgainstContent("\\.load\\('(.+?)'\\)");
                if (!matcher.find()) {
                    throw new PluginImplementationException("Cannot find download URL (2)");
                }
                method = getMethodBuilder().setReferer(fileURL).setAction(matcher.group(1)).toGetMethod();
                downloadTask.sleep(31);
                if (!makeRedirectedRequest(method)) {
                    throw new ServiceConnectionProblemException();
                }
                matcher = getMatcherAgainstContent("\\.href\\s*=\\s*'(.+?)';");
                if (!matcher.find()) {
                    throw new PluginImplementationException("Cannot find download URL (3)");
                }
            } else {
                matcher = getMatcherAgainstContent("<form[^>]+action=\"([^\"]+)\"");
                if (!matcher.find()) {
                    throw new PluginImplementationException("Cannot find download URL (1)");
                }
            }
            String finalDownloadUrl = matcher.group(1);
            logger.info("Download URL: " + finalDownloadUrl);

            method = getMethodBuilder().setReferer(getFileUrl).setAction(finalDownloadUrl).toGetMethod();
            if (!tryDownloadAndSaveFile(method)) {
                checkProblems();
                throw new ServiceConnectionProblemException("Error starting download");
            }
        } else {
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize(String content) throws Exception {
        if (content.contains("file does not exist"))
            throw new URLNotAvailableAnymoreException("Such file does not exist or it has been removed for infringement of copyrights");

        Matcher matcher = getMatcherAgainstContent("eval\\s*\\(\\s*unescape\\s*\\(\\s*'(.+?)'");
        if (!matcher.find()) {
            throw new PluginImplementationException("File name not found (1)");
        }
        final String nameContent = PlugUtils.unescapeUnicode(matcher.group(1).replace("%u", "\\u"));
        matcher = PlugUtils.matcher("File name: <b title=\"(.+?)\"", nameContent);
        if (!matcher.find()) {
            throw new PluginImplementationException("File name not found (2)");
        }
        httpFile.setFileName(matcher.group(1));

        matcher = getMatcherAgainstContent("File size: <b[^<>]*?>(.+?)</b>");
        if (!matcher.find()) {
            throw new PluginImplementationException("File size not found");
        }
        httpFile.setFileSize(PlugUtils.getFileSizeFromString(matcher.group(1)));

        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        Matcher matcher;
        String content = getContentAsString();
        if (content.contains("already downloading"))
            throw new ServiceConnectionProblemException("Your IP is already downloading a file from our system. You cannot download more than one file in parallel.");
        matcher = Pattern.compile("Try in\\s*([0-9]+) minute", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE).matcher(content);
        if (matcher.find())
            throw new YouHaveToWaitException("You used up your limit for file downloading!", Integer.parseInt(matcher.group(1)) * 60 + 20);
        matcher = Pattern.compile("Try in\\s*([0-9]+) hour", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE).matcher(content);
        if (matcher.find())
            throw new YouHaveToWaitException("You used up your limit for file downloading!", Integer.parseInt(matcher.group(1)) * 60 * 60 + 20);
        matcher = Pattern.compile("Please try in\\s*([0-9]+) minute", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE).matcher(content);
        if (matcher.find())
            throw new YouHaveToWaitException("You used up your limit for file downloading!", Integer.parseInt(matcher.group(1)) * 60 + 20);
        matcher = Pattern.compile("Please try in\\s*([0-9]+) hour", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE).matcher(content);
        if (matcher.find())
            throw new YouHaveToWaitException("You used up your limit for file downloading!", Integer.parseInt(matcher.group(1)) * 60 * 60 + 20);
        matcher = Pattern.compile("Please try in\\s*([0-9]+):([0-9]+) hour", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE).matcher(content);
        if (matcher.find())
            throw new YouHaveToWaitException("You used up your limit for file downloading!", Integer.parseInt(matcher.group(1)) * 60 * 60 + Integer.parseInt(matcher.group(2)) * 60 + 20);
        matcher = PlugUtils.matcher("slots[^<]*busy", content);
        if (matcher.find())
            throw new YouHaveToWaitException("All downloading slots for your country are busy", 60);
        if (content.contains("file does not exist"))
            throw new URLNotAvailableAnymoreException("Such file does not exist or it has been removed for infringement of copyrights");
    }

    private boolean checkIsFolder() {
        return fileURL.contains("/folders/");
    }

    private void runFolder() throws Exception {
        List<URI> queue = new LinkedList<URI>();

        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            Matcher matcher = getMatcherAgainstContent("<a[^>]+href=\"/folders/[^\\?]+\\?page=(\\d+)\"[^>]*>\\d+</a>\\s*<a[^>]+href=\"[^\\?]+\\?page=(\\d+)\">&gt;&gt;&gt;</a>");
            int maxPageNumber = 1;
            if (matcher.find())
                maxPageNumber = Integer.parseInt(matcher.group(1));
            for (int i = 1; i <= maxPageNumber; i++) {
                if (i > 1) { // 1st page is already loaded - skip
                    GetMethod getPageMethod = getGetMethod(fileURL + "?page=" + i);
                    if (!makeRedirectedRequest(getPageMethod))
                        throw new ServiceConnectionProblemException();
                }
                matcher = getMatcherAgainstContent("<a\\s+href=\"(http://(www\\.)?depositfiles\\.com/([^/]{2}/)?files/[^\"]+)\"");
                while (matcher.find())
                    queue.add(new URI(matcher.group(1)));
            }
        } else
            throw new ServiceConnectionProblemException();

        getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, queue);

        httpFile.getProperties().put("removeCompleted", true);
    }
}
