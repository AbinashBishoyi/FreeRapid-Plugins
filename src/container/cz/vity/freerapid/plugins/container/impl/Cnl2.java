package cz.vity.freerapid.plugins.container.impl;

import cz.vity.freerapid.plugins.container.ContainerException;
import cz.vity.freerapid.plugins.container.ContainerFormat;
import cz.vity.freerapid.plugins.container.FileInfo;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import cz.vity.freerapid.plugins.webclient.utils.ScriptUtils;
import cz.vity.freerapid.utilities.LogUtils;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;

import javax.crypto.Cipher;
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
 * provides static methods for plugins to use directly.
 *
 * @author ntoskrnl
 */
public final class Cnl2 {
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
        final Matcher matcher = PlugUtils.matcher("(?is)(<FORM[^<>]+?ACTION=\"http://127\\.0\\.0\\.1:9666/.+?</FORM>)", content);
        while (matcher.find()) {
            try {
                final String formContent = matcher.group(1);
                final String crypted = PlugUtils.getParameter("crypted", formContent);
                final String key;
                if (PlugUtils.find("(?i)NAME=\"jk\"", formContent)) {
                    final String jk = PlugUtils.getParameter("jk", formContent).replace("\\\"", "\"").replace("\\'", "'");
                    key = executeKeyJs(jk);
                } else if (PlugUtils.find("(?i)NAME=\"k\"", formContent)) {
                    key = PlugUtils.getParameter("k", formContent);
                } else {
                    throw new ContainerException("Key parameter not found");
                }
                list.addAll(decrypt(crypted, key));
            } catch (final Exception e) {
                logger.log(Level.WARNING, "Failed to decrypt CNL2", e);
            }
        }
        return list;
    }

    /**
     * Executes a CNL2 JavaScript function to retrieve the decryption key.
     *
     * @param script Script to execute
     * @return Key for use with decryption, or an empty string if an exception occurred
     */
    public static String executeKeyJs(final String script) {
        try {
            logger.info("Executing key JS: " + script);
            return ScriptUtils.evaluateJavaScriptToString(script + "  f()");
        } catch (final Exception e) {
            logger.log(Level.WARNING, "Failed to decrypt CNL2", e);
            return "";
        }
    }

    /**
     * Decrypts CNL2 data.
     *
     * @param content Data to decrypt
     * @param key     Key to use for decryption
     * @return List of links decrypted, or an empty list if an exception occurred
     */
    public static List<FileInfo> decrypt(final String content, final String key) {
        final List<FileInfo> list = new LinkedList<FileInfo>();
        try {
            final byte[] keyBytes = Hex.decodeHex(key.toCharArray());
            final Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(keyBytes, "AES"), new IvParameterSpec(keyBytes));
            final String decodedContent = new String(cipher.doFinal(Base64.decodeBase64(content)), "UTF-8");
            for (String url : decodedContent.split("\n")) {
                url = url.trim();
                if (!url.isEmpty()) {
                    try {
                        list.add(new FileInfo(new URL(url)));
                    } catch (final MalformedURLException e) {
                        LogUtils.processException(logger, e);
                    }
                }
            }
        } catch (final Exception e) {
            logger.log(Level.WARNING, "Failed to decrypt CNL2", e);
        }
        return list;
    }

}
