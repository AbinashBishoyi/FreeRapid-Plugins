package cz.vity.freerapid.plugins.services.usercash;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import org.apache.commons.httpclient.methods.GetMethod;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author Ladislav Vitasek
 */
class UserCashRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(UserCashRunner.class.getName());

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting run task " + fileURL);
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            final Matcher matcher = getMatcherAgainstContent("<TITLE>(.+?)</TITLE>");
            if (matcher.find()) {
                final String s = matcher.group(1);
                try {
                    this.httpFile.setNewURL(new URL(s));
                } catch (MalformedURLException e) {
                    throw new URLNotAvailableAnymoreException("Invalid URL");
                }
                this.httpFile.setPluginID("");
                this.httpFile.setState(DownloadState.QUEUED);
            } else {
                checkProblems();
                throw new PluginImplementationException();
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("was not found")) {
            throw new URLNotAvailableAnymoreException("The page you requested was not found in our database.");
        }
    }

}
