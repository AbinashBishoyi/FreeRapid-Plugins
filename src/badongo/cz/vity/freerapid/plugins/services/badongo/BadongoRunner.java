package cz.vity.freerapid.plugins.services.badongo;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import cz.vity.freerapid.utilities.LogUtils;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author Kajda
 * @since 0.82
 */
class BadongoFileRunner extends AbstractRunner {
    private static final Logger logger = Logger.getLogger(BadongoFileRunner.class.getName());
    private static final String SERVICE_WEB = "http://www.badongo.com";

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        fileURL = checkFileURL(fileURL);
        final GetMethod getMethod = getGetMethod(fileURL);

        if (makeRedirectedRequest(getMethod)) {
            checkSeriousProblems();
            checkNameAndSize();
        } else {
            throw new InvalidURLOrServiceProblemException("Invalid URL or service problem");
        }
    }

    @Override
    public void run() throws Exception {
        super.run();
        fileURL = checkFileURL(fileURL);
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod getMethod = getGetMethod(fileURL);

        if (makeRedirectedRequest(getMethod)) {
            checkAllProblems();
            checkNameAndSize();
            final URI fileURI = new URI(fileURL);
            final String[] filePath = fileURI.getPath().split("/");

            if (getContentAsString().contains("This file has been split into") && filePath.length <= 5) { // More files
                processCaptchaForm();
                parseWebsite();
                httpFile.getProperties().put("removeCompleted", true);
            } else { // One file
                final Matcher matcher;

                if (filePath[2].equals("pic")) {
                    if (makeRedirectedRequest(getGetMethod(fileURL + "?size=original"))) {
                        matcher = getMatcherAgainstContent("<img src=\"(.+?)\" border=\"0\">");

                        if (matcher.find()) {
                            client.setReferer(fileURL);
                            final String finalURL = matcher.group(1);
                            downloadFile(getGetMethod(finalURL));
                        } else {
                            throw new PluginImplementationException("Picture link was not found");
                        }
                    } else {
                        throw new ServiceConnectionProblemException();
                    }
                } else {
                    matcher = getMatcherAgainstContent("<a href=\"(.+?)\">Download File");

                    if (matcher.find()) {
                        fileURL = SERVICE_WEB + matcher.group(1);
                    }

                    processDownloadWithCaptcha();
                }
            }
        } else {
            throw new InvalidURLOrServiceProblemException("Invalid URL or service problem");
        }
    }

    private String checkFileURL(String fileURL) throws Exception {
        client.getHTTPClient().getState().addCookie(new Cookie(".badongo.com", "badongoL", "en", "/", 86400, false));

        if (fileURL.endsWith("/")) {
            fileURL = fileURL.substring(0, fileURL.length() - 1);
        }

        final URI fileURI = new URI(fileURL);
        final String filePath = fileURI.getPath();

        final Matcher matcher = PlugUtils.matcher("^/([a-zA-Z]{2})/", filePath);

        if (matcher.find()) {
            final String language = matcher.group(1);

            if (!language.equals("en")) {
                fileURL = fileURL.replaceFirst("/" + language + "/", "/en/");
            }
        } else {
            fileURL = fileURL.replaceFirst(filePath, "/en" + filePath);
        }

        return fileURL;
    }

    private void checkSeriousProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();

        if (contentAsString.contains("This file has been deleted")) {
            throw new URLNotAvailableAnymoreException("This file has been deleted because it has been inactive for over 30 days");
        }

        if (contentAsString.contains("File not found")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    private void checkAllProblems() throws ErrorDuringDownloadingException {
        checkSeriousProblems();
        final String contentAsString = getContentAsString();

        if (contentAsString.contains("FREE MEMBER WAITING PERIOD")) {
            throw new YouHaveToWaitException("You are receiving this message because you are a FREE member and you are limited to 1 concurrent download and a 35 second waiting period between downloading Files", 35);
        }

        if (contentAsString.contains("You have exceeded your Download Quota")) {
            throw new YouHaveToWaitException("You have exceeded your Download Quota. Non-Members are allowed to download a maximum of 100 MB (6:00-10:00 CST GMT-6), 800MB (10:00-20:00 CST GMT-6), 600MB (20:00-6:00 CST GMT-6) every hour and Free Members are allowed to download a maximum of 125 MB (6:00-10:00 CST GMT-6), 1000 MB (10:00-20:00 CST GMT-6), 800 MB (20:00-6:00 CST GMT-6) every hour", 60 * 60);
        }
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        Matcher matcher = getMatcherAgainstContent("class=\"finfo\">(.+?)<");

        if (matcher.find()) {
            final String fileName = matcher.group(1).trim();
            logger.info("File name " + fileName);
            httpFile.setFileName(fileName);

            matcher = getMatcherAgainstContent("Filesize : (.+?)<");

            if (matcher.find()) {
                final long fileSize = PlugUtils.getFileSizeFromString(matcher.group(1));
                logger.info("File size " + fileSize);
                httpFile.setFileSize(fileSize);
            } else {
                logger.warning("File size was not found");
                throw new PluginImplementationException();
            }
        } else {
            logger.warning("File name was not found");
            throw new PluginImplementationException();
        }

        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private void processDownloadWithCaptcha() throws Exception {
        processCaptchaForm();

        Matcher matcher = getMatcherAgainstContent("req\\.open\\(\"GET\", \"(.+?)/status\"");

        if (matcher.find()) {
            final String finalURL = matcher.group(1);
            client.setReferer(finalURL + "/ifr?pr=1&zenc=");
            final GetMethod getMethod = getGetMethod(finalURL + "/loc?pr=1");

            matcher = getMatcherAgainstContent("var check_n = (.+?);");

            final int waitSeconds = (matcher.find()) ? Integer.parseInt(matcher.group(1)) : 45;
            downloadTask.sleep(waitSeconds);
            downloadFile(getMethod);
        } else {
            throw new PluginImplementationException("Download link was not found");
        }
    }

    private void processCaptchaForm() throws Exception {
        while (!getContentAsString().contains("doDownload")) {
            client.setReferer(fileURL);
            final String redirectURL = fileURL + "?rs=displayCaptcha";
            final GetMethod getMethod = getGetMethod(redirectURL);

            if (makeRedirectedRequest(getMethod)) {
                final String contentAsString = getContentAsString().replaceAll("\\\\\"", "\"");

                if (contentAsString.contains("name=\"downForm\"")) {
                    final HttpMethod httpMethod = new MethodBuilder(contentAsString, client).setReferer(fileURL).setActionFromFormByName("downForm", true).setParameter("user_code", stepCaptcha(contentAsString)).toHttpMethod();

                    if (!makeRedirectedRequest(httpMethod)) {
                        throw new ServiceConnectionProblemException();
                    }
                } else {
                    throw new PluginImplementationException("Captcha form was not found");
                }
            } else {
                throw new ServiceConnectionProblemException();
            }
        }
    }

    private void downloadFile(HttpMethod httpMethod) throws Exception {
        if (!tryDownloadAndSaveFile(httpMethod)) {
            checkAllProblems();
            logger.warning(getContentAsString());
            throw new IOException("File input stream is empty");
        }
    }

    private void parseWebsite() {
        final Matcher matcher = getMatcherAgainstContent("href=\"(" + fileURL.replaceFirst("/en/", "/") + "/.+?)\"");
        int start = 0;
        final List<URI> uriList = new LinkedList<URI>();

        while (matcher.find(start)) {
            final String link = matcher.group(1);

            try {
                uriList.add(new URI(link));
            } catch (URISyntaxException e) {
                LogUtils.processException(logger, e);
            }

            start = matcher.end();
        }

        getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, uriList);
    }

    private String stepCaptcha(String contenAsString) throws ErrorDuringDownloadingException {
        final CaptchaSupport captchaSupport = getCaptchaSupport();

        final Matcher matcher = PlugUtils.matcher("img src=\"(.+?)\"", contenAsString);

        if (matcher.find()) {
            final String captchaSrc = SERVICE_WEB + matcher.group(1);
            logger.info("Captcha URL " + captchaSrc);
            final String captcha = captchaSupport.getCaptcha(captchaSrc);

            if (captcha == null) {
                throw new CaptchaEntryInputMismatchException();
            } else {
                return captcha;
            }
        } else {
            throw new PluginImplementationException("Captcha picture was not found");
        }
    }
}