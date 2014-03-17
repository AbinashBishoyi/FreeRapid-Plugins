package cz.vity.freerapid.plugins.container.impl;

import cz.vity.freerapid.plugins.container.ContainerFormat;
import cz.vity.freerapid.plugins.container.ContainerPlugin;
import cz.vity.freerapid.plugins.container.ContainerUtils;
import cz.vity.freerapid.plugins.container.FileInfo;
import cz.vity.freerapid.utilities.LogUtils;
import cz.vity.freerapid.utilities.crypto.Cipher;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;

import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
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

    @Override
    public List<FileInfo> read(final InputStream is) throws Exception {
        final List<FileInfo> list = new ArrayList<FileInfo>();
        final BufferedReader reader = new BufferedReader(new InputStreamReader(new HexInputStream(is)));
        final Cipher cipher = Cipher.getInstance("AES/CFB8/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(KEY, "AES"), new IvParameterSpec(IV));
        String s;
        while ((s = reader.readLine()) != null) {
            s = new String(cipher.update(Base64.decodeBase64(s)), "UTF-8");
            if (s.startsWith("CCF: ")) {
                s = s.substring(5);
            }
            try {
                final FileInfo file = new FileInfo(new URL(s));
                list.add(file);
            } catch (MalformedURLException e) {
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
                    "CCF: ".concat(file.getFileUrl().toString()).getBytes("UTF-8")
            ))).getBytes("UTF-8"));
            os.write(rn);
        }
    }

    private static class HexInputStream extends FilterInputStream {
        public HexInputStream(final InputStream is) {
            super(is);
        }

        @Override
        public int read() throws IOException {
            final byte[] b = new byte[2];
            final int len = read(b, 0, 2);
            if (len <= 0) {
                return -1;
            }
            return b[0];
        }

        @Override
        public int read(final byte[] b) throws IOException {
            return read(b, 0, b.length);
        }

        @Override
        public int read(final byte[] b, final int off, int len) throws IOException {
            len = in.read(b, off, len);
            if (len <= 0) {
                return len;
            }
            final char[] c = new char[len];
            for (int i = 0, j = off; i < len; i++, j++) {
                c[i] = (char) b[j];
            }
            final byte[] hex;
            try {
                hex = Hex.decodeHex(c);
            } catch (Exception e) {
                throw new IOException("Invalid hex stream", e);
            }
            System.arraycopy(hex, 0, b, off, hex.length);
            return hex.length;
        }
    }

}
