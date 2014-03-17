package cz.vity.freerapid.plugins.services.yleareena;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.services.rtmp.AbstractRtmpRunner;
import cz.vity.freerapid.plugins.services.rtmp.RtmpSession;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.codec.binary.Base64;
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
class YleAreenaFileRunner extends AbstractRtmpRunner {
    private final static Logger logger = Logger.getLogger(YleAreenaFileRunner.class.getName());
    private final static String SWF_URL = "http://areena.yle.fi/static/player/1.2.8/flowplayer/flowplayer.commercial-3.2.7-encrypted.swf";
    private final static byte[] KEY = "hjsadf89hk123ghk".getBytes(Charset.forName("UTF-8"));

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final HttpMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        final Matcher matcher = getMatcherAgainstContent("<span itemprop=\"name\">\\s*(.+?)\\s*</span>(?:\\s*<span class=\"episode-number\">\\s*(.+?)\\s*</span>)?");
        if (!matcher.find()) {
            throw new PluginImplementationException("File name not found");
        }
        String name = matcher.group(1).replace(": ", " - ");
        if (matcher.group(2) != null) {
            name += " - " + matcher.group(2);
        }
        httpFile.setFileName(name + ".flv");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize();
            final String id = PlugUtils.getStringBetween(getContentAsString(), "id: '", "'");
            method = getGetMethod("http://papi.yle.fi/ng/mod/rtmp/" + id);
            if (makeRedirectedRequest(method)) {
                final String content = decryptContent();
                final String url = PlugUtils.getStringBetween(content, "<connect>", "</connect>");
                final String play = PlugUtils.getStringBetween(content, "<stream>", "</stream>");
                final RtmpSession rtmpSession = new RtmpSession(url, play);
                rtmpSession.getConnectParams().put("pageUrl", fileURL);
                rtmpSession.getConnectParams().put("swfUrl", SWF_URL);
                tryDownloadAndSaveFile(rtmpSession);
            } else {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        if (getContentAsString().contains("Valitettavasti etsimääsi sivua ei löytynyt")
                || getContentAsString().contains("Tyvärr kunde sidan du sökte inte hittas")
                || getContentAsString().contains("Ohjelman nettikatseluaika on päättynyt")
                || getContentAsString().contains("Programmets visningstid på nätet har utgått")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    private String decryptContent() throws Exception {
        final byte[] content = Base64.decodeBase64(getContentAsString());
        final Cipher cipher = Cipher.getInstance("AES/CFB128/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(KEY, "AES"), new IvParameterSpec(content, 0, 16));
        return new String(cipher.doFinal(content, 16, content.length - 16), "UTF-8");
    }

}