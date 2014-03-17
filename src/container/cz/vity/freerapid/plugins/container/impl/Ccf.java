package cz.vity.freerapid.plugins.container.impl;

import cz.vity.freerapid.plugins.container.*;
import jlibs.xml.sax.binding.BindingHandler;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.AttributesImpl;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.logging.Logger;

/**
 * @author ntoskrnl
 */
public class Ccf implements ContainerFormat {
    private final static Logger logger = Logger.getLogger(Ccf.class.getName());

    private final static byte[] KEY_A1 = ContainerUtils.hexToBytes("64E9E143CE4634DA5CD99B0CBFA3002A9A3765E10CB19CFF906DB6A68F95B398");
    private final static byte[] IV_A1 = ContainerUtils.hexToBytes("E0FEABE3F4B13E6F05F4A5A35B7FBDC8");
    private final static byte[] KEY_A2 = ContainerUtils.hexToBytes("FFC9122B34FAE1043087DCA5FAAAAB109414049A6AD2F9F161C7576BE464E48A");
    private final static byte[] IV_A2 = ContainerUtils.hexToBytes("B506B639984C9285ADFAC5B42BAE6F47");

    private final static byte[] KEY_B = ContainerUtils.hexToBytes("5F679C00548737E120E6518A981BD0BA11AF5C719E97502983AD6AA38ED721C3");
    private final static byte[] IV_B = ContainerUtils.hexToBytes("E3D153AD609EF7358D66684180C7331A");

    private final static byte[] KEY_C = ContainerUtils.hexToBytes("171BF8E34C3D0C0C2693FDD2B080423A5B98F4D028A0AF4D82A385D837A8F95F");
    private final static byte[] IV_C = ContainerUtils.hexToBytes("9FE95FFF7CA4FC0FCEF25E4F7444AE67");

    private final static byte[] KEY_D = ContainerUtils.hexToBytes("026900E977C6402442B661329CFE62D6ED21BDEB0CD6321318A8EDC7BC5A6C86");
    private final static byte[] IV_D = ContainerUtils.hexToBytes("8CE1173EBAD76E08584B94573926231E");

    public static String[] getSupportedFiles() {
        return new String[]{"ccf"};
    }

    public Ccf(final ContainerPlugin plugin) {
    }

    @Override
    public List<FileInfo> read(final InputStream is) throws Exception {
        checkCcf3(is);
        for (char c = 'A'; ; c++) {
            is.mark(1024);
            final List<FileInfo> result;
            switch (c) {
                case 'A':
                    result = tryToReadFromStream(createStreamA(is), c);
                    break;
                case 'B':
                    result = tryToReadFromStream(createStream(is, KEY_B, IV_B), c);
                    break;
                case 'C':
                    result = tryToReadFromStream(createStream(is, KEY_C, IV_C), c);
                    break;
                case 'D':
                    result = tryToReadFromStream(createStream(is, KEY_D, IV_D), c);
                    break;
                default:
                    throw ContainerException.fileIsCorrupt();
            }
            if (result != null) {
                return result;
            }
            is.reset();
        }
    }

    private void checkCcf3(final InputStream is) throws Exception {
        is.mark(6);
        final byte[] b = new byte[6];
        if (is.read(b) == 6
                && b[0] == 'C'
                && b[1] == 'C'
                && b[2] == 'F'
                && b[3] == '3'
                && b[4] == '.'
                && b[5] == '0') {
            // CCF3 is an old, largely obsolete format.
            // Support will be added if anyone finds active use of CCF3 containers.
            throw new ContainerException("CCF3 containers are not supported");
        }
        is.reset();
    }

    private InputStream createStreamA(final InputStream is) throws Exception {
        final Cipher cipher1 = Cipher.getInstance("AES/CBC/NoPadding");
        cipher1.init(Cipher.DECRYPT_MODE, new SecretKeySpec(KEY_A1, "AES"), new IvParameterSpec(IV_A1));
        final Cipher cipher2 = Cipher.getInstance("AES/CBC/NoPadding");
        cipher2.init(Cipher.DECRYPT_MODE, new SecretKeySpec(KEY_A2, "AES"), new IvParameterSpec(IV_A2));
        final Cipher cipher3 = Cipher.getInstance("AES/CBC/NoPadding");
        cipher3.init(Cipher.DECRYPT_MODE, new SecretKeySpec(KEY_A1, "AES"), new IvParameterSpec(IV_A1));
        return new ZeroPaddingInputStream(new CipherInputStream(new CipherInputStream(new CipherInputStream(is, cipher1), cipher2), cipher3));
    }

    private InputStream createStream(final InputStream is, final byte[] key, final byte[] iv) throws Exception {
        final Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));
        return new ZeroPaddingInputStream(new CipherInputStream(is, cipher));
    }

    private List<FileInfo> tryToReadFromStream(final InputStream is, final char c) {
        try {
            @SuppressWarnings("unchecked")
            final List<FileInfo> list = (List<FileInfo>) new BindingHandler(CcfRootBinding.class).parse(new InputSource(is));
            logger.info("Method " + c + " success");
            return list;
        } catch (final Exception e) {
            logger.info("Method " + c + " failed: " + e);
            return null;
        }
    }

    @Override
    public void write(final List<FileInfo> files, OutputStream os) throws Exception {
        final Cipher cipher1 = Cipher.getInstance("AES/CBC/NoPadding");
        cipher1.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(KEY_A1, "AES"), new IvParameterSpec(IV_A1));
        final Cipher cipher2 = Cipher.getInstance("AES/CBC/NoPadding");
        cipher2.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(KEY_A2, "AES"), new IvParameterSpec(IV_A2));
        final Cipher cipher3 = Cipher.getInstance("AES/CBC/NoPadding");
        cipher3.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(KEY_A1, "AES"), new IvParameterSpec(IV_A1));
        os = new ZeroPaddingOutputStream(new CipherOutputStream(new CipherOutputStream(new CipherOutputStream(os, cipher3), cipher2), cipher1));

        final TransformerHandler handler = ((SAXTransformerFactory) SAXTransformerFactory.newInstance()).newTransformerHandler();
        final Transformer serializer = handler.getTransformer();
        serializer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        serializer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        serializer.setOutputProperty(OutputKeys.INDENT, "yes");
        //see com.sun.org.apache.xml.internal.serializer.OutputKeysFactory
        serializer.setOutputProperty("{http://xml.apache.org/xalan}indent-amount", "2");
        serializer.setOutputProperty("{http://xml.apache.org/xalan}line-separator", "\r\n");
        handler.setResult(new StreamResult(os));

        handler.startDocument();
        AttributesImpl attributes = new AttributesImpl();
        handler.startElement("", "", "CryptLoad", attributes);
        final String time = String.valueOf(System.currentTimeMillis());
        attributes.addAttribute("", "", "service", "", "");
        attributes.addAttribute("", "", "name", "", time);
        attributes.addAttribute("", "", "url", "", time);
        handler.startElement("", "", "Package", attributes);
        attributes.clear();
        handler.startElement("", "", "Options", attributes);
        handler.startElement("", "", "Kommentar", attributes);
        handler.endElement("", "", "Kommentar");
        handler.startElement("", "", "Passwort", attributes);
        handler.endElement("", "", "Passwort");
        handler.endElement("", "", "Options");
        for (final FileInfo file : files) {
            final String url = file.getFileUrl().toString();
            attributes.addAttribute("", "", "Url", "", url);
            handler.startElement("", "", "Download", attributes);
            attributes.clear();
            final long size = file.getFileSize();
            if (size >= 0) {
                final String sizeStr = String.valueOf(size);
                handler.startElement("", "", "FileSize", attributes);
                handler.characters(sizeStr.toCharArray(), 0, sizeStr.length());
                handler.endElement("", "", "FileSize");
            }
            handler.startElement("", "", "Url", attributes);
            handler.characters(url.toCharArray(), 0, url.length());
            handler.endElement("", "", "Url");
            final String name = file.getFileName();
            if (name != null) {
                handler.startElement("", "", "FileName", attributes);
                handler.characters(name.toCharArray(), 0, name.length());
                handler.endElement("", "", "FileName");
            }
            handler.endElement("", "", "Download");
        }
        handler.endElement("", "", "Package");
        handler.endElement("", "", "CryptLoad");
        handler.endDocument();
        os.close();
    }

}
