package cz.vity.freerapid.plugins.services.linkdecrypter;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.utilities.LogUtils;
import org.apache.commons.httpclient.HttpMethod;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author Vity
 */
class LinkDecrypterFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(LinkDecrypterFileRunner.class.getName());

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final HttpMethod postMethod1 = getMethodBuilder().setAction("http://linkdecrypter.com").setAndEncodeParameter("links", fileURL).setParameter("modo_links", "link").setParameter("modo_recursivo", "on").setParameter("link_cache", "on").toPostMethod();
        if (makeRedirectedRequest(postMethod1)) { //we make the main request
            logger.info(getContentAsString());
            final HttpMethod getMethod = getMethodBuilder().setAction("http://linkdecrypter.com/?d").toGetMethod();
            if (makeRedirectedRequest(getMethod)) {
                checkProblems();
                final Matcher matcher = getMatcherAgainstContent("_blank\"><b>(.*?)</b>");
                List<URI> uriList = new LinkedList<URI>();
                int start = 0;
                while (matcher.find(start)) {
                    String link = matcher.group(1);
                    try {
                        uriList.add(new URI(link));
                    } catch (URISyntaxException e) {
                        LogUtils.processException(logger, e);
                    }
                    start = matcher.end();
                }
                if (uriList.isEmpty()) {
                    throw new PluginImplementationException("No link detected");
                }
                getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, uriList);
            } else {
                throw new PluginImplementationException("");
            }
        } else {
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("[LINK-ERROR]")) {
            throw new URLNotAvailableAnymoreException("Cannot be unprotected. Link error."); //let to know user in FRD
        }
    }

}