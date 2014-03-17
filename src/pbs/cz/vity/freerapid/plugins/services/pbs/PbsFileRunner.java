package cz.vity.freerapid.plugins.services.pbs;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.rtmp.AbstractRtmpRunner;
import cz.vity.freerapid.plugins.services.rtmp.RtmpSession;
import cz.vity.freerapid.plugins.services.tunlr.Tunlr;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethod;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.Charset;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 */
class PbsFileRunner extends AbstractRtmpRunner {
    private final static Logger logger = Logger.getLogger(PbsFileRunner.class.getName());
    private final static byte[] DECRYPT_KEY = "RPz~i4p*FQmx>t76".getBytes(Charset.forName("UTF-8"));

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final String url = "http://video.pbs.org/videoPlayerInfo/" + getId() + "/";
        final HttpMethod method = getGetMethod(url);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private String getId() throws Exception {
        final Matcher matcher = PlugUtils.matcher("http://video\\.pbs\\.org/video/(\\d+)", fileURL);
        if (!matcher.find()) {
            throw new PluginImplementationException("Error parsing file URL");
        }
        return matcher.group(1);
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        final String name = PlugUtils.getStringBetween(getContentAsString(), "<title>", "</title>");
        httpFile.setFileName(name.replace(": ", " - ") + ".flv");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        runCheck();
        final String url = getUrl();
        final HttpMethod method = getGetMethod(url);
        if (!client.getSettings().isProxySet()) {
            Tunlr.setupMethod(method);
        }
        makeRequest(method);
        checkProblems();
        final Header location = method.getResponseHeader("Location");
        if (location == null) {
            throw new PluginImplementationException("No redirect location");
        }
        final String[] rtmpData = location.getValue().split("mp4:");
        if (rtmpData.length != 2) {
            throw new PluginImplementationException("Error parsing RTMP URL");
        }
        final RtmpSession rtmpSession = new RtmpSession(rtmpData[0], "mp4:" + rtmpData[1]);
        rtmpSession.getConnectParams().put("pageUrl", fileURL);
        tryDownloadAndSaveFile(rtmpSession);
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        if (getContentAsString().contains("We were unable to find the page that was requested")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        if (getContentAsString().contains("unavailable in your region")) {
            throw new NotRecoverableDownloadException("This video is not available in your region");
        }
        if (getContentAsString().contains("Media is not available")) {
            throw new PluginImplementationException("Media is not available");
        }
    }

    private String getUrl() throws Exception {
        final String releaseUrl = PlugUtils.getStringBetween(getContentAsString(), "<releaseURL>", "</releaseURL>");
        final String[] data = releaseUrl.split("\\$");
        if (data.length != 3) {
            throw new PluginImplementationException("Error parsing 'releaseURL'");
        }
        final byte[] iv = Hex.decodeHex(data[1].toCharArray());
        final byte[] cipherText = Base64.decodeBase64(data[2]);
        final Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(DECRYPT_KEY, "AES"), new IvParameterSpec(iv));
        return new String(cipher.doFinal(cipherText), "UTF-8");
    }

}