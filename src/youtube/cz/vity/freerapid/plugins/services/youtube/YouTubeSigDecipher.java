package cz.vity.freerapid.plugins.services.youtube;

import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.webclient.DownloadClient;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpDownloadClient;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import cz.vity.freerapid.utilities.LogUtils;
import org.apache.commons.httpclient.HttpMethod;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.InflaterInputStream;

/**
 * @author tong2shot
 * @author ntoskrnl
 */
class YouTubeSigDecipher {

    private static final Logger logger = Logger.getLogger(YouTubeSigDecipher.class.getName());

    private static final Pattern CLONE_PATTERN = Pattern.compile("^" + Pattern.quote("\u005d\u00ee\u002b\u00d2\u0024") + "(.)" + Pattern.quote("\u0046\u00ee\u002b\u0002\u0080\u0014") + "$", Pattern.DOTALL);
    private static final Pattern REVERSE_PATTERN = Pattern.compile("^" + Pattern.quote("\u005d\u00ef\u002b\u00d2\u0046\u00ef\u002b\u0001\u0080\u0014") + "$", Pattern.DOTALL);
    private static final Pattern SWAP_PATTERN = Pattern.compile("^" + Pattern.quote("\u005d\u00f0\u002b\u00d2\u0024") + "(.)" + Pattern.quote("\u0046\u00f0\u002b\u0002\u0080\u0014") + "$", Pattern.DOTALL);

    private final HttpDownloadClient client;

    public YouTubeSigDecipher(final HttpDownloadClient client) {
        this.client = new DownloadClient();
        this.client.initClient(client.getSettings());
    }

    private List<String> clone(List<String> lst, int from) {
        return lst.subList(from, lst.size());
    }

    private List<String> reverse(List<String> lst) {
        Collections.reverse(lst);
        return lst;
    }

    private List<String> swap(List<String> lstSig, int pos) {
        String head = lstSig.get(0);
        String headSwapTo = lstSig.get(pos % lstSig.size());
        lstSig.set(0, headSwapTo);
        lstSig.set(pos, head);
        return lstSig;
    }

    public String decipher(final HttpMethod method, final String sig) throws Exception {
        final String bytecode = getBytecode(method);
        List<String> lstSig = new ArrayList<String>(Arrays.asList(sig.split("")));
        lstSig.remove(0); //remove empty char at head
        for (final String callBytecode : bytecode.split("\\u00d6")) {
            Matcher matcher = CLONE_PATTERN.matcher(callBytecode);
            if (matcher.find()) {
                final int arg = matcher.group(1).charAt(0);
                logger.info("clone " + arg);
                lstSig = clone(lstSig, arg);
                continue;
            }
            matcher = REVERSE_PATTERN.matcher(callBytecode);
            if (matcher.find()) {
                logger.info("reverse");
                lstSig = reverse(lstSig);
                continue;
            }
            matcher = SWAP_PATTERN.matcher(callBytecode);
            if (matcher.find()) {
                final int arg = matcher.group(1).charAt(0);
                logger.info("swap " + arg);
                lstSig = swap(lstSig, arg);
                continue;
            }
            throw new PluginImplementationException("Error parsing SWF (2)");
        }
        StringBuilder sb = new StringBuilder();
        for (String s : lstSig) {
            sb.append(s);
        }
        return sb.toString();
    }

    private String getBytecode(final HttpMethod method) throws Exception {
        final InputStream is = client.makeRequestForFile(method);
        if (is == null) {
            throw new ServiceConnectionProblemException("Error downloading SWF");
        }
        final String swf = readSwfStreamToString(is);

        final String regex = "(?s)" + Pattern.quote("\u00d0\u0030\u00d1\u002c\u0001\u0046\u00ba\u002e\u0001\u0080\u0014\u00d6")
                + "(.+?)" + Pattern.quote("\u00d6\u00d2\u002c\u0001\u0046\u00e8\u0030\u0001\u0048");
        final Matcher matcher = PlugUtils.matcher(regex, swf);
        if (!matcher.find()) {
            throw new PluginImplementationException("Error parsing SWF (1)");
        }
        return matcher.group(1);
    }

    private static String readSwfStreamToString(InputStream is) throws IOException {
        try {
            final byte[] bytes = new byte[2048];
            if (readBytes(is, bytes, 8) != 8) {
                throw new IOException("Error reading from stream");
            }
            if (bytes[0] == 'C' && bytes[1] == 'W' && bytes[2] == 'S') {
                bytes[0] = 'F';
                is = new InflaterInputStream(is);
            } else if (bytes[0] != 'F' || bytes[1] != 'W' || bytes[2] != 'S') {
                throw new IOException("Invalid SWF stream");
            }
            final StringBuilder sb = new StringBuilder(8192);
            sb.append(new String(bytes, 0, 8, "ISO-8859-1"));
            int len;
            while ((len = is.read(bytes)) != -1) {
                sb.append(new String(bytes, 0, len, "ISO-8859-1"));
            }
            return sb.toString();
        } finally {
            try {
                is.close();
            } catch (final Exception e) {
                LogUtils.processException(logger, e);
            }
        }
    }

    private static int readBytes(InputStream is, byte[] buffer, int count) throws IOException {
        int read = 0, i;
        while (count > 0 && (i = is.read(buffer, 0, count)) != -1) {
            count -= i;
            read += i;
        }
        return read;
    }

}
