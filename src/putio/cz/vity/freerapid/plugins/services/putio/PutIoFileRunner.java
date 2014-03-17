package cz.vity.freerapid.plugins.services.putio;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import cz.vity.freerapid.utilities.LogUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author tong2shot
 */
class PutIoFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(PutIoFileRunner.class.getName());

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        login();
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            if (isCollection()) {
                parseCollection();
                return;
            }
            final HttpMethod httpMethod = getMethodBuilder()
                    .setReferer(fileURL)
                    .setActionFromAHrefWhereATagContains("Download")
                    .toHttpMethod();
            if (!tryDownloadAndSaveFile(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException("Error starting download");
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("File not found")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    private void login() throws Exception {
        synchronized (PutIoFileRunner.class) {
            PutIoServiceImpl service = (PutIoServiceImpl) getPluginService();
            PremiumAccount pa = service.getConfig();
            if (!pa.isSet()) {
                pa = service.showConfigDialog();
                if (pa == null || !pa.isSet()) {
                    throw new NotRecoverableDownloadException("No Put.io account login information!");
                }
            }
            final HttpMethod method = getMethodBuilder()
                    .setReferer(fileURL)
                    .setAction("/login")
                    .setBaseURL("https://put.io")
                    .setParameter("name", pa.getUsername())
                    .setParameter("password", pa.getPassword())
                    .toPostMethod();
            if (!makeRedirectedRequest(method)) {
                throw new ServiceConnectionProblemException("Error posting login info");
            }
            if (method.getURI().toString().endsWith("err=1")) {
                throw new BadLoginException("Invalid account login information");
            }
        }
    }

    private boolean isCollection() {
        return getContentAsString().contains("var collection");
    }

    private void parseCollection() throws PluginImplementationException {
        final List<URI> uriList = new LinkedList<URI>();
        final Matcher matcher = getMatcherAgainstContent(", \"id\": (\\d+),");
        while (matcher.find()) {
            try {
                final String link = "https://put.io/file/" + matcher.group(1);
                final URI uri = new URI(link);
                if (!uriList.contains(uri)) {
                    uriList.add(uri);
                }
            } catch (final URISyntaxException e) {
                LogUtils.processException(logger, e);
            }
        }
        if (uriList.isEmpty()) {
            throw new PluginImplementationException("No links found");
        }
        getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, uriList);
        httpFile.getProperties().put("removeCompleted", true);
        logger.info(uriList.size() + " links added");
    }

}