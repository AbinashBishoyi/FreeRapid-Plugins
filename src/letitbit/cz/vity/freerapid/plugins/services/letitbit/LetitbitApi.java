package cz.vity.freerapid.plugins.services.letitbit;

import cz.vity.freerapid.plugins.webclient.DownloadClient;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpDownloadClient;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

/**
 * @author ntoskrnl
 */
class LetitbitApi {

    private static final Logger logger = Logger.getLogger(LetitbitApi.class.getName());

    private static final String VERSION = "2.12";

    private final HttpDownloadClient client;

    public LetitbitApi(final HttpDownloadClient client) {
        this.client = new DownloadClient();
        this.client.initClient(client.getSettings());
    }

    public List<String> getDownloadUrls(final String fileURL) throws Exception {
        final HttpMethod method = new MethodBuilder(client)
                .setAction("http://api.letitbit.net/internal/index4.php")
                .setParameter("action", "LINK_GET_DIRECT")
                .setParameter("link", fileURL)
                .setParameter("free_link", "1")
                .setParameter("appid", createRandomAppId())
                .setParameter("version", VERSION)
                .setHeader("User-Agent", null)
                .toPostMethod();
        final List<String> list = new LinkedList<String>();
        if (makeRedirectedRequest(method)) {
            for (final String line : client.getContentAsString().split("[\r\n]")) {
                if (line.startsWith("http")) {
                    list.add(line);
                }
            }
        }
        if (list.isEmpty()) {
            logger.warning("API download failed; content from last request:\n" + client.getContentAsString());
            return null;
        } else {
            logger.info("API download success");
            return list;
        }
    }

    private boolean makeRedirectedRequest(final HttpMethod method) throws Exception {
        return client.makeRequest(method, true) == HttpStatus.SC_OK;
    }

    private static String createRandomAppId() {
        final byte[] bytes = new byte[16];
        new Random().nextBytes(bytes);
        return Hex.encodeHexString(bytes);
    }

}
