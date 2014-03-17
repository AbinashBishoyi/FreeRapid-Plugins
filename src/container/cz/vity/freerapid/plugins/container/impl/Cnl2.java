package cz.vity.freerapid.plugins.container.impl;

import cz.vity.freerapid.plugins.container.ContainerException;
import cz.vity.freerapid.plugins.container.ContainerFormat;
import cz.vity.freerapid.plugins.container.FileInfo;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import cz.vity.freerapid.utilities.LogUtils;
import cz.vity.freerapid.utilities.crypto.Cipher;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;

import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Special plugin which, instead of implementing the {@link ContainerFormat} interface,
 * provides a static {@link #read(String)} method for plugins to use directly.
 *
 * @author ntoskrnl
 */
public class Cnl2 {
    private final static Logger logger = Logger.getLogger(Cnl2.class.getName());

    /**
     * Do not instantiate.
     */
    private Cnl2() {
    }

    /**
     * Parses {@code content} for CNL2 items and decrypts them.
     *
     * @param content Content to parse
     * @return List of links parsed, or an empty list if nothing was found or an exception occurred
     */
    public static List<FileInfo> read(final String content) {
        final List<FileInfo> list = new LinkedList<FileInfo>();
        final Matcher matcher = PlugUtils.matcher("(?is)(<FORM NAME=\"cnl2_load\".+?</FORM>)", content);
        while (matcher.find()) {
            try {
                final String formContent = matcher.group(1);
                final String crypted = PlugUtils.getParameter("crypted", formContent);
                final byte[] key;
                Matcher matcher2 = PlugUtils.matcher("(?i)NAME=\"jk\" VALUE=\"(.+?)\"", formContent);
                if (matcher2.find()) {
                    matcher2 = PlugUtils.matcher("return\\s+(.+?);", matcher2.group(1));
                    if (!matcher2.find()) {
                        throw new ContainerException("Error parsing key function");
                    }
                    key = Hex.decodeHex(matcher2.group(1).replaceAll("\\P{XDigit}", "").toCharArray());
                } else if (PlugUtils.find("(?i)NAME=\"k\"", formContent)) {
                    key = Hex.decodeHex(PlugUtils.getParameter("k", formContent).toCharArray());
                } else {
                    throw new ContainerException("Key parameter not found");
                }
                final Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
                cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(key));
                final String decodedContent = new String(cipher.doFinal(Base64.decodeBase64(crypted)), "UTF-8");
                for (String url : decodedContent.split("\n")) {
                    url = url.trim();
                    if (!url.isEmpty()) {
                        try {
                            list.add(new FileInfo(new URL(url)));
                        } catch (MalformedURLException e) {
                            LogUtils.processException(logger, e);
                        }
                    }
                }
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Failed to decrypt CNL2", e);
            }
        }
        return list;
    }

}
