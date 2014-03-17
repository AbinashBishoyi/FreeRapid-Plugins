package cz.vity.freerapid.plugins.services.protectmylinks;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import cz.vity.freerapid.utilities.LogUtils;
import org.apache.commons.httpclient.HttpMethod;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 */
class ProtectMyLinksFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(ProtectMyLinksFileRunner.class.getName());
    private final static int CAPTCHA_MAX = 0;//not worth trying
    private int captchaCounter = 1;

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);

        final HttpMethod method = getGetMethod(fileURL);
        if (!makeRedirectedRequest(method)) throw new ServiceConnectionProblemException();

        checkProblems();

        httpFile.setFileName(PlugUtils.getStringBetween(getContentAsString(), "<h1 class=\"pmclass\">", "</h1>"));

        stepCaptcha();

        parseWebsite();
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        if (content.contains("This data has been removed") || content.contains("<h1>Not Found</h1>"))
            throw new URLNotAvailableAnymoreException("File not found");
    }

    private void parseWebsite() throws Exception {
        final Matcher matcher = getMatcherAgainstContent("<a href='(.+?)' target='_blank'>");
        int start = 0;
        final List<URI> uriList = new LinkedList<URI>();
        while (matcher.find(start)) {
            final String link = decrypt(matcher.group(1));
            try {
                uriList.add(new URI(link));
            } catch (URISyntaxException e) {
                LogUtils.processException(logger, e);
            }
            start = matcher.end();
        }
        getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, uriList);
    }

    private void stepCaptcha() throws Exception {
        String password = null;
        if (getContentAsString().contains("Password :"))
            password = getPassword();

        while (getContentAsString().contains("secureCaptcha")) {
            final CaptchaSupport captchaSupport = getCaptchaSupport();
            final String captchaSrc = getMethodBuilder().setActionFromImgSrcWhereTagContains("secureCaptcha").getEscapedURI();
            logger.info("Captcha URL " + captchaSrc);

            if (getContentAsString().contains("Password is not valid"))
                password = getPassword();

            final String captcha;
            if (captchaCounter <= CAPTCHA_MAX) {
                captcha = PlugUtils.recognize(captchaSupport.getCaptchaImage(captchaSrc), "-d -1 -C A-Z-0-9");
                logger.info("OCR attempt " + captchaCounter + " of " + CAPTCHA_MAX + ", recognized " + captcha);
                captchaCounter++;
            } else {
                captcha = captchaSupport.getCaptcha(captchaSrc);
                if (captcha == null) throw new CaptchaEntryInputMismatchException();
                logger.info("Manual captcha " + captcha);
            }

            final MethodBuilder mb = getMethodBuilder().setReferer(fileURL).setBaseURL(fileURL).setActionFromFormByIndex(2, true).setParameter("captcha", captcha);
            if (password != null) mb.setParameter("passwd", password);
            if (!makeRedirectedRequest(mb.toPostMethod())) throw new ServiceConnectionProblemException();
        }
    }

    private String getPassword() throws Exception {
        final ProtectMyLinksPasswordUI ps = new ProtectMyLinksPasswordUI();
        if (getDialogSupport().showOKCancelDialog(ps, "Secured file on Protect-My-Links")) {
            return ps.getPassword();
        } else throw new NotRecoverableDownloadException("This file is secured with a password");
    }

    private String decrypt(final String url) throws Exception {
        final HttpMethod method = getMethodBuilder().setReferer(fileURL).setAction(url).toGetMethod();
        if (!makeRedirectedRequest(method)) throw new ServiceConnectionProblemException();
        int[] t = getInts();
        final String x = PlugUtils.getStringBetween(getContentAsString(), "x(\"", "\")");
        final StringBuilder r = new StringBuilder();
        int l = x.length(), b = 1024, i, j, p = 0, s = 0, w = 0;
        for (j = (int) Math.ceil((double) l / b); j > 0; j--) {
            for (i = Math.min(l, b); i > 0; i--, l--) {
                w |= (t[x.charAt(p++) - 48]) << s;
                if (s > 0) {
                    r.append((char) (165 ^ w & 255));
                    w >>= 8;
                    s -= 2;
                } else {
                    s = 6;
                }
            }
            r.append('\n');
        }
        return getMethodBuilder(r.toString()).setActionFromIFrameSrcWhereTagContains("scrolling=\"auto\"").getEscapedURI();
    }

    private int[] getInts() throws Exception {
        final String c = PlugUtils.getStringBetween(getContentAsString(), "c=\"", "\"");
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < c.length(); i++) {
            if (i % 3 == 0) sb.append('%');
            else sb.append(c.charAt(i));
        }
        final String unescaped = URLDecoder.decode(sb.toString(), "UTF-8");
        final String array = PlugUtils.getStringBetween(unescaped, "Array(", ")");
        final String[] strings = array.replaceAll("\\s", "").split(",");
        final int[] ints = new int[strings.length];
        for (int i = 0; i < strings.length; i++) {
            ints[i] = Integer.parseInt(strings[i]);
        }
        return ints;
    }

    @Override
    protected String getBaseURL() {
        return "http://www.protect-my-links.com";
    }

}