package cz.vity.freerapid.plugins.services.linksave;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import cz.vity.freerapid.utilities.LogUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

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
 * @author Arthur Gunawan, ntoskrnl
 */
class LinksaveFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(LinksaveFileRunner.class.getName());

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();

            while (getContentAsString().contains("captcha")) {
                if (!makeRedirectedRequest(stepCaptcha())) throw new ServiceConnectionProblemException();
                if (getContentAsString().contains("Wrong code")) {
                    if (!makeRedirectedRequest(method)) throw new ServiceConnectionProblemException();
                }
            }

            //preparations - these will be used as progress indicators
            long size = 0;
            int count = 1;
            final Matcher prep = getMatcherAgainstContent("<td align=\"center\">(?:&lt;)?([^<>]+?)</td>");
            if (prep.find(0)) {
                size = PlugUtils.getFileSizeFromString(prep.group(1));
                httpFile.setFileSize(size);
            }
            if (prep.find(prep.end())) {
                count = Integer.valueOf(prep.group(1));
            }
            //avoid division by zero, just in case
            if (count < 1) count = 1;
            httpFile.setState(DownloadState.GETTING);

            //first check for "premiumlinks" AKA unprotected plaintext links
            Matcher matcher = getMatcherAgainstContent("(?s)<textarea[^<>]*>(.+?)</textarea>");
            if (matcher.find()) {
                final String found = matcher.group(1).trim();
                final String[] split = found.split("\\s+");

                final List<URI> uriList = new LinkedList<URI>();
                for (String link : split) {
                    link = link.trim();
                    if (link == null || link.isEmpty()) continue;
                    try {
                        uriList.add(new URI(link));
                    } catch (URISyntaxException e) {
                        LogUtils.processException(logger, e);
                    }
                }
                httpFile.setDownloaded(size);
                getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, uriList);

                return;
            }

            //then check for "webprotection" links
            final List<URI> uriList = new LinkedList<URI>();
            int page = 1;
            String content;
            int i = 0;
            do {
                if (page > 1) {
                    final HttpMethod pageMethod = getMethodBuilder().setReferer(fileURL).setAction(fileURL + "?s=" + page + "#down").toGetMethod();
                    if (!makeRedirectedRequest(pageMethod)) throw new ServiceConnectionProblemException();
                }
                content = getContentAsString();
                matcher = getMatcherAgainstContent("href=\"(http://.+?)\" onclick=\"javascript:");
                if (matcher.find()) {
                    int start = 0;
                    while (matcher.find(start)) {
                        final String link = unWebProtect(matcher.group(1));
                        try {
                            uriList.add(new URI(link));
                        } catch (URISyntaxException e) {
                            LogUtils.processException(logger, e);
                        }
                        start = matcher.end();
                        httpFile.setDownloaded(++i * (size / count));
                    }
                } else break;
            } while (content.contains(">[" + ++page + "]<"));

            if (!uriList.isEmpty()) {
                httpFile.setDownloaded(size);
                getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, uriList);
            } else {
                //this might happen eg. if only containers are available
                throw new NotRecoverableDownloadException("No download links found");
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        if (content.contains("Folder not found") || content.contains("<h1>404 - Not Found</h1>")) {
            throw new URLNotAvailableAnymoreException("Folder not found");
        }
    }

    private String unWebProtect(final String url) throws Exception {
        HttpMethod method = getMethodBuilder().setReferer(fileURL).setAction(url).toGetMethod();
        if (!makeRedirectedRequest(method)) throw new ServiceConnectionProblemException();

        method = getMethodBuilder().setReferer(method.getURI().toString()).setActionFromIFrameSrcWhereTagContains("scrolling=\"auto\"").toGetMethod();
        if (!makeRedirectedRequest(method)) throw new ServiceConnectionProblemException();

        if (getContentAsString().contains("IIIIIl(\"")) {
            final String toUnescape = PlugUtils.getStringBetween(getContentAsString(), "IIIIIl(\"", "\"");
            final String unescaped = URLDecoder.decode(toUnescape, "UTF-8");
            final String toDecrypt = PlugUtils.getStringBetween(unescaped, "a('", "')");
            final String decrypted = decrypt(toDecrypt);

            final Matcher matcher = PlugUtils.matcher("(?:location\\.replace\\('|src=\"|URL=)(.+?)['\"\\)]", decrypted);
            if (!matcher.find()) throw new PluginImplementationException("Problem with final download link");

            method = getMethodBuilder(decrypted).setReferer(method.getURI().toString()).setAction(matcher.group(1)).toGetMethod();
            if (!makeRedirectedRequest(method)) throw new ServiceConnectionProblemException();
        }

        final String src = PlugUtils.getStringBetween(getContentAsString(), "<iframe src=\"", "\"");

        return PlugUtils.unescapeHtml(src);
    }

    private String decrypt(String m) {
        String c = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=";
        String b = "";
        int i = 0;
        m = m.replaceAll("[^A-Za-z0-9\\+\\=]", "");

        do {
            int i1 = i;
            int i2 = i + 1;
            int i3 = i + 2;
            int i4 = i + 3;
            i = i + 4;
            int g = c.indexOf(m.charAt(i1));
            int h = c.indexOf(m.charAt(i2));
            int k = c.indexOf(m.charAt(i3));
            int l = c.indexOf(m.charAt(i4));
            int d = (g << 2) | (h >> 4);
            int e = ((h & 15) << 4) | (k >> 2);
            int f = ((k & 3) << 6) | l;
            char nCode = (char) d;
            b = b + nCode;
            if (k != 64) {
                nCode = (char) e;
                b = b + nCode;
            }
            if (l != 64) {
                nCode = (char) f;
                b = b + nCode;
            }
        } while (i < m.length());

        return b;
    }

    private HttpMethod stepCaptcha() throws Exception {
        final CaptchaSupport captchaSupport = getCaptchaSupport();
        final String captchaURL = getMethodBuilder().setActionFromImgSrcWhereTagContains("captcha").getEscapedURI();
        logger.info("Captcha URL " + captchaURL);

        final String captcha = captchaSupport.getCaptcha(captchaURL);
        if (captcha == null) throw new CaptchaEntryInputMismatchException();
        logger.info("Manual captcha " + captcha);

        return getMethodBuilder()
                .setReferer(fileURL)
                .setBaseURL(fileURL)
                .setActionFromFormWhereTagContains("captcha", true)
                .setParameter("code", captcha)
                .toPostMethod();
    }

    @Override
    protected String getBaseURL() {
        return "http://linksave.in/";
    }

}
