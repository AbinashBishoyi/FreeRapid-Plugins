package cz.vity.freerapid.plugins.services.megavideo;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 */
class MegaVideoFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(MegaVideoFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
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
                    .setAction("http://www" + s + ".megavideo.com/files/" + decrypt(un, k1, k2) + "/")
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
        final Matcher matcher = getMatcherAgainstContent("size=\"(.+?)\".+?title=\"(.+?)[\\.\"]");
        if (!matcher.find()) throw new PluginImplementationException("File name/size not found");
        this.httpFile.setFileName(matcher.group(2).replace("+", " ") + ".flv");
        this.httpFile.setFileSize(Integer.parseInt(matcher.group(1)));
        this.httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("Invalid or unknown video link")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    private String getXmlUrl(final String url) {
        return "http://www.megavideo.com/xml/videolink.php?" + url.substring(url.lastIndexOf("v="));
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
    private String decrypt(String str_hex, String str_key1, String str_key2) {
        // http://wordrider.net/forum/read.php?10,3209,3212#msg-3212

        // 1. Convert hexadecimal string to binary string
        char[] chr_hex = str_hex.toCharArray();
        String str_bin = "";
        for (int i = 0; i < 32; i++) {
            String temp1 = Integer.toBinaryString(Integer.parseInt(Character.toString(chr_hex[i]), 16));
            while (temp1.length() < 4) {
                temp1 = "0" + temp1;
            }
            str_bin += temp1;
        }
        char[] chr_bin = str_bin.toCharArray();

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
            char temp3 = chr_bin[key[i]];
            chr_bin[key[i]] = chr_bin[i % 128];
            chr_bin[i % 128] = temp3;
        }

        // 4. XOR entire binary string
        for (int i = 0; i < 128; i++) {
            chr_bin[i] = (char) (chr_bin[i] ^ key[i + 256] & 1);
        }

        // 5. Convert binary string back to hexadecimal
        str_bin = new String(chr_bin);
        str_hex = "";
        for (int i = 0; i < 128; i += 4) {
            str_hex += Integer.toHexString(Integer.parseInt(str_bin.substring(i, i + 4), 2));
        }

        // 6. Return counted string
        return str_hex;
    }

}