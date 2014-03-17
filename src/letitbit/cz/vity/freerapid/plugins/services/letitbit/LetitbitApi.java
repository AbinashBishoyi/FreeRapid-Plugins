package cz.vity.freerapid.plugins.services.letitbit;

import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpDownloadClient;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;

import java.util.Random;
import java.util.logging.Logger;

/**
 * @author ntoskrnl
 */
public class LetitbitApi {

    private static final Logger logger = Logger.getLogger(LetitbitApi.class.getName());

    private static final String VERSION = "1.78";
    private static final int MAX_APPID_USES = 20;

    private static String appId;
    private static int appIdUses;

    private final HttpDownloadClient client;

    public LetitbitApi(final HttpDownloadClient client) {
        this.client = client;
    }

    public String getDownloadUrl(final String fileURL) throws Exception {
        final String appId = getAppId();
        final HttpMethod method = new MethodBuilder(client)
                .setAction("http://api.letitbit.net/internal/index2.php")
                .setParameter("action", "LINK_GET_DIRECT")
                .setParameter("link", fileURL)
                .setParameter("free_link", "1")
                .setParameter("appid", appId)
                .setParameter("version", VERSION)
                .toPostMethod();
        if (makeRedirectedRequest(method)) {
            for (final String line : client.getContentAsString().split("[\r\n]")) {
                if (line.startsWith("http")) {
                    logger.info("API download success");
                    return line;
                }
            }
        }
        logger.warning("API download failed; content from last request:\n" + client.getContentAsString());
        return null;
    }

    private boolean makeRedirectedRequest(final HttpMethod method) throws Exception {
        return client.makeRequest(method, true) == HttpStatus.SC_OK;
    }

    private String getAppId() throws Exception {
        synchronized (LetitbitApi.class) {
            if (appId == null || appIdUses >= MAX_APPID_USES) {
                appIdUses = 0;
                appId = createRandomAppId();
                activateAppId();
            }
            appIdUses++;
            return appId;
        }
    }

    private void activateAppId() throws Exception {
        final HttpMethod method = new MethodBuilder(client)
                .setAction("http://skymonk.net/?page=activate")
                .setParameter("act", "get_activation_key")
                .setParameter("phone", createRandomPhoneNumber())
                .setParameter("email", createRandomEmailAddress())
                .setParameter("app_id", appId)
                .setParameter("app_version", VERSION)
                .toPostMethod();
        makeRedirectedRequest(method);
        logger.info("Activation response: " + client.getContentAsString());
    }

    private static String createRandomAppId() {
        final byte[] bytes = new byte[16];
        new Random().nextBytes(bytes);
        return Hex.encodeHexString(bytes);
    }

    private static String createRandomPhoneNumber() {
        return createRandomString(11, "0123456789");
    }

    private static String createRandomEmailAddress() {
        return createRandomString(32, "abcdefghijklmnopqrstuvwxyz0123456789") + "@mail.ru";
    }

    private static String createRandomString(final int length, final String characters) {
        final Random random = new Random();
        final StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            final int index = random.nextInt(characters.length());
            sb.append(characters.charAt(index));
        }
        return sb.toString();
    }

}
