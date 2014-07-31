package cz.vity.freerapid.plugins.services.crunchyroll_premium;

import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import cz.vity.freerapid.plugins.webclient.utils.HttpUtils;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import cz.vity.freerapid.utilities.LogUtils;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.util.Arrays;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.zip.Inflater;

/**
 * @author tong2shot
 */
public class SubtitleDownloader {
    private final static Logger logger = Logger.getLogger(SubtitleDownloader.class.getName());
    private final static int HASH_MAGIC_INT = 88140282; // (int) Math.floor(Math.sqrt(6.9) * Math.pow(2,25))
    private final static String HASH_MAGIC_STRING = "$&).6CXzPHw=2N_+isZK";

    private byte[] generateKey(int subtitleId) {
        long subtitleMagic = HASH_MAGIC_INT ^ subtitleId;
        long hashMagic = subtitleMagic ^ subtitleMagic >> 3 ^ subtitleMagic * 32;
        return Arrays.copyOf(DigestUtils.sha(HASH_MAGIC_STRING + hashMagic), 32); //32-bytes padding
    }

    private String decrypt(int subtitleId, byte[] iv, byte[] data) throws Exception {
        byte[] key = generateKey(subtitleId);
        Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE,
                new SecretKeySpec(key, "AES"),
                new IvParameterSpec(iv));
        byte[] decrypted = cipher.doFinal(data);

        Inflater inflater = new Inflater(false);
        inflater.setInput(decrypted);
        ByteArrayOutputStream baos = null;
        String ret;
        try {
            baos = new ByteArrayOutputStream(decrypted.length);
            byte[] buffer = new byte[1024];
            while (!inflater.finished()) {
                int count = inflater.inflate(buffer);
                baos.write(buffer, 0, count);
            }
            ret = new String(baos.toByteArray());
        } finally {
            inflater.end();
            if (baos != null) {
                try {
                    baos.close();
                } catch (IOException e) {
                    LogUtils.processException(logger, e);
                }
            }
        }
        return ret;
    }

    private static String toTimeFormat(String value) {
        String[] split1 = value.replace(".", ",").split(",");
        String hms = split1[0];
        int millis = (split1.length == 1 ? 0 : Integer.parseInt(split1[1]));
        int h, m, s;
        String[] split2 = hms.split(":");
        h = Integer.parseInt(split2[0]);
        m = Integer.parseInt(split2[1]);
        s = Integer.parseInt(split2[2]);
        return String.format("%02d:%02d:%02d,%03d", h, m, s, millis);
    }

    private String convertToSrt(String subtitleScript) {
        StringBuilder subtitleSb = new StringBuilder();
        try {
            Element events = (Element) DocumentBuilderFactory.newInstance().newDocumentBuilder()
                    .parse(new ByteArrayInputStream(subtitleScript.getBytes("UTF-8")))
                    .getElementsByTagName("events").item(0);
            NodeList eventElements = events.getElementsByTagName("event");
            for (int i = 0, eventsLength = eventElements.getLength(); i < eventsLength; i++) {
                Element eventElement = (Element) eventElements.item(i);
                subtitleSb.append(i + 1).append("\n");
                subtitleSb.append(toTimeFormat(eventElement.getAttribute("start")));
                subtitleSb.append(" --> ");
                subtitleSb.append(toTimeFormat(eventElement.getAttribute("end")));
                subtitleSb.append("\n");
                subtitleSb.append(PlugUtils.unescapeUnicode(eventElement.getAttribute("text")
                        .replaceAll("\\{\\\\([b|i|s|u])1\\}", "<$1>")
                        .replaceAll("\\{\\\\([b|i|s|u])0\\}", "</$1>")
                        .replaceAll("\\{\\\\[^\\{\\}]+\\}", "") //disable the rest of effects
                        .replace("\\N", "\n")
                        .replace("\\n", "\n")
                        .replace("\\h", " ")));
                subtitleSb.append("\n\n");
            }
        } catch (Exception e) {
            LogUtils.processException(logger, e);
        }
        return subtitleSb.toString();
    }

    public void downloadSubtitle(HttpFile httpFile, String content) throws Exception {
        logger.info("Downloading subtitle");
        Matcher matcher = PlugUtils.matcher("subtitle id='(\\d+)'", content);
        if (!matcher.find()) {
            logger.info("Subtitle not found");
            return;
        }
        int subtitleId = Integer.parseInt(matcher.group(1));

        matcher = PlugUtils.matcher("<iv>([^<>]+)</iv>", content);
        String iv;
        if (!matcher.find() || ((iv = matcher.group(1)).isEmpty())) {
            logger.info("IV not found");
            return;
        }
        matcher = PlugUtils.matcher("<data>([^<>]+)</data>", content);
        String data;
        if (!matcher.find() || ((data = matcher.group(1)).isEmpty())) {
            logger.info("Data not found");
            return;
        }

        String fnameNoExt = HttpUtils.replaceInvalidCharsForFileSystem(httpFile.getFileName().replaceFirst("\\..{3,4}$", ""), "_");
        String fnameOutput = fnameNoExt + ".srt";
        File outputFile = new File(httpFile.getSaveToDirectory(), fnameOutput);
        BufferedWriter bw = null;
        int outputFileCounter = 2;
        try {
            while (outputFile.exists()) {
                fnameOutput = fnameNoExt + "-" + outputFileCounter++ + ".srt";
                outputFile = new File(httpFile.getSaveToDirectory(), fnameOutput);
            }
            bw = new BufferedWriter(new FileWriter((outputFile)));
            bw.write(convertToSrt(decrypt(subtitleId, Base64.decodeBase64(iv), Base64.decodeBase64(data))));
        } finally {
            if (bw != null) {
                try {
                    bw.close();
                } catch (IOException e) {
                    LogUtils.processException(logger, e);
                }
            }
        }
    }

}
