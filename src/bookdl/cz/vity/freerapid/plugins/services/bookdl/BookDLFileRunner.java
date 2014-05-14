package cz.vity.freerapid.plugins.services.bookdl;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import org.apache.commons.httpclient.methods.GetMethod;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author CrazyCoder
 */
class BookDLFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(BookDLFileRunner.class.getName());
    private BookDLSettingsConfig config;

    private void setConfig() throws Exception {
        final BookDLServiceImpl service = (BookDLServiceImpl) getPluginService();
        config = service.getConfig();
    }

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
        setConfig();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL);

        if (makeRedirectedRequest(method)) {
            checkProblems();
            List<URI> uriList = new LinkedList<URI>();

            if (config.isDownloadPDF()) {
                addLink("pdf", uriList);
            }

            if (config.isDownloadEPUB()) {
                addLink("epub", uriList);
            }

            if (config.isDownloadMOBI()) {
                addLink("mobi", uriList);
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

    private void addLink(String format, List<URI> list) {
        String link = getLink(format);
        if (link != null) {
            try {
                list.add(new URI(link));
                logger.info("Added download link: " + link);
            } catch (URISyntaxException ignored) {
            }
        }
    }

    private String getLink(String format) {
        final String regexp = "href=\"([^\"]+?)\" class=\"visitbutton bookbutton " + format;
        final Matcher matcher = getMatcherAgainstContent(regexp);
        if (!matcher.find()) {
            return null;
        }
        return matcher.group(1);
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("File Not Found") || contentAsString.contains("Page Cannot Be Found")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }
}
