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
    private String CONTENT_TYPE, FILE_EXTENSION;

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL);


        if (fileURL.contains("/photo/")) {
            CONTENT_TYPE = "picture";
            FILE_EXTENSION = ".jpg";
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


        } else if (fileURL.contains("/video/")) {
            CONTENT_TYPE = "video";
            FILE_EXTENSION = ".flv";
            if (!makeRedirectedRequest(method)) {
                throw new ServiceConnectionProblemException();
            }
            checkProblems();
            checkNameAndSize();
            final HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL).setActionFromTextBetween("writeFlashVideo(\"", "\"").toGetMethod();
            client.getHTTPClient().getParams().setParameter("considerAsStream", "text/plain; charset=ISO-8859-1");
            if (!tryDownloadAndSaveFile(httpMethod)) {
                checkProblems();
                logger.warning(getContentAsString());
                throw new IOException("File input stream is empty");
            }


        } else if (fileURL.contains("/album/")) {
/* found a more effective method
            if (!makeRedirectedRequest(method)) {
                throw new ServiceConnectionProblemException();
            }
            checkProblems();
            try {
                this.httpFile.setFileName("Album: " + PlugUtils.getStringBetween(getContentAsString(), "<title>", "pictures from"));
            } catch (PluginImplementationException e) {
                logger.info("Title not found");
            }
            final int numPhotos = PlugUtils.getNumberBetween(getContentAsString(), "<li>Photos: <strong>", "</strong></li>");
            int i = 0;
            HttpMethod httpMethod;
            while (i < numPhotos) { //several pages are supported
                httpMethod = getMethodBuilder().setReferer(fileURL).setAction(fileURL).setParameter("start", Integer.toString(i)).toGetMethod();
                if (!makeRequest(httpMethod)) {
                    checkProblems();
                    throw new PluginImplementationException();
                }
                parseWebsite("<h5>\\s+?<a href=\"(http://.+?)\"");
                i += 28;
            }
*/
            //http://rides.webshots.com/album/574558879SovaQH --> http://rides.webshots.com/slideshow/meta/574558879SovaQH
            final String link = fileURL.replace("/album/", "/slideshow/meta/");
            final HttpMethod httpMethod = getMethodBuilder().setAction(link).toGetMethod();
            if (!makeRedirectedRequest(httpMethod)) {
                checkProblems();
                throw new PluginImplementationException();
            }
            try {
                this.httpFile.setFileName("Album: " + PlugUtils.getStringBetween(getContentAsString(), "<name>", "</name>"));
            } catch (PluginImplementationException e) {
                logger.info("Title not found");
            }
            parseWebsite("<item src=\"http://.+?\" href=\"(http://.+?)\"");


        } else if (fileURL.contains("/user/")) {
            if (!makeRedirectedRequest(method)) {
                throw new ServiceConnectionProblemException();
            }
            checkProblems();
            try {
                this.httpFile.setFileName("User: " + PlugUtils.getStringBetween(getContentAsString(), "<title>", "&#039;s photos"));
            } catch (PluginImplementationException e) {
                logger.info("Title not found");
            }
            final int numPhotos = PlugUtils.getNumberBetween(getContentAsString(), "My Albums (", ")");
            int i = 0;
            HttpMethod httpMethod;
            while (i < numPhotos) { //several pages are supported
                httpMethod = getMethodBuilder().setReferer(fileURL).setAction(fileURL).setParameter("start", Integer.toString(i)).toGetMethod();
                if (!makeRequest(httpMethod)) {
                    checkProblems();
                    throw new PluginImplementationException();
                }
                parseWebsite("<h4>\\s+?<a href=\"(http://.+?)\"");
                i += 42;
            }


        } else if (fileURL.contains("/slideshow/")) {
            //http://community.webshots.com/slideshow/575465878TZfRwO --> http://community.webshots.com/slideshow/meta/575465878TZfRwO
            String link = fileURL;
            if (!fileURL.contains("/meta/")) {
                link = fileURL.replace("/slideshow/", "/slideshow/meta/");
            }
            final HttpMethod httpMethod = getMethodBuilder().setAction(link).toGetMethod();
            if (!makeRedirectedRequest(httpMethod)) {
                checkProblems();
                throw new PluginImplementationException();
            }
            try {
                this.httpFile.setFileName("Slideshow: " + PlugUtils.getStringBetween(getContentAsString(), "<name>", "</name>"));
            } catch (PluginImplementationException e) {
                logger.info("Title not found");
            }
            parseWebsite("<item src=\"http://.+?\" href=\"(http://.+?)\"");


        } else {
            throw new PluginImplementationException("Could not determine content type");
        }
    }

    private void parseWebsite(final String regexp) {
        final Matcher matcher = getMatcherAgainstContent(regexp);
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
        String fn = PlugUtils.getStringBetween(getContentAsString(), "<title>", CONTENT_TYPE + "s from");
        if (fn == null || fn.equals(" ") || fn.equals("")) fn = "unnamed";
        this.httpFile.setFileName(fn + FILE_EXTENSION);
        this.httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("This page has moved")
                || contentAsString.contains("Not Found")
                || contentAsString.contains("page you have requested has either moved")
                || contentAsString.contains("not available right now")
                || contentAsString.contains("An error has occurred")
                || contentAsString.contains("We can't find that Webshots page")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

}