package cz.vity.freerapid.plugins.services.rapidshareuser;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.NotRecoverableDownloadException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.utilities.LogUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;


/**
 * @author Alex
 */
class RapidshareUserRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(RapidshareUserRunner.class.getName());

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting run task " + fileURL);
        final GetMethod method = getGetMethod(fileURL);
        logger.info(fileURL);
        if (makeRedirectedRequest(method)) {
            if (!getContentAsString().contains("View LinkList"))
                throw new NotRecoverableDownloadException("No link list available");
            checkProblems();
            final MethodBuilder builder = getMethodBuilder().setActionFromFormByName("fformular", true);
            final HttpMethod httpMethod = builder.setReferer(fileURL).setParameter("browse", "ID=1").toHttpMethod();
            if (makeRedirectedRequest(httpMethod)) {
                checkProblems();
                final Matcher matcher = getMatcherAgainstContent("target=\"_blank\" href=\"(http://rapidshare.com/files/.+?)\">");
                int start = 0;
                final List<URI> uriList = new ArrayList<URI>();
                while (matcher.find(start)) {
                    final String link = matcher.group(1);
                    try {
                        uriList.add(new URI(link));
                    } catch (URISyntaxException e) {
                        LogUtils.processException(logger, e);
                    }
                    start = matcher.end();
                }
                getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, uriList);
            } else throw new PluginImplementationException();
        } else throw new PluginImplementationException();

    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("LinkList not found.")) {
            throw new URLNotAvailableAnymoreException("LinkList not found.");
        }
    }

    @Override
    protected String getBaseURL() {
        return "http://rapidshare.com/";
    }
}
