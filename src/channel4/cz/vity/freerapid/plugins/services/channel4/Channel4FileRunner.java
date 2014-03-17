package cz.vity.freerapid.plugins.services.channel4;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.rtmp.AbstractRtmpRunner;
import cz.vity.freerapid.plugins.services.rtmp.RtmpSession;
import cz.vity.freerapid.plugins.services.rtmp.SwfVerificationHelper;
import cz.vity.freerapid.plugins.services.tunlr.Tunlr;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.HttpMethod;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.Charset;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 */
class Channel4FileRunner extends AbstractRtmpRunner {
    private final static Logger logger = Logger.getLogger(Channel4FileRunner.class.getName());
    private final static byte[] DECRYPT_KEY = "STINGMIMI".getBytes(Charset.forName("UTF-8"));
    private static SwfVerificationHelper helper;
    private String id;

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        checkFileUrl();
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
        final String name;
        final Matcher matcher = getMatcherAgainstContent("id=\"brandLink\">\\s*(.+?)(?: \\| Series (\\d+))?(?: \\| Episode (\\d+))? \\- (.+?)\\s*</");
        if (matcher.find()) {
            final String program = matcher.group(1).replace(": ", " - ");
            final String seasonNum = matcher.group(2);
            final String episodeNum = matcher.group(3);
            final String episode = matcher.group(4).replace(": ", " - ");
            if (seasonNum == null && episodeNum == null) {
                if (program.equals(episode)) {
                    name = program;
                } else {
                    name = String.format("%s - %s", program, episode);
                }
            } else if (seasonNum == null) {
                final int episodeNumI = Integer.parseInt(episodeNum);
                name = String.format("%s - E%02d - %s", program, episodeNumI, episode);
            } else if (episodeNum == null) {
                final int seasonNumI = Integer.parseInt(seasonNum);
                name = String.format("%s - S%02d - %s", program, seasonNumI, episode);
            } else {
                final int seasonNumI = Integer.parseInt(seasonNum);
                final int episodeNumI = Integer.parseInt(episodeNum);
                name = String.format("%s - S%02dE%02d - %s", program, seasonNumI, episodeNumI, episode);
            }
        } else {
            try {
                name = PlugUtils.getStringBetween(getContentAsString(), "<title>", "- 4oD - Channel 4</title>");
            } catch (final PluginImplementationException e) {
                throw new PluginImplementationException("File name not found");
            }
        }
        httpFile.setFileName(name + ".flv");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private void checkFileUrl() throws ErrorDuringDownloadingException {
        final Matcher matcher = PlugUtils.matcher("^(.+?/programmes/.+?/4od).+?(\\d+)$", fileURL);
        if (!matcher.find()) {
            throw new PluginImplementationException("Error parsing file URL");
        }
        id = matcher.group(2);
        fileURL = matcher.group(1) + "/player/" + id;
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        checkFileUrl();
        HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize();
            final SwfVerificationHelper helper = getSwfvHelper();
            method = getGetMethod("http://ais.channel4.com/asset/" + id);
            if (!client.getSettings().isProxySet()) {
                Tunlr.setupMethod(method);
            }
            makeRedirectedRequest(method);
            if (getContentAsString().contains("status=\"ERROR\"")) {
                if (getContentAsString().contains("ip tested positive for anonymous activity")) {
                    throw new NotRecoverableDownloadException("Channel4 noticed that you are trying to access them through a proxy");
                } else if (getContentAsString().contains("Asset not found")) {
                    throw new URLNotAvailableAnymoreException("Video not found");
                } else {
                    try {
                        throw new NotRecoverableDownloadException(
                                PlugUtils.getStringBetween(getContentAsString(), "<description>", "</description>"));
                    } catch (PluginImplementationException e) {
                        throw new NotRecoverableDownloadException("Error fetching playlist");
                    }
                }
            }
            logger.info(getContentAsString());
            final String streamUri = PlugUtils.getStringBetween(getContentAsString(), "<streamUri>", "</streamUri>");
            if (streamUri.startsWith("http")) {
                throw new PluginImplementationException("This link is currently not supported by the plugin");
            }
            final Matcher matcher = PlugUtils.matcher("(rtmpe?)://(.+?)/(.+?/)(mp4:.+)", streamUri);
            if (!matcher.find()) {
                throw new PluginImplementationException("Error parsing stream URI");
            }
            final boolean ak = "ak".equalsIgnoreCase(PlugUtils.getStringBetween(getContentAsString(), "<cdn>", "</cdn>"));
            final String auth = getAuthParams(getContentAsString(), ak);
            final String playName = matcher.group(4);
            final RtmpSession rtmpSession = new RtmpSession(
                    matcher.group(2), 1935, matcher.group(3) + auth, ak ? playName : playName + auth, matcher.group(1));
            rtmpSession.getConnectParams().put("swfUrl", helper.getSwfURL());
            rtmpSession.getConnectParams().put("pageUrl", fileURL);
            helper.setSwfVerification(rtmpSession, client);
            tryDownloadAndSaveFile(rtmpSession);
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        if (getContentAsString().contains("This page cannot be found")
                || getContentAsString().contains("Please try again later")
                || getContentAsString().contains("Programme Unavailable")) {
            throw new URLNotAvailableAnymoreException("Page not found");
        }
    }

    private SwfVerificationHelper getSwfvHelper() throws ErrorDuringDownloadingException {
        final String url = "http://www.channel4.com/static/programmes/asset/flash/swf/"
                + PlugUtils.getStringBetween(getContentAsString(), "var fourodPlayerFile = '", "';");
        synchronized (Channel4FileRunner.class) {
            if (helper == null || !url.equals(helper.getSwfURL())) {
                helper = new SwfVerificationHelper(url);
            }
            return helper;
        }
    }

    private static String getAuthParams(final String content, final boolean ak) throws Exception {
        final String token = PlugUtils.getStringBetween(content, "<token>", "</token>");
        final Cipher cipher = Cipher.getInstance("Blowfish/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(DECRYPT_KEY, "Blowfish"));
        final byte[] decrypted = cipher.doFinal(Base64.decodeBase64(token));
        final String h = new String(decrypted, "UTF-8").replace('+', '-').replace('/', '_').replace("=", "");
        logger.info("h = " + h);
        if (ak) {
            final String aifp = PlugUtils.getStringBetween(content, "<fingerprint>", "</fingerprint>");
            final String slist = PlugUtils.getStringBetween(content, "<slist>", "</slist>");
            return "?auth=" + h + "&aifp=" + aifp + "&slist=" + slist;
        } else {
            final String e = PlugUtils.getStringBetween(content, "<e>", "</e>");
            return "?e=" + e + "&h=" + h;
        }
    }

}