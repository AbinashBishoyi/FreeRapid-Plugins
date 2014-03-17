package cz.vity.freerapid.plugins.services.tunlr;

import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.URI;

import javax.naming.Context;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author ntoskrnl
 */
public final class Tunlr {

    private final static Logger logger = Logger.getLogger(Tunlr.class.getName());
    private final static Map<String, CacheItem> CACHE = new HashMap<String, CacheItem>();

    public static void setupMethod(final HttpMethod method) throws PluginImplementationException, InterruptedException {
        try {
            final URI uri = method.getURI();
            final String host = uri.getHost();
            final String ip = getIpFor(host);
            method.setURI(new URI(uri.getScheme(), uri.getUserinfo(), ip,
                    uri.getPort(), uri.getPath(), uri.getQuery(), uri.getFragment()));
            method.getParams().setVirtualHost(host);
            logger.info(host + " --> " + ip);
        } catch (final InterruptedException e) {
            throw e;
        } catch (final Exception e) {
            throw new PluginImplementationException(e);
        }
    }

    private static String getIpFor(final String host) throws Exception {
        CacheItem item;
        synchronized (Tunlr.class) {
            item = CACHE.get(host);
            if (item == null || item.isOld()) {
                item = new CacheItem(host);
                CACHE.put(host, item);
            }
        }
        return item.get();
    }

    private static String lookup(final String host) throws Exception {
        final Hashtable<String, String> env = new Hashtable<String, String>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.dns.DnsContextFactory");
        env.put(Context.PROVIDER_URL, "dns://69.197.169.9 dns://192.95.16.109");
        final DirContext context = new InitialDirContext(env);
        final Attributes attributes = context.getAttributes(host, new String[]{"A"});
        return (String) attributes.getAll().next().get();
    }

    private static class CacheItem {
        private final static int MAX_AGE = 10 * 60 * 1000;//10 minutes

        private final FutureTask<String> task;
        private final long created;

        public CacheItem(final String host) {
            task = new FutureTask<String>(new Callable<String>() {
                @Override
                public String call() {
                    try {
                        return lookup(host);
                    } catch (final Exception e) {
                        logger.log(Level.INFO, "DNS lookup exception", e);
                        logger.warning("DNS lookup exception: " + e);
                        return host;
                    }
                }
            });
            created = System.currentTimeMillis();
        }

        public String get() throws Exception {
            task.run();
            return task.get();
        }

        public boolean isOld() {
            return System.currentTimeMillis() - created > MAX_AGE;
        }
    }

    private Tunlr() {
    }

}
