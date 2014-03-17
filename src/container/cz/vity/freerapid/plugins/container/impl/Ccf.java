package cz.vity.freerapid.plugins.container.impl;

import cz.vity.freerapid.plugins.container.*;
import cz.vity.freerapid.utilities.crypto.Cipher;
import cz.vity.freerapid.utilities.crypto.CipherInputStream;
import cz.vity.freerapid.utilities.crypto.CipherOutputStream;
import jlibs.xml.sax.binding.BindingHandler;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.AttributesImpl;

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

    private final static byte[] KEY1 = ContainerUtils.hexToBytes("64E9E143CE4634DA5CD99B0CBFA3002A9A3765E10CB19CFF906DB6A68F95B398");
    private final static byte[] IV1 = ContainerUtils.hexToBytes("E0FEABE3F4B13E6F05F4A5A35B7FBDC8");
    private final static byte[] KEY2 = ContainerUtils.hexToBytes("FFC9122B34FAE1043087DCA5FAAAAB109414049A6AD2F9F161C7576BE464E48A");
    private final static byte[] IV2 = ContainerUtils.hexToBytes("B506B639984C9285ADFAC5B42BAE6F47");

    public static String[] getSupportedFiles() {
        return new String[]{"ccf"};
    }

    public Ccf(final ContainerPlugin plugin) {
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<FileInfo> read(InputStream is) throws Exception {
        final Cipher cipher1 = Cipher.getInstance("AES/CBC/NoPadding");
        cipher1.init(Cipher.DECRYPT_MODE, new SecretKeySpec(KEY1, "AES"), new IvParameterSpec(IV1));
        final Cipher cipher2 = Cipher.getInstance("AES/CBC/NoPadding");
        cipher2.init(Cipher.DECRYPT_MODE, new SecretKeySpec(KEY2, "AES"), new IvParameterSpec(IV2));
        final Cipher cipher3 = Cipher.getInstance("AES/CBC/NoPadding");
        cipher3.init(Cipher.DECRYPT_MODE, new SecretKeySpec(KEY1, "AES"), new IvParameterSpec(IV1));
        is = new ZeroPaddingInputStream(new CipherInputStream(new CipherInputStream(new CipherInputStream(is, cipher1), cipher2), cipher3));
        final Object o = new BindingHandler(CcfRootBinding.class).parse(new InputSource(is));
        if (o == null || !(o instanceof List)) {
            throw ContainerException.fileIsCorrupt();
        }
        return (List<FileInfo>) o;
    }

    @Override
    public void write(final List<FileInfo> files, OutputStream os) throws Exception {
        final Cipher cipher1 = Cipher.getInstance("AES/CBC/NoPadding");
        cipher1.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(KEY1, "AES"), new IvParameterSpec(IV1));
        final Cipher cipher2 = Cipher.getInstance("AES/CBC/NoPadding");
        cipher2.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(KEY2, "AES"), new IvParameterSpec(IV2));
        final Cipher cipher3 = Cipher.getInstance("AES/CBC/NoPadding");
        cipher3.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(KEY1, "AES"), new IvParameterSpec(IV1));
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
