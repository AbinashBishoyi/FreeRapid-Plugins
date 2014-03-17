package cz.vity.freerapid.plugins.container.impl;

import cz.vity.freerapid.plugins.container.ContainerFormat;
import cz.vity.freerapid.plugins.container.ContainerPlugin;
import cz.vity.freerapid.plugins.container.ContainerUtils;
import cz.vity.freerapid.plugins.container.FileInfo;
import cz.vity.freerapid.utilities.LogUtils;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StreamTokenizer;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

/**
 * @author ntoskrnl
 */
public class Rsdf implements ContainerFormat {
    private final static Logger logger = Logger.getLogger(Rsdf.class.getName());

    private final static byte[] KEY = ContainerUtils.hexToBytes("8C35192D964DC3182C6F84F3252239EB4A320D2500000000");
    private final static byte[] IV = ContainerUtils.hexToBytes("A3D5A33CB95AC1F5CBDB1AD25CB0A7AA");

    public static String[] getSupportedFiles() {
        return new String[]{"rsdf", "rsd"};
    }

    public Rsdf(final ContainerPlugin plugin) {
    }

    private void setTokenizerSyntax(final StreamTokenizer tokenizer) {
        tokenizer.whitespaceChars(0, 0xFF);
        tokenizer.ordinaryChars('0', '9');
        tokenizer.wordChars('0', '9');
        tokenizer.ordinaryChars('A', 'Z');
        tokenizer.wordChars('A', 'Z');
        tokenizer.ordinaryChars('a', 'z');
        tokenizer.wordChars('a', 'z');
        tokenizer.ordinaryChars('+', '+');
        tokenizer.wordChars('+', '+');
        tokenizer.ordinaryChars('/', '/');
        tokenizer.wordChars('/', '/');
        tokenizer.ordinaryChars('=', '=');
        tokenizer.wordChars('=', '=');
    }

    @Override
    public List<FileInfo> read(final InputStream is) throws Exception {
        final List<FileInfo> list = new LinkedList<FileInfo>();
        final StreamTokenizer tokenizer = new StreamTokenizer(new InputStreamReader(new HexInputStream(is),
                "ISO-8859-1"));//read Base64 characters in extended ASCII
        setTokenizerSyntax(tokenizer);
        final Cipher cipher = Cipher.getInstance("AES/CFB8/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(KEY, "AES"), new IvParameterSpec(IV));
        while (tokenizer.nextToken() != StreamTokenizer.TT_EOF) {
            String s = new String(cipher.update(Base64.decodeBase64(tokenizer.sval)), "UTF-8");
            if (s.startsWith("CCF: ")) {
                s = s.substring(5);
            }
            try {
                final FileInfo file = new FileInfo(new URL(s));
                list.add(file);
            } catch (final MalformedURLException e) {
                LogUtils.processException(logger, e);
            }
        }
        return list;
    }

    @Override
    public void write(final List<FileInfo> files, final OutputStream os) throws Exception {
        final Cipher cipher = Cipher.getInstance("AES/CFB8/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(KEY, "AES"), new IvParameterSpec(IV));
        final byte[] rn = Hex.encodeHexString(new byte[]{'\r', '\n'}).getBytes("UTF-8");
        for (final FileInfo file : files) {
            os.write(Hex.encodeHexString(Base64.encodeBase64(cipher.update(
                    file.getFileUrl().toString().getBytes("UTF-8")
            ))).getBytes("UTF-8"));
            os.write(rn);
        }
    }

}
