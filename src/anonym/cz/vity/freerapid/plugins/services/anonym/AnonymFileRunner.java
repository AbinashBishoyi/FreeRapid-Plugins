package cz.vity.freerapid.plugins.services.anonym;

import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadState;

import java.net.URL;
import java.net.URLDecoder;
import java.util.logging.Logger;

/**
 * @author benpicco
 */
class AnonymFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(AnonymFileRunner.class.getName());


    @Override
    public void run() throws Exception {
        super.run();
        String url = URLDecoder.decode(fileURL.substring(fileURL.indexOf("?") + 1), "UTF-8");

        logger.info("New URL: " + url);

        this.httpFile.setNewURL(new URL(url));  //Set New URL for the link
        this.httpFile.setPluginID("");
        this.httpFile.setState(DownloadState.QUEUED);
    }
}
