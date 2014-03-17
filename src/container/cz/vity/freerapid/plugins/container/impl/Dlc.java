package cz.vity.freerapid.plugins.container.impl;

import cz.vity.freerapid.plugins.container.*;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpDownloadClient;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import jlibs.xml.sax.binding.BindingHandler;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Base64OutputStream;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.AttributesImpl;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.util.List;
import java.util.logging.Logger;

/**
 * @author ntoskrnl
 */
public class Dlc implements ContainerFormat {
    private final static Logger logger = Logger.getLogger(Dlc.class.getName());

    private final static String SERVICE_URL = "http://service.jdownloader.org/dlcrypt/service.php";
    private final static byte[] KEY = ContainerUtils.hexToBytes("447e7873c6a96b3964be0c51e60e29bd");

    public static String[] getSupportedFiles() {
        return new String[]{"dlc"};
    }

    private final ContainerPlugin plugin;

    public Dlc(final ContainerPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<FileInfo> read(final InputStream is) throws Exception {
        final String content = ContainerUtils.readToString(is);
        logger.info("content = " + content);
        if (content.length() < 88) {
            throw ContainerException.fileIsCorrupt();
        }
        final String encryptedContent = content.substring(0, content.length() - 88);
        final String keyFromServer = getKeyFromServer("jd=11&destType=cltc3&srcType=dlc&data=" + content.substring(content.length() - 88));
        if (keyFromServer.equals("2YVhzRFdjR2dDQy9JL25aVXFjQ1RPZ")) {
            throw new ContainerException("You have reached the limit for opening DLC containers. Please wait a few minutes and try again.");
        }
        logger.info("keyFromServer = " + keyFromServer);
        final String key = new String(decryptKey(Base64.decodeBase64(keyFromServer)), "UTF-8");
        logger.info("key = " + key);
        final String decryptedContent = decryptContent(encryptedContent, Base64.decodeBase64(key));
        logger.info("decryptedContent =\n" + decryptedContent);
        final Object o = new BindingHandler(DlcRootBinding.class).parse(new InputSource(new StringReader(decryptedContent)));
        if (o == null || !(o instanceof List)) {
            throw ContainerException.fileIsCorrupt();
        }
        return (List<FileInfo>) o;
    }

    private String getKeyFromServer(final String content) throws Exception {
        final HttpDownloadClient client = plugin.createDownloadClient();
        final PostMethod method = client.getPostMethod(SERVICE_URL);
        method.setRequestHeader("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1; .NET CLR 1.1.4322; .NET CLR 2.0.50727)");
        method.setRequestHeader("Accept-Language", "de, en-gb;q=0.9, en;q=0.8");
        method.setRequestHeader("Accept", "text/html, image/gif, image/jpeg, *; q=.2, */*; q=.2");
        method.setRequestEntity(new StringRequestEntity(content, "application/x-www-form-urlencoded", "UTF-8"));
        if (client.makeRequest(method, false) != HttpStatus.SC_OK) {
            throw new ServiceConnectionProblemException();
        }
        return PlugUtils.getStringBetween(client.getContentAsString(), "<rc>", "</rc>");
    }

    private byte[] decryptKey(final byte[] input) throws Exception {
        final Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(KEY, "AES"));
        return cipher.doFinal(input);
    }

    private String decryptContent(final String input, final byte[] key) throws Exception {
        final Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(key));
        return new String(Base64.decodeBase64(cipher.doFinal(Base64.decodeBase64(input))), "UTF-8");
    }

    @Override
    public void write(final List<FileInfo> files, final OutputStream os0) throws Exception {
        final String key = DigestUtils.md5Hex(files.hashCode() + "salt" + System.currentTimeMillis() + "pepper" + Math.random()).substring(0, 16);
        logger.info("key = " + key);
        final String keyFromServer = getKeyFromServer("jd=1&srcType=plain&data=" + key);
        logger.info("keyFromServer = " + keyFromServer);

        final byte[] keyBytes = key.getBytes("UTF-8");
        final Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(keyBytes, "AES"), new IvParameterSpec(keyBytes));
        final OutputStream os = new Base64OutputStream(new ZeroPaddingOutputStream(new CipherOutputStream(new Base64OutputStream(new DontCloseOutputStream(os0), true, 0, null), cipher)), true, 0, null);

        final TransformerHandler handler = ((SAXTransformerFactory) SAXTransformerFactory.newInstance()).newTransformerHandler();
        final Transformer serializer = handler.getTransformer();
        serializer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        serializer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        serializer.setOutputProperty(OutputKeys.INDENT, "yes");
        //see com.sun.org.apache.xml.internal.serializer.OutputKeysFactory
        serializer.setOutputProperty("{http://xml.apache.org/xalan}indent-amount", "2");
        serializer.setOutputProperty("{http://xml.apache.org/xalan}line-separator", "\n");
        handler.setResult(new StreamResult(os));

        handler.startDocument();
        AttributesImpl attributes = new AttributesImpl();
        handler.startElement("", "", "dlc", attributes);
        handler.startElement("", "", "header", attributes);
        handler.startElement("", "", "generator", attributes);
        handler.startElement("", "", "app", attributes);
        addString(handler, "FreeRapid Downloader");
        handler.endElement("", "", "app");
        handler.startElement("", "", "version", attributes);
        addString(handler, "0.85");
        handler.endElement("", "", "version");
        handler.startElement("", "", "url", attributes);
        addString(handler, "http://wordrider.net/freerapid/");
        handler.endElement("", "", "url");
        handler.endElement("", "", "generator");
        handler.startElement("", "", "tribute", attributes);
        handler.startElement("", "", "name", attributes);
        handler.endElement("", "", "name");
        handler.endElement("", "", "tribute");
        handler.startElement("", "", "dlcxmlversion", attributes);
        addString(handler, "20_02_2008");
        handler.endElement("", "", "dlcxmlversion");
        handler.endElement("", "", "header");
        handler.startElement("", "", "content", attributes);
        attributes.addAttribute("", "", "name", "", "");
        attributes.addAttribute("", "", "passwords", "", "");
        attributes.addAttribute("", "", "comment", "", "");
        attributes.addAttribute("", "", "category", "", "");
        handler.startElement("", "", "package", attributes);
        attributes.clear();
        for (final FileInfo file : files) {
            handler.startElement("", "", "file", attributes);
            handler.startElement("", "", "url", attributes);
            addString(handler, file.getFileUrl().toString());
            handler.endElement("", "", "url");
            final String name = file.getFileName();
            if (name != null) {
                handler.startElement("", "", "filename", attributes);
                addString(handler, name);
                handler.endElement("", "", "filename");
            }
            final long size = file.getFileSize();
            if (size >= 0) {
                handler.startElement("", "", "size", attributes);
                addString(handler, String.valueOf(size));
                handler.endElement("", "", "size");
            }
            handler.endElement("", "", "file");
        }
        handler.endElement("", "", "package");
        handler.endElement("", "", "content");
        handler.endElement("", "", "dlc");
        handler.endDocument();

        os.close();
        os0.write(keyFromServer.getBytes("UTF-8"));
        os0.close();
    }

    private void addString(final TransformerHandler handler, final String s) throws Exception {
        final String encoded = new String(Base64.encodeBase64(s.getBytes("UTF-8")), "UTF-8");
        handler.characters(encoded.toCharArray(), 0, encoded.length());
    }

    private static class DontCloseOutputStream extends FilterOutputStream {
        public DontCloseOutputStream(final OutputStream os) {
            super(os);
        }

        @Override
        public void close() throws IOException {
        }
    }

}
