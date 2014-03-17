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
import java.util.regex.Matcher;

/**
 * @author Alex, ntoskrnl, birchie
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
                final Matcher matchT = PlugUtils.matcher("linkbucksmedia.com/director/\\?t=(.+?)[\"']", content);
                if (!matchT.find())
                    throw new PluginImplementationException("linkbucks token not found");
                final String token = matchT.group(1);

                // valid authkey is from script function containing uncommented line "var f = window['init' + 'Lb' + 'js' + ''];" ("initLbjs" only, no trailing chars)
                final String authKeyMatchStr = "A(?:'\\s?\\+\\s?')?u(?:'\\s?\\+\\s?')?t(?:'\\s?\\+\\s?')?h(?:'\\s?\\+\\s?')?K(?:'\\s?\\+\\s?')?e(?:'\\s?\\+\\s?')?y";
                final String aKey1MatchStr = "\\s\\sparams\\['" + authKeyMatchStr + "'\\]\\s?=\\s?(\\d+?);";
                final String aKey2MatchStr = "\\s\\sparams\\['" + authKeyMatchStr + "'\\]\\s?=\\s?params\\['" + authKeyMatchStr + "'\\]\\s?\\+\\s?(\\d+?);";
                final String initLbjsMatchStr = "\\s\\s\\w[^/]+?js'\\s?\\+\\s?''\\];";
                final Matcher matchKey = PlugUtils.matcher(aKey1MatchStr + "[^<]+?" + aKey2MatchStr + "[^<]+?" + initLbjsMatchStr, content.replaceAll("\\s", " "));
                if (!matchKey.find())
                    throw new PluginImplementationException("linkbucks authkey not found");
                final String authKey = "" + (Long.parseLong(matchKey.group(1)) + Long.parseLong(matchKey.group(2)));

                final HttpMethod httpMethod = getMethodBuilder().setAjax()
                        .setAction("/intermission/loadTargetUrl")
                        .setParameter("t", token)
                        .setParameter("ak", authKey)
                        .setReferer(fileURL)
                        .toGetMethod();
                final int wait = PlugUtils.getNumberBetween(content, "Countdown: ", ",");
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
