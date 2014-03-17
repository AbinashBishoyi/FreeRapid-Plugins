package cz.vity.freerapid.plugins.services.webshots;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import cz.vity.freerapid.utilities.LogUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 */
class WebshotsFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(WebshotsFileRunner.class.getName());

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL);


        if (fileURL.contains("/photo/")) {
            if (!makeRedirectedRequest(method)) {
                throw new ServiceConnectionProblemException();
            }
            checkProblems();
            checkNameAndSize();
            HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL).setActionFromAHrefWhereATagContains("Full screen").toGetMethod();
            if (!makeRequest(httpMethod)) {
                checkProblems();
                throw new PluginImplementationException();
            }
            httpMethod = getMethodBuilder().setReferer(fileURL).setActionFromTextBetween("('source', '", "');").toGetMethod();
            if (!tryDownloadAndSaveFile(httpMethod)) {
                checkProblems();
                logger.warning(getContentAsString());
                throw new IOException("File input stream is empty");
            }


        } else if (fileURL.contains("/album/")) {
            if (!makeRedirectedRequest(method)) {
                throw new ServiceConnectionProblemException();
            }
            checkProblems();
            final int numPhotos = PlugUtils.getNumberBetween(getContentAsString(), "<li>Photos: <strong>", "</strong></li>");
            int i = 0;
            HttpMethod httpMethod;
            while (i < numPhotos) { //several pages of pictures are supported
                httpMethod = getMethodBuilder().setReferer(fileURL).setAction(fileURL).setParameter("start", Integer.toString(i)).toGetMethod();
                if (!makeRequest(httpMethod)) {
                    checkProblems();
                    throw new PluginImplementationException();
                }
                parseWebsite();
                i += 28;
            }


        } else {
            throw new PluginImplementationException("Couldn't determine content type");
        }
    }

    private void parseWebsite() {
        final Matcher matcher = getMatcherAgainstContent("<h5>\\s+?<a href=\"(http://.+?)\">");
        int start = 0;
        final List<URI> uriList = new LinkedList<URI>();
        while (matcher.find(start)) {
            String link = matcher.group(1);
            try {
                uriList.add(new URI(link));
            } catch (URISyntaxException e) {
                LogUtils.processException(logger, e);
            }
            start = matcher.end();
        }
        getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, uriList);
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        String fn = PlugUtils.getStringBetween(getContentAsString(), "<title>", "pictures from");
        if (fn.equals(" ")) fn = "unnamed";
        this.httpFile.setFileName(fn + ".jpg");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("This page has moved")
                || contentAsString.contains("Not Found")
                || contentAsString.contains("page you have requested has either moved")
                || contentAsString.contains("not available right now")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

}