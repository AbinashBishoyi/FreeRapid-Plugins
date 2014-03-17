package com.subgraph.orchid;

import com.subgraph.orchid.circuits.TorInitializationTracker;
import com.subgraph.orchid.crypto.PRNGFixes;
import com.subgraph.orchid.dashboard.Dashboard;
import com.subgraph.orchid.directory.downloader.DirectoryDownloaderImpl;
import com.subgraph.orchid.sockets.OrchidSocketFactory;

import javax.crypto.Cipher;
import javax.net.SocketFactory;
import java.lang.reflect.Field;
import java.security.NoSuchAlgorithmException;
import java.security.Permission;
import java.security.PermissionCollection;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class is the main entry-point for running a Tor proxy
 * or client.
 */
public class TorClient {
    private final static Logger logger = Logger.getLogger(TorClient.class.getName());
    private final TorConfig config;
    private final Directory directory;
    private final TorInitializationTracker initializationTracker;
    private final ConnectionCache connectionCache;
    private final CircuitManager circuitManager;
    private final SocksPortListener socksListener;
    private final DirectoryDownloaderImpl directoryDownloader;
    private final Dashboard dashboard;

    private boolean isStarted = false;
    private boolean isStopped = false;

    private final CountDownLatch readyLatch;

    static {
        removeCryptographyRestrictions();
    }

    public TorClient() {
        this(null);
    }

    public TorClient(TorConfig config) {
        this(config, null);
    }

    public TorClient(TorConfig config, DirectoryStore customDirectoryStore) {
        if (Tor.isAndroidRuntime()) {
            PRNGFixes.apply();
        }
        this.config = config;
        directory = Tor.createDirectory(config, customDirectoryStore);
        initializationTracker = Tor.createInitalizationTracker();
        initializationTracker.addListener(createReadyFlagInitializationListener());
        connectionCache = Tor.createConnectionCache(config, initializationTracker);
        directoryDownloader = Tor.createDirectoryDownloader(config, initializationTracker);
        circuitManager = Tor.createCircuitManager(config, directoryDownloader, directory, connectionCache, initializationTracker);
        socksListener = Tor.createSocksPortListener(config, circuitManager);
        readyLatch = new CountDownLatch(1);
        dashboard = new Dashboard();
        dashboard.addRenderables(circuitManager, directoryDownloader, socksListener);
    }

    public TorConfig getConfig() {
        return config;
    }

    public SocketFactory getSocketFactory() {
        return new OrchidSocketFactory(this);
    }

    /**
     * Start running the Tor client service.
     */
    public synchronized void start() {
        if (isStarted) {
            return;
        }
        if (isStopped) {
            throw new IllegalStateException("Cannot restart a TorClient instance.  Create a new instance instead.");
        }
        logger.info("Starting Orchid (version: " + Tor.getFullVersion() + ")");
        //verifyUnlimitedStrengthPolicyInstalled();
        directoryDownloader.start(directory);
        circuitManager.startBuildingCircuits();
        if (dashboard.isEnabledByProperty()) {
            dashboard.startListening();
        }
        isStarted = true;
    }

    public synchronized void stop() {
        if (!isStarted || isStopped) {
            return;
        }
        try {
            socksListener.stop();
            if (dashboard.isListening()) {
                dashboard.stopListening();
            }
            directoryDownloader.stop();
            circuitManager.stopBuildingCircuits(true);
            directory.close();
            connectionCache.close();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Unexpected exception while shutting down TorClient instance: " + e, e);
        } finally {
            isStopped = true;
        }
    }

    public Directory getDirectory() {
        return directory;
    }

    public ConnectionCache getConnectionCache() {
        return connectionCache;
    }

    public CircuitManager getCircuitManager() {
        return circuitManager;
    }

    public DirectoryDownloader getDirectoryDownloader() {
        return directoryDownloader;
    }

    public void waitUntilReady() throws InterruptedException {
        readyLatch.await();
    }

    public void waitUntilReady(long timeout) throws InterruptedException, TimeoutException {
        if (!readyLatch.await(timeout, TimeUnit.MILLISECONDS)) {
            throw new TimeoutException();
        }
    }

    public Stream openExitStreamTo(String hostname, int port) throws InterruptedException, TimeoutException, OpenFailedException {
        ensureStarted();
        return circuitManager.openExitStreamTo(hostname, port);
    }

    private synchronized void ensureStarted() {
        if (!isStarted) {
            throw new IllegalStateException("Must call start() first");
        }
    }

    public void enableSocksListener(int port) {
        socksListener.addListeningPort(port);
    }

    public void enableSocksListener() {
        enableSocksListener(9150);
    }

    public void enableDashboard() {
        if (!dashboard.isListening()) {
            dashboard.startListening();
        }
    }

    public void enableDashboard(int port) {
        dashboard.setListeningPort(port);
        enableDashboard();
    }

    public void disableDashboard() {
        if (dashboard.isListening()) {
            dashboard.stopListening();
        }
    }

    public void addInitializationListener(TorInitializationListener listener) {
        initializationTracker.addListener(listener);
    }

    public void removeInitializationListener(TorInitializationListener listener) {
        initializationTracker.removeListener(listener);
    }

    private TorInitializationListener createReadyFlagInitializationListener() {
        return new TorInitializationListener() {
            public void initializationProgress(String message, int percent) {
            }

            public void initializationCompleted() {
                readyLatch.countDown();
            }
        };
    }

    public static void main(String[] args) {
        final TorClient client = new TorClient();
        client.addInitializationListener(createInitalizationListner());
        client.start();
        client.enableSocksListener();
    }

    private static TorInitializationListener createInitalizationListner() {
        return new TorInitializationListener() {

            public void initializationProgress(String message, int percent) {
                System.out.println(">>> [ " + percent + "% ]: " + message);
            }

            public void initializationCompleted() {
                System.out.println("Tor is ready to go!");
            }
        };
    }

    private void verifyUnlimitedStrengthPolicyInstalled() {
        try {
            if (Cipher.getMaxAllowedKeyLength("AES") < 256) {
                final String message = "Unlimited Strength Jurisdiction Policy Files are required but not installed.";
                logger.severe(message);
                throw new TorException(message);
            }
        } catch (NoSuchAlgorithmException e) {
            logger.log(Level.SEVERE, "No AES provider found");
            throw new TorException(e);
        } catch (NoSuchMethodError e) {
            logger.info("Skipped check for Unlimited Strength Jurisdiction Policy Files");
        }
    }

    /**
     * The Tor protocol makes use of a 256-bit TLS cipher suite.
     * Setting 'isRestricted' to false (as is done in cz.vity.freerapid.utilities.Utils)
     * is not enough to allow this; 'defaultPolicy' has to be modified too.
     * This should probably be moved to Utils in the next version of FRD.
     */
    private static void removeCryptographyRestrictions() {
        if (!isRestrictedCryptography()) {
            logger.fine("Cryptography restrictions removal not needed");
            return;
        }
        try {
            /*
             * Do the following, but with reflection to bypass access checks:
             *
             * JceSecurity.isRestricted = false;
             * JceSecurity.defaultPolicy.perms.clear();
             * JceSecurity.defaultPolicy.add(CryptoAllPermission.INSTANCE);
             */
            final Class<?> jceSecurity = Class.forName("javax.crypto.JceSecurity");
            final Class<?> cryptoPermissions = Class.forName("javax.crypto.CryptoPermissions");
            final Class<?> cryptoAllPermission = Class.forName("javax.crypto.CryptoAllPermission");

            final Field isRestrictedField = jceSecurity.getDeclaredField("isRestricted");
            isRestrictedField.setAccessible(true);
            isRestrictedField.set(null, false);

            final Field defaultPolicyField = jceSecurity.getDeclaredField("defaultPolicy");
            defaultPolicyField.setAccessible(true);
            final PermissionCollection defaultPolicy = (PermissionCollection) defaultPolicyField.get(null);

            final Field perms = cryptoPermissions.getDeclaredField("perms");
            perms.setAccessible(true);
            ((Map<?, ?>) perms.get(defaultPolicy)).clear();

            final Field instance = cryptoAllPermission.getDeclaredField("INSTANCE");
            instance.setAccessible(true);
            defaultPolicy.add((Permission) instance.get(null));

            logger.fine("Successfully removed cryptography restrictions");
        } catch (final Throwable e) {
            logger.log(Level.WARNING, "Failed to remove cryptography restrictions", e);
        }
    }

    private static boolean isRestrictedCryptography() {
        return "Java(TM) SE Runtime Environment".equals(System.getProperty("java.runtime.name"));
    }

}
