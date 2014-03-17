package cz.vity.freerapid.plugins.dev.plugimpl;

import cz.vity.freerapid.plugins.container.FileInfo;
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
        StringBuilder builder = new StringBuilder().append("The following files will be added into the queue:\n");
        for (URI uri : uriList) {
            builder.append(uri).append('\n');
        }
        logger.info(builder.toString());
        return true;
    }

    @Override
    public boolean addLinksToQueue(HttpFile parentFile, String data) {
        StringBuilder builder = new StringBuilder().append("The following data will be parsed for links to be added into the queue:\n\n").append(data);
        logger.info(builder.toString());
        return true;
    }

    @Override
    public boolean addLinksToQueueFromContainer(HttpFile parentFile, List<FileInfo> infoList) {
        StringBuilder builder = new StringBuilder().append("The following files will be added into the queue:\n");
        for (FileInfo info : infoList) {
            builder.append(info.getFileUrl()).append('\n');
        }
        logger.info(builder.toString());
        return true;
    }

    @Override
    public boolean addLinkToQueueUsingPriority(HttpFile parentFile, List<URL> urlList) throws Exception {
        StringBuilder builder = new StringBuilder().append("One of the following files will be added into the queue:\n");
        for (URL uri : urlList) {
            builder.append(uri).append('\n');
        }
        logger.info(builder.toString());
        return true;
    }

    @Override
    public boolean addLinkToQueueUsingPriority(HttpFile parentFile, String data) throws Exception {
        StringBuilder builder = new StringBuilder().append("The following data will be parsed for links of which one will be chosen to be added into the queue:\n\n").append(data);
        logger.info(builder.toString());
        return true;
    }

    @Override
    public boolean addLinkToQueueFromContainerUsingPriority(HttpFile parentFile, List<FileInfo> infoList) {
        StringBuilder builder = new StringBuilder().append("One of the following files will be added into the queue:\n");
        for (FileInfo info : infoList) {
            builder.append(info.getFileUrl()).append('\n');
        }
        logger.info(builder.toString());
        return true;
    }

}
