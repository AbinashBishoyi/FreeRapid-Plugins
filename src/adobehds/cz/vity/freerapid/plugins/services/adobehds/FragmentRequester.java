package cz.vity.freerapid.plugins.services.adobehds;

import cz.vity.freerapid.plugins.webclient.interfaces.HttpDownloadClient;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethod;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author ntoskrnl
 */
class FragmentRequester {

    private final HttpFile httpFile;
    private final HttpDownloadClient client;
    private final HdsMedia media;
    private int currentFragment = 1;

    public FragmentRequester(final HttpFile httpFile, final HttpDownloadClient client, final HdsMedia media) {
        this.httpFile = httpFile;
        this.client = client;
        this.media = media;
    }

    public InputStream nextFragment() throws IOException {
        if (currentFragment > media.getFragmentCount()) {
            return null;
        }
        final String url = media.getUrl() + "Seg1-Frag" + currentFragment;
        final HttpMethod method = client.getGetMethod(url);
        final InputStream in = client.makeRequestForFile(method);
        if (in == null) {
            throw new IOException("Failed to request fragment " + currentFragment);
        }
        if (httpFile.getFileSize() <= 0) {
            final Header header = method.getResponseHeader("Content-Length");
            if (header != null) {
                final long fragmentSize = Long.parseLong(header.getValue());
                httpFile.setFileSize(fragmentSize * media.getFragmentCount());//estimate
            }
        }
        currentFragment++;
        return new FragmentInputStream(method, in);
    }

}
