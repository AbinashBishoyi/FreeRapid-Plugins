package cz.vity.freerapid.plugins.services.rapidgator;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadClientConsts;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.awt.*;
import java.awt.geom.QuadCurve2D;
import java.awt.image.BufferedImage;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author tong2shot, birchie
 */
class RapidGatorFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(RapidGatorFileRunner.class.getName());
    private final static long CAPTCHA_TIMEOUT = 25; // tried 30, but still caught captcha expired

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        setClientParameter(DownloadClientConsts.USER_AGENT, "Opera/9.80 (Windows NT 6.1; WOW64; U; pt) Presto/2.10.229 Version/11.62");
        addCookie(new Cookie(".rapidgator.net", "lang", "en", "/", 86400, false));
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
        final String filenameRegexRule = "Downloading:\\s*</strong>\\s*<a.+?>\\s*(\\S+)\\s*</a>\\s*</p>";
        final String filesizeRegexRule = "File size:\\s*<strong>(.+?)</strong>";

        final Matcher filenameMatcher = PlugUtils.matcher(filenameRegexRule, content);
        if (filenameMatcher.find()) {
            httpFile.setFileName(filenameMatcher.group(1));
        } else {
            throw new PluginImplementationException("File name not found");
        }

        final Matcher filesizeMatcher = PlugUtils.matcher(filesizeRegexRule, content);
        if (filesizeMatcher.find()) {
            httpFile.setFileSize(PlugUtils.getFileSizeFromString(filesizeMatcher.group(1)));
        } else {
            throw new PluginImplementationException("File size not found");
        }
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        setClientParameter(DownloadClientConsts.USER_AGENT, "Opera/9.80 (Windows NT 6.1; WOW64; U; pt) Presto/2.10.229 Version/11.62");
        addCookie(new Cookie(".rapidgator.net", "lang", "en", "/", 86400, false));
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            String contentAsString = getContentAsString();
            checkProblems();
            checkNameAndSize(contentAsString);
            final int waitTime = PlugUtils.getWaitTimeBetween(contentAsString, "var secs =", ";", TimeUnit.SECONDS);
            final String fileId = PlugUtils.getStringBetween(contentAsString, "var fid =", ";");
            HttpMethod httpMethod = getMethodBuilder()
                    .setReferer(fileURL)
                    .setAction("http://rapidgator.net/download/AjaxStartTimer")
                    .setParameter("fid", fileId)
                    .toGetMethod();
            httpMethod.addRequestHeader("X-Requested-With", "XMLHttpRequest");
            if (!makeRedirectedRequest(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            checkProblems();
            contentAsString = getContentAsString();
            final String sid = PlugUtils.getStringBetween(contentAsString, "\"sid\":\"", "\"}");
            downloadTask.sleep(waitTime + 1);
            httpMethod = getMethodBuilder()
                    .setReferer(fileURL)
                    .setAction("http://rapidgator.net/download/AjaxGetDownloadLink")
                    .setParameter("sid", sid)
                    .toGetMethod();
            httpMethod.addRequestHeader("X-Requested-With", "XMLHttpRequest");
            if (!makeRedirectedRequest(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            checkProblems();
            httpMethod = getMethodBuilder()
                    .setReferer(fileURL)
                    .setAction("http://rapidgator.net/download/captcha")
                    .toGetMethod();
            if (!makeRedirectedRequest(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            int captchaCounter = 0;
            while (getContentAsString().contains("/download/captcha")) {
                if (captchaCounter++ > 8) {
                    throw new PluginImplementationException("Unable to solve captcha");
                }
                httpMethod = stepCaptcha();
                if (!makeRedirectedRequest(httpMethod)) {
                    checkProblems();
                    throw new ServiceConnectionProblemException();
                }
            }
            checkProblems();
            httpMethod = getMethodBuilder()
                    .setAction(PlugUtils.getStringBetween(getContentAsString(), "location.href = '", "';"))
                    .toGetMethod();
            if (!tryDownloadAndSaveFile(httpMethod)) {
                checkProblems();
                logger.warning(getContentAsString());
                throw new ServiceConnectionProblemException("Error starting download");
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private HttpMethod stepCaptcha() throws Exception {
        if (getContentAsString().contains("NoScript.aspx")) {
            logger.info("Captcha Type 1");
            HttpMethod method = getMethodBuilder()
                    .setReferer("http://rapidgator.net/download/captcha")
                    .setActionFromIFrameSrcWhereTagContains("NoScript.aspx")
                    .toGetMethod();
            if (!makeRedirectedRequest(method)) {
                throw new ServiceConnectionProblemException();
            }
            final Matcher codeMatcher = PlugUtils.matcher("<td\\s*class=\"code\">(.+)</td>", getContentAsString());
            if (!codeMatcher.find()) {
                throw new PluginImplementationException("Captcha code not found");
            }
            final String codeTxt = codeMatcher.group(1);
            final Matcher challengeMatcher = PlugUtils.matcher("img\\s*src=\"(.+)\"\\s*width", getContentAsString());
            if (!challengeMatcher.find()) {
                throw new PluginImplementationException("Captcha challenge not found");
            }
            final String challengeImg = challengeMatcher.group(1);
            final CaptchaSupport captchaSupport = getCaptchaSupport();
            final String captchaTxt = captchaSupport.getCaptcha(challengeImg);
            if (captchaTxt == null) throw new CaptchaEntryInputMismatchException("No Input");
            MethodBuilder methodBuilder = getMethodBuilder()
                    .setReferer(fileURL)
                    .setAction("http://rapidgator.net/download/captcha")
                    .setParameter("adscaptcha_challenge_field", codeTxt)
                    .setParameter("adscaptcha_response_field", captchaTxt)
                    .setParameter("DownloadCaptchaForm[captcha]", "");
            return methodBuilder.toPostMethod();
        } else if (getContentAsString().contains("papi/challenge.noscript")) {
            logger.info("Captcha Type 2");
            final Matcher captchaKeyMatcher = getMatcherAgainstContent("papi/challenge\\.noscript\\?k=(.*?)\"");
            if (!captchaKeyMatcher.find()) {
                throw new PluginImplementationException("Captcha not found");
            }
            final String captchaKey = captchaKeyMatcher.group(1);
            HttpMethod httpMethod = getMethodBuilder()
                    .setReferer("http://rapidgator.net/download/captcha")
                    .setAction("http://api.solvemedia.com/papi/challenge.script")
                    .setParameter("k", captchaKey)
                    .toGetMethod();
            if (!makeRedirectedRequest(httpMethod)) {
                throw new ServiceConnectionProblemException();
            }
            String mediaType;
            int mediaTypeCounter = 0;
            do {
                /*
                httpMethod = getMethodBuilder()
                        .setReferer("http://rapidgator.net/download/captcha")
                        .setAction("http://api.solvemedia.com/papi/_puzzle.js")
                        .toGetMethod();
                if (!makeRedirectedRequest(httpMethod)) {
                    throw new ServiceConnectionProblemException();
                }
                final String fwv = PlugUtils.getStringBetween(getContentAsString(), "caps=caps+',", "'+'.'+String.fromCharCode");
                final StringBuilder fw = new StringBuilder(7);
                */
                final StringBuilder fwv = new StringBuilder(18);
                fwv.append("fwv/");
                for (int i = 0; i < 6; i++) {
                    fwv.append(Character.toChars(Math.random() > 0.2 ? new Random().nextInt(26) + 65 : new Random().nextInt(26) + 97));
                }
                fwv.append(".");
                for (int i = 0; i < 4; i++) {
                    fwv.append(Character.toChars(new Random().nextInt(26) + 97));
                }
                fwv.append(new Random().nextInt(99));
                final String captchaAction = "http://api.solvemedia.com/papi/_challenge.js" + "?k=" + captchaKey + ";f=_ACPuzzleUtil.callbacks%5B0%5D;l=en;t=img;s=standard;c=js,h5c,h5ct,svg,h5v,v/ogg,v/webm,h5a,a/ogg,ua/opera,ua/opera11,os/nt,os/nt6.1," +
                        fwv + ",jslib/jquery;ts=" + Long.toString(System.currentTimeMillis()).substring(0, 10) + ";th=white;r=" + Math.random();
                httpMethod = getMethodBuilder()
                        .setReferer("http://rapidgator.net/download/captcha")
                        .setAction(captchaAction)
                        .toGetMethod();
                if (!makeRedirectedRequest(httpMethod)) {
                    throw new ServiceConnectionProblemException();
                }
                final Matcher mediaTypeMatcher = getMatcherAgainstContent("\"mediatype\"\\s*:\\s*\"(.+?)\",");
                if (!mediaTypeMatcher.find()) {
                    throw new PluginImplementationException("Captcha media type not found");
                }
                mediaType = mediaTypeMatcher.group(1);
                logger.info("ATTEMPT " + mediaTypeCounter + ", mediaType = " + mediaType);
            }
            while (!mediaType.equals("img") && !mediaType.equals("html") && (mediaTypeCounter++ < 10)); //anticipate mediaType!=(img|html)

            final Matcher chidMatcher = getMatcherAgainstContent("\"chid\"\\s*:\\s*\"(.+?)\",");
            if (!chidMatcher.find()) {
                throw new PluginImplementationException("Captcha challenge ID not found");
            }
            final String chid = chidMatcher.group(1);
            final String captchaTxt;
            final String challengeImg = "http://api.solvemedia.com/papi/media?c=" + chid + ";w=300;h=150;fg=000000;bg=f8f8f8";
            final long captchaStartTime;
            if (mediaType.equals("img")) {
                client.setReferer("http://rapidgator.net/download/captcha");
                final CaptchaSupport captchaSupport = getCaptchaSupport();
                captchaStartTime = System.currentTimeMillis();
                captchaTxt = captchaSupport.getCaptcha(challengeImg);
                if (captchaTxt == null) throw new CaptchaEntryInputMismatchException("No Input");
            } else if (mediaType.equals("html")) {
                httpMethod = getMethodBuilder()
                        .setReferer("http://rapidgator.net/download/captcha")
                        .setAction(challengeImg)
                        .toGetMethod();
                if (!makeRedirectedRequest(httpMethod)) {
                    throw new ServiceConnectionProblemException();
                }
                logger.info(getContentAsString());
                captchaStartTime = System.currentTimeMillis();
                if (getContentAsString().contains("var slog =")) { // cryptic HTML captcha + recognizer
                    final String slog = PlugUtils.unescapeUnicode(PlugUtils.getStringBetween(getContentAsString(), "var slog = '", "';"));
                    final String secr = PlugUtils.unescapeUnicode(PlugUtils.getStringBetween(getContentAsString(), "var secr = '", "';"));
                    int cn = 0;
                    char[] captchaResponse = new char[slog.length()];
                    for (int i = 0; i < slog.length(); i++) {
                        char x = (char) ((secr.charAt(i) ^ (cn | 1) ^ (((cn++ & 1) != 0) ? i : 0) ^ 0x55) ^ (slog.charAt(i) ^ (cn | 1)
                                ^ (((cn++ & 1) != 0) ? i : 0) ^ 0x55));
                        captchaResponse[i] = x;
                    }
                    captchaTxt = new String(captchaResponse);
                } else { // canvas HTML captcha 
                    captchaTxt = getCaptchaSupport().askForCaptcha(drawHTMLCaptcha(getContentAsString(), 300, 150, Color.blue));
                    if (captchaTxt == null) throw new CaptchaEntryInputMismatchException("No Input");
                }

            } else {
                throw new ServiceConnectionProblemException("Captcha media type 'img' or 'html' not found");
            }
            if (((System.currentTimeMillis() - captchaStartTime) / 1000) > CAPTCHA_TIMEOUT)
                throw new YouHaveToWaitException("Retry request to avoid captcha expired", 5);
            MethodBuilder methodBuilder = getMethodBuilder()
                    .setReferer(fileURL)
                    .setAction("http://rapidgator.net/download/captcha")
                    .setParameter("adcopy_challenge", chid)
                    .setParameter("adcopy_response", captchaTxt)
                    .setParameter("DownloadCaptchaForm[captcha]", "");
            return methodBuilder.toPostMethod();
        } else {
            logger.info("Captcha Error");
            checkProblems();
            throw new PluginImplementationException("Captcha not found");
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("File not found")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        if (contentAsString.contains("Delay between downloads must be not less than")) {
            final String waitTime = PlugUtils.getStringBetween(contentAsString, "must be not less than", "min");
            throw new YouHaveToWaitException("Delay between downloads must be not less than " + waitTime + " minutes", 300);
        }
        if (contentAsString.contains("You have reached your daily downloads limit")) {
            throw new PluginImplementationException("You have reached your daily downloads limit");
        }
        if (contentAsString.contains("You have reached your hourly downloads limit")) {
            throw new YouHaveToWaitException("You have reached your hourly downloads limit", 15 * 60);
        }
        final Matcher uptoMatcher = getMatcherAgainstContent("You can download files up to (.+?) in free mode");
        if (uptoMatcher.find()) {
            throw new PluginImplementationException("You can download files up to " + uptoMatcher.group(1) + " in free mode");
        }
        if (contentAsString.contains("You can`t download not more than")) {
            throw new PluginImplementationException("You can`t download not more than 1 file at a time in free mode");
        }
        if (contentAsString.contains("Captcha expired")) {
            throw new YouHaveToWaitException("Captcha expired. Try again in 15 minutes", 300);
        }
        if (contentAsString.contains("file can be downloaded by premium")) {
            throw new NotRecoverableDownloadException("This file can be downloaded by premium only");
        }
    }

    private BufferedImage drawHTMLCaptcha(final String content, final int width, final int height, final Color color) {
        final BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D g = (Graphics2D) image.getGraphics();
        g.setColor(Color.white);
        g.drawRect(1, 1, width, height);
        g.fillRect(1, 1, width, height);
        g.setStroke(new BasicStroke(3));
        g.setColor(color);
        final Matcher matcher = PlugUtils.matcher("CM\\((\\d{1,3}),(\\d{1,3})\\);(.*?)CS\\(\\);\\s", content);
        while (matcher.find()) {
            int prevX, prevY;
            prevX = Integer.parseInt(matcher.group(1));
            prevY = Integer.parseInt(matcher.group(2));
            final Matcher matcher2 = PlugUtils.matcher("CL\\((\\d{1,3}),(\\d{1,3})\\);|CQ\\((\\d{1,3}),(\\d{1,3}),(\\d{1,3}),(\\d{1,3})\\);", matcher.group(3));
            while (matcher2.find()) {
                if (matcher2.group(0).contains("CL")) { //draw line
                    g.drawLine(prevX, prevY, Integer.parseInt(matcher2.group(1)), Integer.parseInt(matcher2.group(2)));
                    prevX = Integer.parseInt(matcher2.group(1));
                    prevY = Integer.parseInt(matcher2.group(2));
                } else if (matcher2.group(0).contains("CQ")) { //draw quadratic curve
                    final QuadCurve2D q = new QuadCurve2D.Float();
                    q.setCurve(prevX, prevY, Integer.parseInt(matcher2.group(3)), Integer.parseInt(matcher2.group(4)), Integer.parseInt(matcher2.group(5)), Integer.parseInt(matcher2.group(6)));
                    g.draw(q);
                    prevX = Integer.parseInt(matcher2.group(5));
                    prevY = Integer.parseInt(matcher2.group(6));
                }
            }
        }
        g.dispose();
        return image;
    }

}