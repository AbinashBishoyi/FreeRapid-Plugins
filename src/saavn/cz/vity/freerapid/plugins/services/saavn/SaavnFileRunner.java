package cz.vity.freerapid.plugins.services.saavn;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.rtmp.AbstractRtmpRunner;
import cz.vity.freerapid.plugins.services.rtmp.RtmpSession;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.HttpMethod;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class which contains main code
 *
 * @author tong2shot
 * @since 0.9u2
 */
class SaavnFileRunner extends AbstractRtmpRunner {
    private final static Logger logger = Logger.getLogger(SaavnFileRunner.class.getName());
    private final static byte[] SECRET_KEY = "38346591".getBytes(Charset.forName("UTF-8"));
    //private final static String SWF_URL = "http://www.saavn.com/dplayer/2.inviplayer.swf";

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final HttpMethod getMethod = getMediaInfoMethod().toGetMethod();
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize(PlugUtils.unescapeHtml(getContentAsString()));
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        if (isAlbum()) {
            final String album = PlugUtils.getStringBetween(content, "<meta itemprop=\"name\" content =\"", "\"").trim();
            httpFile.setFileName(album);
        } else {
            final String album = PlugUtils.getStringBetween(content, "\"album\":\"", "\"").trim();
            final String title = PlugUtils.getStringBetween(content, "\"title\":\"", "\"").trim();
            httpFile.setFileName(String.format("%s - %s.flv", album, title));
        }
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final HttpMethod method = getMediaInfoMethod().toGetMethod();
        if (makeRedirectedRequest(method)) {
            final String contentAsString = PlugUtils.unescapeHtml(getContentAsString());
            checkProblems();
            checkNameAndSize(contentAsString);
            if (isAlbum()) {
                parseAlbum();
            } else {
                final String url = PlugUtils.getStringBetween(contentAsString, "\"url\":\"", "\"").replace("\\/", "/");
                final String app = "ondemand";
                final String host = "r.saavncdn.com";
                final String play = decryptPlay(url);
                final RtmpSession rtmpSession = new RtmpSession(host, 1935, app, play, true);
                tryDownloadAndSaveFile(rtmpSession);
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("File Not Found")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    private boolean isAlbum() {
        return fileURL.contains("/album/");
    }

    private boolean isSongUrl(final String url) {
        return url.matches("http://(?:www\\.)?saavn\\.com/(?:s/#!/)?(s|p)/.+");
    }

    private MethodBuilder getMediaInfoMethod() throws BuildMethodException {
        return getMethodBuilder()
                .setReferer(fileURL)
                .setAction(fileURL.replaceFirst("s/#!/", ""))
                .setParameter("qt", "a")
                .setParameter("st", "1")
                .setParameter("t", String.valueOf(System.currentTimeMillis()));
    }

    private String decryptPlay(final String str) throws Exception {
        final Cipher cipher = Cipher.getInstance("DES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(SECRET_KEY, "DES"));
        return new String(cipher.doFinal(Base64.decodeBase64(str)), "UTF-8");
    }

    private void parseAlbum() throws Exception {
        final List<URI> uriList = new LinkedList<URI>();
        final Matcher trackMatcher = Pattern.compile("<div itemprop=\"track\".*?>(.+?)</div>", Pattern.DOTALL).matcher(getContentAsString());
        final Matcher urlMatcher = Pattern.compile("<meta itemprop=\"url\" content =\"(.+?)\"").matcher(getContentAsString());
        while (trackMatcher.find()) {
            urlMatcher.region(trackMatcher.start(1), trackMatcher.end(1));
            if (!urlMatcher.find()) {
                throw new PluginImplementationException("Song url not found");
            }
            final String url = urlMatcher.group(1);
            if (!isSongUrl(url)) {
                throw new PluginImplementationException("Unrecognized song url pattern"); //to prevent original link disappear when song url pattern unrecognized
            }
            uriList.add(new URI(url));
        }
        if (uriList.isEmpty()) {
            throw new PluginImplementationException("No song url found");
        }
        getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, uriList);
        httpFile.setState(DownloadState.COMPLETED);
        httpFile.getProperties().put("removeCompleted", true);
    }

}
