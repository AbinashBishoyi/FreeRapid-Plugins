package cz.vity.freerapid.plugins.services.tor;

import cz.vity.freerapid.plugins.webclient.interfaces.ConfigurationStorageSupport;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpDownloadClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;

/**
 * Simple version of {@link TorProxy}.
 * Example usage:
 * <p/>
 * <pre>{@code
 * HttpMethod method = ...;
 * TorProxyClient torClient = TorProxyClient.forCountry("us", ...);
 * if (!torClient.makeRequest(method) {
 *     //handle error
 * }}</pre>
 *
 * @author ntoskrnl
 */
public class TorProxyClient {

    private final TorProxy proxy;
    private final HttpDownloadClient httpClient;

    public TorProxyClient(final TorProxy proxy, final HttpDownloadClient httpClient) {
        this.proxy = proxy;
        this.httpClient = httpClient;
    }

    public static TorProxyClient forCountry(final String country, final HttpDownloadClient httpClient, final ConfigurationStorageSupport storage) {
        final TorProxy torProxy = TorProxy.forCountryIfNecessary(country, storage, httpClient.getSettings());
        return new TorProxyClient(torProxy, httpClient);
    }

    public boolean makeRequest(final HttpMethod method) throws Exception {
        try {
            proxy.open();
            proxy.modifyMethod(method);
            return httpClient.makeRequest(method, true) == HttpStatus.SC_OK;
        } finally {
            proxy.close();
        }
    }

}
