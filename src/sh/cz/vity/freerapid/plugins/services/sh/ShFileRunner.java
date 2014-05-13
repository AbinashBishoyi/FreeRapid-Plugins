package cz.vity.freerapid.plugins.services.sh;

import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethod;

import java.net.URL;
import java.util.logging.Logger;

/**
 * Class which contains main code
 *
 * @author CrazyCoder
 */
class ShFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(ShFileRunner.class.getName());

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final HttpMethod httpMethod = getMethodBuilder()
                .setReferer(fileURL)
                .setAction(fileURL)
                .toGetMethod();
        // sh.st redirects automatically for non-browser user agents
        httpMethod.setRequestHeader("User-Agent", "curl/7.18.2 (i586-pc-mingw32msvc) libcurl/7.18.2 zlib/1.2.3");
        final int st = client.makeRequest(httpMethod, false);
        if (st / 100 == 3) {
            final Header locationHeader = httpMethod.getResponseHeader("Location");
            String directURL = locationHeader.getValue();

            logger.info("Download Service URL: " + directURL);
            httpFile.setNewURL(new URL(directURL));
            httpFile.setPluginID("");
            httpFile.setState(DownloadState.QUEUED);
        } else {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }
}
