package cz.vity.freerapid.plugins.services.linkbucks;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import org.apache.commons.httpclient.methods.GetMethod;

import java.net.URL;
import java.util.logging.Logger;

/**
 * @author Alex
 */
class LinkBucksRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(LinkBucksRunner.class.getName());

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting run task " + fileURL);
        final GetMethod method = getGetMethod(fileURL);
        logger.info(fileURL);

        if (makeRedirectedRequest(method)) {
            String content = getContentAsString();

            //Matcher matcher = PlugUtils.matcher("var LinkURL = '(.+?)'", content);//getMatcherAgainstContent("href=\"(.+?)+\" id=\"linkBucksSkip");
            final String s = getMethodBuilder().setActionFromAHrefWhereATagContains("Skip This Page").getAction();
            logger.info("New Links :" + s);
            this.httpFile.setNewURL(new URL(s));
            this.httpFile.setPluginID("");
            this.httpFile.setState(DownloadState.QUEUED);

        } else throw new PluginImplementationException();
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("Unable to find site")) {
            throw new URLNotAvailableAnymoreException("Unable to find site's URL to redirect to.");
        }
    }

}
