package cz.vity.freerapid.plugins.services.hotfilefolder;

import cz.vity.freerapid.plugins.exceptions.InvalidURLOrServiceProblemException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import org.apache.commons.httpclient.HttpMethod;

import java.net.URI;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author RickCL
 */
class HotFileFolderRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(HotFileFolderRunner.class.getName());

    final private List<URI> queye = new LinkedList<URI>();

    @Override
    public void run() throws Exception {
        super.run();

        logger.info("Starting run task " + fileURL);

        final HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL).setAction(fileURL).toHttpMethod();
        if( !makeRedirectedRequest(httpMethod) ) {
            throw new InvalidURLOrServiceProblemException("Invalid URL or service problem");
        }
        final Matcher matcher = getMatcherAgainstContent("\"(http://.*?hotfile\\.com/(dl|links)/.+)\"");

        while(matcher.find()) {
            queye.add(new URI(matcher.group(1)));
        }
        if( queye.isEmpty() ) {
            throw new PluginImplementationException("Can't locate any file !!!");
        }

        httpFile.setState(DownloadState.COMPLETED);
        synchronized ( getPluginService().getPluginContext().getQueueSupport() ) {
            getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, queye);
        }
    }

}
