package cz.vity.freerapid.plugins.services.bookdl;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.methods.GetMethod;

import java.net.URI;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Class which contains main code
 *
 * @author CrazyCoder
 */
class BookDLFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(BookDLFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL);

        if (makeRedirectedRequest(method)) {
            checkProblems();
            List<URI> uriList = new LinkedList<URI>();

            final String start = "<a target=\"_blank\" href=\"";
            final String end = "\" class=\"visitbutton bookbutton";
            for (int i = 1; i < 4; i++) {
                String directURL = PlugUtils.getStringBetween(getContentAsString(), start, end, i);
                if (directURL == null) break;
                logger.info("Download Service URL: " + directURL);
                uriList.add(new URI(directURL));
            }

            if (uriList.size() > 0) {
                fileURL = uriList.remove(0).toString();
                getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, uriList);
                httpFile.setNewURL(new URL(fileURL));
                httpFile.setPluginID("");
                httpFile.setState(DownloadState.QUEUED);
            } else {
                throw new ErrorDuringDownloadingException("Can't find download link");
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("File Not Found")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }
}
