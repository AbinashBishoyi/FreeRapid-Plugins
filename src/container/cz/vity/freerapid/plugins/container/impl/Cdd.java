package cz.vity.freerapid.plugins.container.impl;

import cz.vity.freerapid.plugins.container.ContainerException;
import cz.vity.freerapid.plugins.container.ContainerFormat;
import cz.vity.freerapid.plugins.container.ContainerPlugin;
import cz.vity.freerapid.plugins.container.FileInfo;
import jlibs.xml.sax.binding.BindingHandler;
import org.apache.commons.codec.binary.Hex;
import org.xml.sax.InputSource;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author ntoskrnl
 */
public class Cdd implements ContainerFormat {
    private final static Logger logger = Logger.getLogger(Cdd.class.getName());

    public static String[] getSupportedFiles() {
        return new String[]{"cdd"};
    }

    public Cdd(final ContainerPlugin plugin) {
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<FileInfo> read(InputStream is) throws Exception {
        final String password = String.format("Crypt%sLoad",
                Math.abs(dotNetHashCode(InetAddress.getLocalHost().getHostName()) + Runtime.getRuntime().availableProcessors()));
        logger.info(password);
        final byte[] key = SecretKeyFactory
                .getInstance("PBKDF2WithHmacSHA1")
                .generateSecret(new PBEKeySpec(password.toCharArray(), password.getBytes("UTF-8"), 1000, 256))
                .getEncoded();
        if (logger.isLoggable(Level.INFO)) {
            logger.info(Hex.encodeHexString(key));
        }
        final Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(key, 0, 16));
        is = new CipherInputStream(is, cipher);
        // The XML declaration is somehow corrupted. The only option is to munch it before passing the stream on to the XML reader.
        if (is.read(new byte[43]) != 43) {
            throw ContainerException.fileIsCorrupt();
        }
        final Object o = new BindingHandler(CcfRootBinding.class).parse(new InputSource(is));
        if (o == null || !(o instanceof List)) {
            throw ContainerException.fileIsCorrupt();
        }
        return (List<FileInfo>) o;
    }

    private static int dotNetHashCode(final String s) {
        final char[] chars = s.toCharArray();
        int num = 352654597;
        int num2 = num;
        for (int i = chars.length, j = 0; i > 0; i -= 4, j += 4) {
            num = (((num << 5) + num) + (num >> 27)) ^ (chars[j] | (chars[j + 1] << 16));
            if (i <= 2) {
                break;
            }
            num2 = (((num2 << 5) + num2) + (num2 >> 27)) ^ (chars[j + 2] | (chars[j + 3] << 16));
        }
        return (num + (num2 * 1566083941));
    }

    @Override
    public void write(final List<FileInfo> files, final OutputStream os) throws Exception {
        throw new ContainerException("Exporting to CDD files is not supported");
        // Numerous reasons for that.
        // First of all, it's not a very good format for storage, as the encryption key
        // depends on the computer's name and the number of processors.
        // Second, the XML contains lots of specific information, which we don't even try to replicate.
    }

}
