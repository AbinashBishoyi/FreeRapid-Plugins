package cz.vity.freerapid.plugins.services.tor;

import com.subgraph.orchid.Tor;
import com.subgraph.orchid.TorClient;
import com.subgraph.orchid.TorConfig;
import cz.vity.freerapid.plugins.services.geoip.CountryLocator;
import cz.vity.freerapid.plugins.webclient.ConnectionSettings;
import cz.vity.freerapid.plugins.webclient.interfaces.ConfigurationStorageSupport;
import cz.vity.freerapid.utilities.LogUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.protocol.Protocol;
import org.jdesktop.application.ApplicationContext;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Example usage:
 * <p/>
 * <pre>{@code
 * HttpMethod method = ...;
 * TorProxy proxy = TorProxy.forCountryIfNecessary("us", ...);
 * try {
 *     proxy.open();
 *     proxy.modifyMethod(method);
 *     //make request...
 * } finally {
 *     proxy.close();
 * }}</pre>
 * <p/>
 * Should perhaps implement {@link AutoCloseable} in the future.
 *
 * @author ntoskrnl
 */
public class TorProxy {

    private static final Logger logger = Logger.getLogger(TorProxy.class.getName());

    private static final Map<String, TorProxy> PROXIES = new HashMap<String, TorProxy>();

    private final String country;
    private final String protocolId;
    private final ConfigurationStorageSupport storage;
    private TorClient client;
    private ScheduledFuture<?> shutdownTask;
    private int refereceCount;

    private TorProxy(final String country, final ConfigurationStorageSupport storage) {
        this.country = country;
        this.protocolId = "tor-" + country + "-http";
        this.storage = storage;
    }

    public static TorProxy forCountryIfNecessary(final String country, final ConfigurationStorageSupport storage, final ConnectionSettings connection) {
        if (!connection.isProxySet() && !CountryLocator.getDefault().is(country)) {
            return forCountry(country, storage);
        }
        return DummyProxy.INSTANCE;
    }

    public static synchronized TorProxy forCountry(final String country, final ConfigurationStorageSupport storage) {
        TorProxy proxy = PROXIES.get(country);
        if (proxy == null) {
            proxy = new TorProxy(country, storage);
            PROXIES.put(country, proxy);
        }
        return proxy;
    }

    public void modifyMethod(final HttpMethod method) throws Exception {
        final URI uri = method.getURI();
        method.setURI(new URI(protocolId, uri.getUserinfo(), uri.getHost(),
                uri.getPort(), uri.getPath(), uri.getQuery(), uri.getFragment()));
    }

    public synchronized void open() throws Exception {
        refereceCount++;
        if (shutdownTask != null) {
            shutdownTask.cancel(true);
            shutdownTask = null;
        }
        if (client == null) {
            init();
        }
    }

    private void init() throws Exception {
        logger.info("Initializing proxy for '" + country + "'");
        final TorConfig config = Tor.createConfig();
        config.setExitNodes(Arrays.asList("{" + country + "}"));
        config.setDataDirectory(new File(getConfigDirectory(), "tor-" + country));
        client = new TorClient(config);
        client.start();
        client.waitUntilReady();
        final Protocol protocol = new Protocol(protocolId, new TorProtocolSocketFactory(client.getSocketFactory()), 80);
        Protocol.registerProtocol(protocolId, protocol);
    }

    private File getConfigDirectory() throws Exception {
        //we really need a getConfigDirectory() method in ConfigurationStorageSupport
        final Field field = storage.getClass().getDeclaredField("context");
        field.setAccessible(true);
        final ApplicationContext context = (ApplicationContext) field.get(storage);
        return context.getLocalStorage().getDirectory();
    }

    public synchronized void close() {
        if (refereceCount < 1) {
            throw new IllegalStateException();
        }
        refereceCount--;
        if (refereceCount == 0) {
            final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
            try {
                shutdownTask = executor.schedule(new ShutdownTask(), 1, TimeUnit.MINUTES);
            } finally {
                executor.shutdown();
            }
        }
    }

    private class ShutdownTask implements Runnable {
        @Override
        public void run() {
            synchronized (TorProxy.this) {
                if (Thread.currentThread().isInterrupted()) {
                    //the task was cancelled while waiting on 'TorProxy.this'
                    return;
                }
                logger.info("Shutting down proxy for '" + country + "'");
                try {
                    Protocol.unregisterProtocol(protocolId);
                    client.stop();
                    client = null;
                    shutdownTask = null;
                } catch (final Exception e) {
                    LogUtils.processException(logger, e);
                }
            }
        }
    }

    private static class DummyProxy extends TorProxy {
        public static final TorProxy INSTANCE = new DummyProxy();

        private DummyProxy() {
            super(null, null);
        }

        @Override
        public void modifyMethod(final HttpMethod method) throws Exception {
        }

        @Override
        public void open() throws Exception {
        }

        @Override
        public void close() {
        }
    }

}
