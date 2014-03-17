package cz.vity.freerapid.plugins.services.badongo;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.badongo.captcha.CaptchaReader;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import cz.vity.freerapid.utilities.LogUtils;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethod;

import java.awt.image.BufferedImage;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Kajda, ntoskrnl
 */
class BadongoFileRunner extends AbstractRunner {
    private static final Logger logger = Logger.getLogger(BadongoFileRunner.class.getName());
    private static final String SERVICE_WEB = "http://www.badongo.com";
    private static final int CAPTCHA_MAX = 10;
    private int captchaCounter = 1;
    private String referer = fileURL;

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        fileURL = checkFileURL(fileURL);
        final HttpMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    @Override
    public void run() throws Exception {
        super.run();
        fileURL = checkFileURL(fileURL);
        logger.info("Starting download in TASK " + fileURL);
        HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize();
            final String[] filePath = new URI(fileURL).getPath().split("/");
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
                            method = getMethodBuilder().setReferer(fileURL).setAction(matcher.group(1)).toGetMethod();
                            downloadFile(method);
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
                    processCaptchaForm();
                    final String url = processJavaScript();
                    method = getMethodBuilder().setReferer(referer).setAction(url).toGetMethod();
                    downloadFile(method);
                }
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private String checkFileURL(String fileURL) throws Exception {
        addCookie(new Cookie(".badongo.com", "badongoL", "en", "/", 86400, false));
        if (fileURL.endsWith("/")) {
            fileURL = fileURL.substring(0, fileURL.length() - 1);
        }
        fileURL = fileURL.replaceFirst("/cfile/", "/file/").replaceFirst("/cpic/", "/pic/").replaceFirst("/cvid/", "/vid/");
        final String filePath = new URI(fileURL).getPath();
        final Matcher matcher = PlugUtils.matcher("^/([a-zA-Z]{2})/", filePath);
        if (matcher.find()) {
            final String language = matcher.group(1);
            if (!language.equals("en")) {
                fileURL = fileURL.replaceFirst("/" + language + "/", "/en/");
            }
        } else {
            fileURL = fileURL.replaceFirst(Pattern.quote(filePath), "/en" + filePath);
        }
        return fileURL;
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        if (content.contains("This file has been deleted")) {
            throw new URLNotAvailableAnymoreException("This file has been deleted because it has been inactive for over 30 days");
        }
        if (content.contains("This File Has Been Deactivated")) {
            throw new URLNotAvailableAnymoreException("This file has been deactivated and is no longer available");
        }
        if (content.contains("File not found")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        if (content.contains("Page Not Found")) {
            throw new URLNotAvailableAnymoreException("Page not found");
        }
        if (content.contains("FREE MEMBER WAITING PERIOD")) {
            throw new YouHaveToWaitException("You are receiving this message because you are a FREE member and you are limited to 1 concurrent download and a 35 second waiting period between downloading Files", 35);
        }
        if (content.contains("You have exceeded your Download Quota")) {
            throw new YouHaveToWaitException("You have exceeded your Download Quota. Non-Members are allowed to download a maximum of 100 MB (6:00-10:00 CST GMT-6), 800MB (10:00-20:00 CST GMT-6), 600MB (20:00-6:00 CST GMT-6) every hour and Free Members are allowed to download a maximum of 125 MB (6:00-10:00 CST GMT-6), 1000 MB (10:00-20:00 CST GMT-6), 800 MB (20:00-6:00 CST GMT-6) every hour", 60 * 60);
        }
        if (content.contains("The link timed out")) {
            throw new ServiceConnectionProblemException("The link timed out");
        }
        if (content.contains("Please wait while processing")) {
            throw new ServiceConnectionProblemException("Please wait while processing");
        }
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, getContentAsString(), "class=\"finfo\">", "<");
        PlugUtils.checkFileSize(httpFile, getContentAsString(), "Filesize :", "<");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private void processCaptchaForm() throws Exception {
        do {
            final String redirectURL = fileURL + "?rs=displayCaptcha&rst=&rsrnd=" + System.currentTimeMillis() + "&rsargs[]=yellow";
            final HttpMethod getMethod = getMethodBuilder().setReferer(fileURL).setAction(redirectURL).toGetMethod();
            if (makeRedirectedRequest(getMethod)) {
                final String content = getContentAsString().replaceAll("\\\\\"", "\"");
                final HttpMethod method = stepCaptcha(content);
                if (!makeRedirectedRequest(method)) {
                    throw new ServiceConnectionProblemException();
                }
                referer = method.getURI().toString();
            } else {
                throw new ServiceConnectionProblemException();
            }
        } while (getContentAsString().contains("<div id=\"link\">"));
    }

    private void downloadFile(HttpMethod httpMethod) throws Exception {
        if (!tryDownloadAndSaveFile(httpMethod)) {
            checkProblems();
            throw new ServiceConnectionProblemException("Error starting download");
        }
    }

    private void parseWebsite() {
        final Matcher matcher = getMatcherAgainstContent("href=\"(" + Pattern.quote(fileURL.replaceFirst("/en/", "/")) + "/.+?)\"");
        final List<URI> uriList = new LinkedList<URI>();
        while (matcher.find()) {
            try {
                uriList.add(new URI(matcher.group(1)));
            } catch (URISyntaxException e) {
                LogUtils.processException(logger, e);
            }
        }
        getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, uriList);
    }

    private HttpMethod stepCaptcha(final String content) throws Exception {
        final CaptchaSupport captchaSupport = getCaptchaSupport();
        final String captchaURL = getMethodBuilder(content).setActionFromImgSrcWhereTagContains("").getEscapedURI();
        logger.info("Captcha URL " + captchaURL);
        final String captcha;
        if (captchaCounter <= CAPTCHA_MAX) {
            final BufferedImage captchaImage = captchaSupport.getCaptchaImage(captchaURL);
            captcha = CaptchaReader.recognize(captchaImage);
            if (captcha == null) {
                logger.info("Could not separate captcha letters (attempt " + captchaCounter + " of " + CAPTCHA_MAX + ")");
            }
            logger.info("Attempt " + captchaCounter + " of " + CAPTCHA_MAX + ", OCR recognized " + captcha);
            captchaCounter++;
        } else {
            captcha = captchaSupport.getCaptcha(captchaURL);
            if (captcha == null) throw new CaptchaEntryInputMismatchException();
            logger.info("Manual captcha " + captcha);
        }
        return getMethodBuilder(content)
                .setReferer(fileURL)
                .setActionFromFormByName("downForm", true)
                .setParameter("user_code", captcha)
                .toPostMethod();
    }

    private String processJavaScript() throws Exception {
        String content = getContentAsString();

        Matcher matcher = PlugUtils.matcher("function sajax_do_call\\([^\\}]+?uri = \"(.+?)\"", content);
        if (!matcher.find()) throw new PluginImplementationException("AJAX URI not found");
        final String ajaxUri = matcher.group(1);
        matcher = PlugUtils.matcher("gflFunc\\((.+?)\\)", content);
        if (!matcher.find()) throw new PluginImplementationException("AJAX parameters not found");
        final List<String> ajaxParams = new ArrayList<String>();
        matcher = PlugUtils.matcher("getFileLinkInitOpt\\['(.+?)'\\]", matcher.group(1));
        while (matcher.find()) {
            ajaxParams.add(matcher.group(1));
        }
        if (ajaxParams.isEmpty()) throw new PluginImplementationException("AJAX params is empty");
        final String finalParam = PlugUtils.getStringBetween(content, "= dlUrl + \"", "\"");

        final Map<String, String> params = new LinkedHashMap<String, String>();
        params.put("id", PlugUtils.getStringBetween(content, "getFileLinkId = '", "';"));
        params.put("type", PlugUtils.getStringBetween(content, "getFileLinkType = '", "';"));
        params.put("ext", content.contains("getFileLinkPart = '';") ? "" : PlugUtils.getStringBetween(content, "getFileLinkPart = '", "';"));
        params.put("f", "download:init");
        matcher = PlugUtils.matcher("\\);\\s*?window\\.getFileLinkInitOpt = \\{(.+?)\\};", content);
        if (!matcher.find()) throw new PluginImplementationException("initOpt not found");
        matcher = PlugUtils.matcher("'(.+?)'\\s*?:\\s*?'(.*?)'", matcher.group(1));
        while (matcher.find()) {
            params.put(matcher.group(1), matcher.group(2));
        }

        HttpMethod method = createMethod(params);
        if (!makeRedirectedRequest(method)) throw new ServiceConnectionProblemException();
        for (int i = 0; ; i++) {
            if (i > 5) {
                logger.warning("I don't think this is going anywhere...");
                throw new PluginImplementationException("Cannot proceed from AJAX calls");
            }
            content = getContentAsString();
            matcher = PlugUtils.matcher("(?s)= \\{(.+?)\\}", content);
            if (matcher.find()) {
                matcher = PlugUtils.matcher("'(.+?)'\\s*?:\\s*?'(.*?)'", matcher.group(1));
                while (matcher.find()) {
                    params.put(matcher.group(1), matcher.group(2));
                }
            }
            matcher = PlugUtils.matcher("check_n[^\r\n]*?= \"(\\d+?)\"", content);
            if (matcher.find()) {
                downloadTask.sleep(Integer.parseInt(matcher.group(1)) + 2);
            }
            if (content.contains("getFileLinkCanDownload = 1;")) {
                break;
            }
            params.put("f", "download:check");
            method = createMethod(params);
            if (!makeRedirectedRequest(method)) throw new ServiceConnectionProblemException();
        }

        final StringBuilder sb = new StringBuilder(ajaxUri)
                .append("?rs=")
                .append("vid".equals(params.get("type")) ? "getVidLink" : "getFileLink")
                .append("&rst=&rsrnd=")
                .append(System.currentTimeMillis())
                .append("&rsargs[]=0&rsargs[]=yellow");
        for (final String s : ajaxParams) {
            sb.append("&rsargs[]=").append(params.get(s));
        }
        method = getMethodBuilder().setReferer(referer).setAction(sb.toString()).toGetMethod();
        if (!makeRedirectedRequest(method)) throw new ServiceConnectionProblemException();

        final String url = PlugUtils.getStringBetween(getContentAsString(), "doDownload(\\'", "\\');");
        method = getMethodBuilder().setReferer(referer).setAction(url + finalParam + "?zenc=").toGetMethod();
        if (!makeRedirectedRequest(method)) throw new ServiceConnectionProblemException();
        referer = method.getURI().toString();

        content = getContentAsString();
        return PlugUtils.getStringBetween(content, "window.location.href = '", "';");
    }

    private HttpMethod createMethod(final Map<String, String> params) throws ErrorDuringDownloadingException {
        final MethodBuilder mb = getMethodBuilder().setReferer(referer).setAction("http://www.badongo.com/ajax/prototype/ajax_api_filetemplate.php");
        for (final Map.Entry<String, String> entry : params.entrySet()) {
            mb.setParameter(entry.getKey(), entry.getValue());
        }
        final HttpMethod method = mb.toPostMethod();
        method.addRequestHeader("X-Requested-With", "XMLHttpRequest");
        return method;
    }

    /*
    private static String decryptJavaScript(final String content) throws PluginImplementationException {
        final StringBuilder sb = new StringBuilder();
        try {
            final Matcher matcher = PlugUtils.matcher("(eval[^\r\n]+)", content);
            while (matcher.find()) {
                final String group = matcher.group(1).replaceAll("%(?:\\P{XDigit}.|.\\P{XDigit})", "");
                final Matcher matcher1 = PlugUtils.matcher("\\(((?:\\p{Alpha}\\d{1,2},?)+?)\\)", group);
                final Matcher matcher2 = PlugUtils.matcher("\\(\"((?:%\\p{XDigit}{2})+?)\"\\)", group);
                final Matcher matcher3 = PlugUtils.matcher("\\(((?:\"(?:%\\p{XDigit}{2})*?\",)+?(?:\"(?:%\\p{XDigit}{2})*?\"))\\)", group);
                if (matcher1.find() && matcher2.find() && matcher3.find()) {
                    final String[] split1 = matcher1.group(1).split(",");
                    final String[] split2 = URLDecoder.decode(matcher2.group(1), "UTF-8").split("\\+");
                    final String[] split3 = matcher3.group(1).split(",");
                    if (split1.length != split2.length || split1.length != split3.length) {
                        throw new PluginImplementationException("Error decrypting JavaScript");
                    }
                    final Map<String, String> map = new HashMap<String, String>(split1.length);
                    for (int i = 0; i < split1.length; i++) {
                        map.put(split1[i], URLDecoder.decode(split3[i].substring(1, split3[i].length() - 1), "UTF-8"));
                    }
                    for (final String s : split2) {
                        final String t = map.get(s);
                        if (t == null) {
                            throw new PluginImplementationException("Error decrypting JavaScript");
                        }
                        sb.append(t);
                    }
                    sb.append("\n\n----------\n\n");
                }
            }
            if (sb.length() <= 0) {
                throw new PluginImplementationException("Error decrypting JavaScript");
            }
        } catch (PluginImplementationException e) {
            logger.warning(sb.toString());
            throw e;
        } catch (Exception e) {
            logger.warning(sb.toString());
            throw new PluginImplementationException("Error decrypting JavaScript", e);
        }
        return sb.toString();
    }
    */

    @Override
    protected String getBaseURL() {
        return SERVICE_WEB;
    }

}