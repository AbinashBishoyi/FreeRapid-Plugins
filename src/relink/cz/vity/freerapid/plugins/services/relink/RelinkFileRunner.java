package cz.vity.freerapid.plugins.services.relink;

import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
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
 * @author benpicco
 */
class RelinkFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(RelinkFileRunner.class.getName());

    private final String HTTP_SERIENJUNKIES = "http://download.serienjunkies.org";

    @Override
    public void run() throws Exception {
        super.run();
        //checkURL(fileURL);
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {

            checkProblems();


            String content = getContentAsString();
            Matcher matcher = PlugUtils.matcher("getFile\\('(.+)'\\);", content);

            List<URI> uriList = new LinkedList<URI>();

            while (matcher.find()) {

                String url = "http://www.relink.us/frame.php?" + PlugUtils.getStringBetween(matcher.group(), "getFile(\'", "\');");

                // We will get to a frameset that encapsulates the real page
                if (!makeRedirectedRequest(getGetMethod(url)))
                    logger.warning("Request Failed");

                // escape the frameset
                try {
                    url = PlugUtils.getStringBetween(getContentAsString(), "<iframe name=\"Container\" height=\"100%\" frameborder=\"no\" width=\"100%\" src=\"", "\"");
                } catch (PluginImplementationException e) {
                    logger.warning(getContentAsString());
                }

//                // We are getting redirected to the real site now
//                HttpMethod method = this.getGetMethod(url);
//                this.makeRedirectedRequest(method);            
//
//                url = method.getURI().toString();

                logger.info("New URL : " + url);

                uriList.add(new URI(url));

                // We have to wait again, otherwise we will be detected as a bot
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException ie) {
                }
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

    private void checkProblems() throws ServiceConnectionProblemException {
        if (getContentAsString().contains("Dieser Container wurde nicht gefunden!")) {
            throw new ServiceConnectionProblemException("relink.us Error : Link was not found on this server!");
        }
    }
}
