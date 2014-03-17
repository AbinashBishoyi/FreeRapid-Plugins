package cz.vity.freerapid.plugins.services.megauri;

import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import org.apache.commons.codec.binary.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.net.URL;
import java.nio.charset.Charset;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 */
class MegaUriFileRunner extends AbstractRunner {
    private static final byte[] KEY = "k1o6Al-1kz?!z05yXXXXXXXXXXXXXXXX".getBytes(Charset.forName("ASCII"));
    private static final byte[] IV = {121, (byte) 241, 10, 1, (byte) 132, 74, 11, 39, (byte) 255, 91, 45, 78, 14, (byte) 211, 22, 62};

    @Override
    public void run() throws Exception {
        super.run();
        final String megaUrl = getMegaUrl(fileURL);
        httpFile.setNewURL(new URL(megaUrl));
        httpFile.setPluginID("");
        httpFile.setState(DownloadState.QUEUED);
    }

    private String getMegaUrl(String fileUrl) throws Exception {
        /*
         * Examples:
		 * mega:!ABC!12345678900
		 * mega:#!ABC!12345678900
		 * mega:#F!ABC!12345678900
		 * mega://#!123!456789ABC
		 * mega://#F!123!456789ABC
		 * mega://https://mega.co.nz/#!abcdef!ghijklmnopqr
		 * mega://mega-search?tn
		 * mega://enc?_xlPqemSILarh5VBKbhSTFyQQQ0
		 * mega://senc?_xlPqemSILarh5VBKbhSTFyQQQ0
         */
        fileUrl = fileUrl.replaceFirst("(http://)?mega:(//)?", "");
        String fileId;
        if (fileUrl.startsWith("https://mega.co.nz/")) {
            return fileUrl;
        } else if (!fileUrl.contains("?")) {
            fileId = fileUrl;
        } else if (fileUrl.startsWith("enc") || fileUrl.startsWith("fenc")) {
            fileId = decrypt(fileUrl.substring(fileUrl.indexOf('?') + 1));
            if (fileUrl.startsWith("fenc")) {
                fileId = "F" + fileId;
            }
        } else {
            throw new PluginImplementationException("Unable to parse URI");
        }
        if (!fileId.startsWith("#")) {
            fileId = "#" + fileId;
        }
        return "https://mega.co.nz/" + fileId;
    }

    private String decrypt(final String s) throws Exception {
        final Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(KEY, "AES"), new IvParameterSpec(IV));
        final byte[] data = cipher.doFinal(Base64.decodeBase64(s));
        return new String(data, Charset.forName("ASCII"));
    }

}