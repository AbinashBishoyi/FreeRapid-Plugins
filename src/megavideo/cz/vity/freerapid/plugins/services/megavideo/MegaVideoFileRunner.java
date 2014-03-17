package cz.vity.freerapid.plugins.services.megavideo;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.utilities.LogUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Formatter;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 */
class MegaVideoFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(MegaVideoFileRunner.class.getName());
    private String HTTP_SITE = "megavideo.com";

    @Override
    public void runCheck() throws Exception {
        super.runCheck();

        if (fileURL.contains("megaporn")) {
            HTTP_SITE = "megaporn.com/video";
        }
        if (fileURL.contains("d=")) {
            fileURL = fileURL.replace("megavideo.com", "megaupload.com").replace("/video/", "/");
            httpFile.setNewURL(new URL(fileURL));
            httpFile.setPluginID("");
            httpFile.setState(DownloadState.QUEUED);
            return;
        }

        final GetMethod method = getGetMethod(getXmlUrl(fileURL));
        if (makeRedirectedRequest(method)) {
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
        logger.info("Starting download in TASK " + fileURL);

        if (fileURL.contains("megaporn")) {
            HTTP_SITE = "megaporn.com/video";
        }
        if (fileURL.contains("d=")) {
            fileURL = fileURL.replace("megavideo.com", "megaupload.com").replace("/video/", "/");
            httpFile.setNewURL(new URL(fileURL));
            httpFile.setPluginID("");
            httpFile.setState(DownloadState.QUEUED);
            return;
        }

        final GetMethod method = getGetMethod(getXmlUrl(fileURL));
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize();

            final Matcher matcher = getMatcherAgainstContent("k1=\"(.+?)\".+?k2=\"(.+?)\".+?un=\"(.+?)\".+?s=\"(.+?)\"");
            if (!matcher.find()) throw new PluginImplementationException("Encryption codes not found");
            final String k1 = matcher.group(1);
            final String k2 = matcher.group(2);
            final String un = matcher.group(3);
            final String s = matcher.group(4);

            final HttpMethod httpMethod = getMethodBuilder()
                    .setReferer(fileURL)
                    .setAction("http://www" + s + "." + HTTP_SITE + "/files/" + decrypt(un, k1, k2) + "/")
                    .toGetMethod();

            if (!tryDownloadAndSaveFile(httpMethod)) {
                checkProblems();
                throw new PluginImplementationException();
            }

        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        final Matcher matcher = getMatcherAgainstContent("size=\"(.+?)\".+?title=\"(.+?)\"");
        if (!matcher.find()) throw new PluginImplementationException("File name/size not found");
        httpFile.setFileName(urlDecode(matcher.group(2)) + ".flv");
        httpFile.setFileSize(Long.parseLong(matcher.group(1)));
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final Matcher matcher = getMatcherAgainstContent("errortext=\"(.+?)\"");
        if (matcher.find()) {
            throw new URLNotAvailableAnymoreException(matcher.group(1));
        }
    }

    private String getXmlUrl(final String url) {
        return "http://www." + HTTP_SITE + "/xml/videolink.php?" + url.substring(url.lastIndexOf("v="));
    }

    private static String urlDecode(final String url) {
        try {
            return url == null ? "" : URLDecoder.decode(url, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            LogUtils.processException(logger, e);
            return "";
        }
    }

    /**
     * Method for decrypting MegaVideo stream download links.
     * See&nbsp;http://wordrider.net/forum/read.php?10,3209,3212#msg-3212
     *
     * @param str_hex  String to decrypt
     * @param str_key1 Decrypt key 1
     * @param str_key2 Decrypt key 2
     * @return Decrypted string
     */
    private static String decrypt(String str_hex, String str_key1, String str_key2) {
        char[] chr_hex;
        char[] chr_bin;

        // 1. Convert hexadecimal string to binary string
        chr_hex = str_hex.toCharArray();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 32; i++) {
            String temp = Integer.toBinaryString(Integer.parseInt(Character.toString(chr_hex[i]), 16));
            for (int n = temp.length() - 4; n < 0; n++) {
                sb.append('0');//pad the value to 4 characters
            }
            sb.append(temp);
        }
        chr_bin = sb.toString().toCharArray();

        // 2. Generate switch and XOR keys
        int key1 = Integer.parseInt(str_key1);
        int key2 = Integer.parseInt(str_key2);
        int[] key = new int[384];
        for (int i = 0; i < 384; i++) {
            key1 = (key1 * 11 + 77213) % 81371;
            key2 = (key2 * 17 + 92717) % 192811;
            key[i] = (key1 + key2) % 128;
        }

        // 3. Switch bits positions
        for (int i = 256; i >= 0; i--) {
            char temp = chr_bin[key[i]];
            chr_bin[key[i]] = chr_bin[i % 128];
            chr_bin[i % 128] = temp;
        }

        // 4. XOR entire binary string
        for (int i = 0; i < 128; i++) {
            chr_bin[i] = (char) (chr_bin[i] ^ key[i + 256] & 1);
        }

        // 5. Convert binary string back to hexadecimal
        BigInteger bi = new BigInteger(new String(chr_bin), 2);
        return new Formatter().format("%1$032x", bi).toString();
    }

}