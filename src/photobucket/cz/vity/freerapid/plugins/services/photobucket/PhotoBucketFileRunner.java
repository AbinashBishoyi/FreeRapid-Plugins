package cz.vity.freerapid.plugins.services.photobucket;

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
class PhotoBucketFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(PhotoBucketFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize();
        } else {
            checkProblems();
            throw new PluginImplementationException();
        }
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("Share this album")) {
            httpFile.setFileName("Album: " + PlugUtils.getStringBetween(contentAsString, "<title>", "- Photobucket"));

        } else {
            final Matcher name = getMatcherAgainstContent("\"name\":\"(.+?)\",");
            if (name.find()) {
                final String filename = name.group(1);
                logger.info("File name " + filename);
                httpFile.setFileName(filename);
            } else {
                logger.warning("File name not found");
                throw new PluginImplementationException("File name not found");
            }

            final Matcher size = getMatcherAgainstContent("File Size: (.+?)(?: -.*?)?</p>");
            if (size.find()) {
                final String filesize = size.group(1);
                logger.info("File size " + filesize);
                if (!filesize.contains("unknown")) {
                    httpFile.setFileSize(PlugUtils.getFileSizeFromString(filesize));
                }
            } else {
                logger.warning("File size not found");
                //throw new PluginImplementationException("File size not found");
            }
        }

        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("Page not found")
                || contentAsString.contains("Image not found")
                || contentAsString.contains("The action that you were trying to")
                || contentAsString.contains("The specified image does not exist")
                || contentAsString.contains("Logging into album")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize();
            String contentAsString = getContentAsString();

            if (contentAsString.contains("Share this video")) { //video
                final HttpMethod httpMethod = getMethodBuilder().setActionFromTextBetween("player.swf?file=", "\"").toGetMethod();
                client.getHTTPClient().getParams().setParameter("considerAsStream", "text/plain");
                if (!tryDownloadAndSaveFile(httpMethod)) {
                    checkProblems();
                    throw new PluginImplementationException();
                }

            } else if (contentAsString.contains("Share this album")) { //album
                final HttpMethod httpMethod = getMethodBuilder().setAction(fileURL).setParameter("start", "all").toGetMethod();
                if (!makeRedirectedRequest(httpMethod)) {
                    checkProblems();
                    throw new PluginImplementationException();
                }
                parseWebsite("<a href=\"(http://.+?)\" onclick=\"tr\\('album_thumb_click'\\);\">");

            } else { //image
                final HttpMethod httpMethod = getGetMethod(PlugUtils.getStringBetween(getContentAsString(), "downloadUrl\":\"", "\",").replaceAll("\\\\/", "/"));
                if (!tryDownloadAndSaveFile(httpMethod)) {
                    checkProblems();
                    throw new PluginImplementationException();
                }
            }

        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void parseWebsite(final String regexp) throws Exception {
        final Matcher matcher = getMatcherAgainstContent(regexp);
        int start = 0;
        final List<URI> uriList = new LinkedList<URI>();
        while (matcher.find(start)) {
            final String link = PlugUtils.replaceEntities(matcher.group(1));
            try {
                uriList.add(new URI(link));
            } catch (URISyntaxException e) {
                LogUtils.processException(logger, e);
            }
            start = matcher.end();
        }
        getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, uriList);
    }

}