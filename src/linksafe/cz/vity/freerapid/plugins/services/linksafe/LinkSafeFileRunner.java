package cz.vity.freerapid.plugins.services.linksafe;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.InvalidURLOrServiceProblemException;
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
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author hanakus
 */
class LinkSafeFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(LinkSafeFileRunner.class.getName());

    @Override
    public void run() throws Exception {
        super.run();
        //checkURL(fileURL);
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {

            checkProblems();

            String content = getContentAsString();
            Matcher matcher;

            if (fileURL.matches("^http://linksafe.me/p/.+$")) {
                matcher = PlugUtils.matcher("<a target=\"_blank\" href=\"d/[a-z0-9]{10,10}\">(.*?)</a><br />", content);
            } else if (fileURL.matches("^http://linksafe.me/d/.+$")) {
                matcher = PlugUtils.matcher("window.location=\"(.*?)\";", content);
            } else {
                // wrong url, throw Exception
                throw new InvalidURLOrServiceProblemException("Invalid URL");
            }

            List<URI> uriList = new LinkedList<URI>();

            while (matcher.find()) {

                String url = matcher.group(1);

                logger.info("New URL : " + url);

                uriList.add(new URI(url));

                // We have to wait again, otherwise we will be detected as a bot
                /*try {
                    Thread.sleep(3000);
                } catch (InterruptedException ie) {
                } */
            }

            // We converti this download task in order to keep the list clean
            String url = uriList.remove(0).toString();
            getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, uriList);
            this.httpFile.setNewURL(new URL(url));
            this.httpFile.setPluginID("");
            this.httpFile.setState(DownloadState.QUEUED);

        } else {
            logger.warning("Request failed");
            checkProblems();
        }

    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("Link Not Found")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }

}