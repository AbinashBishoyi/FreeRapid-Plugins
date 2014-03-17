package cz.vity.freerapid.plugins.services.linkbucks;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.net.URL;
import java.util.logging.Logger;

/**
 * @author Alex, ntoskrnl
 */
class LinkBucksRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(LinkBucksRunner.class.getName());

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting run task " + fileURL);
        final GetMethod method = getGetMethod(fileURL);

        if (makeRedirectedRequest(method)) {
            checkProblems();
            final String content = getContentAsString();

            String s = "";
            if (content.contains("TargetUrl = '")) {
                s = getMethodBuilder().setActionFromTextBetween("TargetUrl = '", "'").getAction();
            } else if (content.contains("frame id=\"content\"")) {
                s = getMethodBuilder().setActionFromIFrameSrcWhereTagContains("id=\"content\"").getAction();
            }
            if ((s.equals("") || s.equals("about:blank")) && (content.contains("linkbucksmedia.com/director/?t="))) {
                final String token = PlugUtils.getStringBetween(content, "linkbucksmedia.com/director/?t=", "';");
                final HttpMethod httpMethod = getMethodBuilder().setAjax()
                        .setAction("/intermission/loadTargetUrl")
                        .setParameter("t", token)
                        .setReferer(fileURL)
                        .toGetMethod();
                final int wait = PlugUtils.getNumberBetween(content, "Countdown : ", ",");
                downloadTask.sleep(wait + 1);
                if (!makeRedirectedRequest(httpMethod)) {
                    checkProblems();
                    throw new ServiceConnectionProblemException();
                }
                s = PlugUtils.getStringBetween(getContentAsString(), "Url\":\"", "\"");
            } else {
                throw new PluginImplementationException("Redirect URL not found");
            }
            logger.info("New Link: " + s);
            this.httpFile.setNewURL(new URL(s));
            this.httpFile.setPluginID("");
            this.httpFile.setState(DownloadState.QUEUED);

        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("Unable to find site")) {
            throw new URLNotAvailableAnymoreException("Unable to find site's URL to redirect to");
        }
        if (contentAsString.contains("The link you have requested could not be found"))
            throw new URLNotAvailableAnymoreException("Link not found");
    }

}
