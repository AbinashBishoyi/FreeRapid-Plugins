package cz.vity.freerapid.plugins.services.communitychannel;

import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;

import java.net.URL;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 */
class CommunityChannelFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(CommunityChannelFileRunner.class.getName());

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final Matcher matcher = PlugUtils.matcher("/video/([^/]+)", fileURL);
        if (!matcher.find()) {
            throw new PluginImplementationException("Error parsing file URL");
        }
        httpFile.setNewURL(new URL("http://www.youtube.com/watch?v=" + matcher.group(1)));
        httpFile.setPluginID("");
        httpFile.setState(DownloadState.QUEUED);
    }

}