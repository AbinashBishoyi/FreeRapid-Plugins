package cz.vity.freerapid.plugins.services.linkbee;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import org.apache.commons.httpclient.methods.GetMethod;

import java.net.URL;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author Javi
 */
class LinkBeeFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(LinkBeeFileRunner.class.getName());

    @Override
    public void run() throws Exception {
        super.run();
        fileURL = fileURL.replace("linkb.com", "linkbee.com");
        logger.info("Starting run task " + fileURL);

        final GetMethod method = getGetMethod(fileURL);

        if (makeRedirectedRequest(method)) {
            checkProblems();
            final String content = getContentAsString();

            String s = null;
            if (content.contains("skipBtn")) {
                Matcher matcher = getMatcherAgainstContent("id='urlholder' value='(.*?)' />");
                if (matcher.find()) {
                    s = matcher.group(1);
                    logger.info("1 " + matcher.group(1));
                }
            } else if (content.contains("iframe")) {
                Matcher matcher = getMatcherAgainstContent("<iframe src=\"(.*?)\" frameborder=\"0\"");
                if (matcher.find()) {
                    s = matcher.group(1);
                    logger.info("2 " + matcher.group(1));
                }
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
        if (contentAsString.contains("invalid")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }

}