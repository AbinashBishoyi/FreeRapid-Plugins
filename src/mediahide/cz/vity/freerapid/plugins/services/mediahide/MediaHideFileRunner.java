package cz.vity.freerapid.plugins.services.mediahide;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.net.URI;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author tong2shot
 */
class MediaHideFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(MediaHideFileRunner.class.getName());

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            int tryCount = 0;
            while (getContentAsString().contains("var link='';")) {
                GetMethod getMethod = getGetMethod(fileURL);
                if (!makeRedirectedRequest(getMethod)) {
                    throw new PluginImplementationException();
                }
                tryCount++;
                if (tryCount > 5) {
                    break;
                }
            }

            final String mediafireLink;
            if (getContentAsString().contains("var link='")) {  //ajax type
                String link = "";
                if (getContentAsString().contains("var link='';")) {
                    //throw new URLNotAvailableAnymoreException("Sorry File Deleted");
                } else {
                    link = PlugUtils.getStringBetween(getContentAsString(), "var link='", "';");
                }

                final HttpMethod httpMethod = getMethodBuilder()
                        .setReferer(fileURL)
                        .setAction("http://mediahide.com/get.php?do=getlink")
                        .setParameter("url", link)
                        .setParameter("pass", "")
                        .toPostMethod();
                httpMethod.addRequestHeader("X-Requested-With", "XMLHttpRequest");

                if (!makeRedirectedRequest(httpMethod)) {
                    throw new PluginImplementationException();
                }

                checkProblems();
                if (getContentAsString().contains("recaptcha/api/noscript")) {
                    mediafireLink = PlugUtils.getStringBetween(getContentAsString(),"rel=\"canonical\" href=\"","\"/>");
                } else {
                    mediafireLink = PlugUtils.getStringBetween(getContentAsString(), "\"req\":\"", "\",").replaceAll("\\\\", "");
                }

            } else { //direct link type
                logger.info(getContentAsString().trim());
                Matcher mediafireLinkMatcher = getMatcherAgainstContent("(http://(?:www\\.)?mediafire\\.com/\\?[\\w]+)");
                if (mediafireLinkMatcher.find()) {
                    mediafireLink = mediafireLinkMatcher.group(1);
                } else {
                    throw new PluginImplementationException("Can't find mediafire link");
                }
            }

            httpFile.setNewURL(new URI(mediafireLink).toURL());
            httpFile.setPluginID("");
            httpFile.setState(DownloadState.QUEUED);

        } else {
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("\"status\":4")) {
            throw new URLNotAvailableAnymoreException("Sorry File Deleted"); //let to know user in FRD
        }
    }

}