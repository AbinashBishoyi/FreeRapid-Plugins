package cz.vity.freerapid.plugins.services.oneclickmoviez;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.methods.GetMethod;

import java.net.URI;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class OneClickMoviezFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(OneClickMoviezFileRunner.class.getName());

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            checkProblems();//check problems
            final Matcher match = PlugUtils.matcher("http://.+?(https?://.+)", method.getURI().getURI());
            if (match.find()) { // Single redirected link
                this.httpFile.setNewURL(new URL(match.group(1))); //to setup new URL
                this.httpFile.setFileState(FileState.NOT_CHECKED);
                this.httpFile.setPluginID(""); //to run detection what plugin should be used for new URL, when file is in QUEUED state
                this.httpFile.setState(DownloadState.QUEUED);
            } else { // Multiple links
                List<URI> list = new LinkedList<URI>();
                final Matcher m = PlugUtils.matcher("href=\"(http://(www\\.)?oneclickmoviez\\.com/.+?/.+?/\\d+?/\\d{1,2})\"", getContentAsString());
                while (m.find()) {
                    if (!m.group(1).contains("/IMDB/"))
                        list.add(new URI(m.group(1).trim().replace(" ", "%20")));
                }
                if (list.isEmpty()) throw new PluginImplementationException("No link(s) found");
                getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(this.httpFile, list);
                this.httpFile.setFileName("Link(s) Extracted !");
                this.httpFile.setState(DownloadState.COMPLETED);
                this.httpFile.getProperties().put("removeCompleted", true);
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("The page you are looking is not here")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }

}