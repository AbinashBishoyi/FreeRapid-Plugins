package cz.vity.freerapid.plugins.services.adobehds;

import cz.vity.freerapid.plugins.webclient.interfaces.HttpDownloadClient;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethod;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

/**
 * @author ntoskrnl
 */
public class FragmentRequester {
    private final static Logger logger = Logger.getLogger(FragmentRequester.class.getName());

    protected final HttpFile httpFile;
    protected final HttpDownloadClient client;
    protected final HdsMedia media;
    private int currentFragment = 1;
    private long totalFragmentsSize;

    public FragmentRequester(final HttpFile httpFile, final HttpDownloadClient client, final HdsMedia media) {
        this.httpFile = httpFile;
        this.client = client;
        this.media = media;
    }

    public InputStream nextFragment() throws IOException {
        if (currentFragment > media.getFragmentCount()) {
            return null;
        }
        final String url = media.getUrl() + "Seg1-Frag" + currentFragment + (media.getUrlQuery() == null ? "" : "?" + media.getUrlQuery());
        logger.info("Downloading: " + url);
        final HttpMethod method = client.getGetMethod(url);
        final InputStream in = client.makeRequestForFile(method);
        if (in == null) {
            throw new IOException("Failed to request fragment " + currentFragment);
        }
        final Header header = method.getResponseHeader("Content-Length");
        if (header != null) {
            final long fragmentSize = Long.parseLong(header.getValue());
            totalFragmentsSize += fragmentSize;
            httpFile.setFileSize((totalFragmentsSize / currentFragment) * media.getFragmentCount());//estimate
        }
        currentFragment++;
        return new FragmentInputStream(method, in);
    }

}
