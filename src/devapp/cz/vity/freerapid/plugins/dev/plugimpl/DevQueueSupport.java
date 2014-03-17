package cz.vity.freerapid.plugins.dev.plugimpl;

import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import cz.vity.freerapid.plugins.webclient.interfaces.MaintainQueueSupport;

import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.logging.Logger;

/**
 * @author Vity
 */
public class DevQueueSupport implements MaintainQueueSupport {
    private final static Logger logger = Logger.getLogger(DevQueueSupport.class.getName());

    @Override
    public boolean addLinksToQueue(HttpFile parentFile, List<URI> uriList) {
        StringBuilder builder = new StringBuilder().append("The following files were added into the queue:\n");
        for (URI uri : uriList) {
            builder.append(String.format("%s%n", uri));
        }
        logger.info(builder.toString());
        return false;
    }

    public boolean addLinksToQueue(HttpFile parentFile, String data) {
        StringBuilder builder = new StringBuilder().append("The following files were added into the queue:\n").append(data);
        logger.info(builder.toString());
        return false;
    }

    public boolean addLinkToQueueUsingPriority(HttpFile parentFile, String data) throws Exception {
        StringBuilder builder = new StringBuilder().append("The following files were added into the queue:\n").append(data);
        logger.info(builder.toString());
        return false;
    }

    public boolean addLinkToQueueUsingPriority(HttpFile parentFile, List<URL> urlList) throws Exception {
        StringBuilder builder = new StringBuilder().append("The following files were added into the queue:\n");
        for (URL uri : urlList) {
            builder.append(String.format("%s%n", uri));
        }
        logger.info(builder.toString());
        return false;

    }
}
