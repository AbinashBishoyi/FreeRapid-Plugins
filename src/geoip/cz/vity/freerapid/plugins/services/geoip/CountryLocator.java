package cz.vity.freerapid.plugins.services.geoip;

import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.webclient.ConnectionSettings;
import cz.vity.freerapid.plugins.webclient.DownloadClient;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpDownloadClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;

import java.net.InetAddress;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author ntoskrnl
 */
public class CountryLocator {

    private static final Logger logger = Logger.getLogger(CountryLocator.class.getName());

    private final String userCountry;

    public CountryLocator(final ConnectionSettings connectionSettings) {
        userCountry = identifyCountry(connectionSettings);
    }

    public boolean is(final String country) {
        return userCountry.equals(country);
    }

    private static String identifyCountry(final ConnectionSettings settings) {
        try {
            final String s = fetchIpAddress(settings);
            final InetAddress ipAddress = InetAddress.getByName(s);
            final String country = CountryCodeService.getInstance()
                    .getCountryCodeForAddress(ipAddress).toLowerCase(Locale.ROOT);
            final String connectionName = settings.getProxyURL() != null ? settings.getProxyURL() : "default";
            logger.info("Connection: " + connectionName + ", country: " + country);
            return country;
        } catch (final Exception e) {
            logger.log(Level.SEVERE, "Failed to identify country", e);
            return "--";
        }
    }

    private static String fetchIpAddress(final ConnectionSettings settings) throws Exception {
        final HttpDownloadClient client = new DownloadClient();
        client.initClient(settings);
        /*
         * From http://whatismyipaddress.com/api:
         *     "We provide a specific page that returns just the IP address of the device making the request
         *     on http://bot.whatismyipaddress.com. You are free to query that server but please limit queries
         *     to no more than once per five minutes."
         * Querying the service once per program launch seems reasonable.
         */
        final HttpMethod method = client.getGetMethod("http://bot.whatismyipaddress.com/");
        if (client.makeRequest(method, false) != HttpStatus.SC_OK) {
            throw new ServiceConnectionProblemException("HTTP status " + method.getStatusCode());
        }
        return client.getContentAsString().trim();
    }

    public static CountryLocator getDefault() {
        return LazyDefault.INSTANCE;
    }

    private static class LazyDefault {
        static final CountryLocator INSTANCE = new CountryLocator(new ConnectionSettings());
    }

}
