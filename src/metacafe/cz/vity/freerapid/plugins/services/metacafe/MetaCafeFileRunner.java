package cz.vity.freerapid.plugins.services.metacafe;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import cz.vity.freerapid.utilities.LogUtils;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 * @author tong2shot
 */
class MetaCafeFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(MetaCafeFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        if (isYouTube()) return;
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            isAtHomePage(method);
            if (getContentAsString().contains("mature%20audiences") || method.getURI().toString().contains("/family_filter/")) {
                setFamilyFilterOff();
            }
            checkProblems();
            checkNameAndSize();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, getContentAsString(), "itemTitle\":\"", "\"");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        if (isYouTube()) {
            processYouTube();
            return;
        }
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            isAtHomePage(method);
            if (getContentAsString().contains("mature%20audiences") || method.getURI().toString().contains("/family_filter/")) {
                setFamilyFilterOff();
            }
            checkProblems();
            checkNameAndSize();
            String mediaData = URLDecoder.decode(PlugUtils.getStringBetween(getContentAsString(), "mediaData=", "&"), "UTF-8");
            //prefer HD
            final int index = mediaData.indexOf("highDefinition");
            if (index > -1) {
                logger.info("Grabbing HD version");
                mediaData = "{\"" + mediaData.substring(index + 14);
            }
            final String fileExt = PlugUtils.getStringBetween(mediaData, "{\"", "\"").toLowerCase(Locale.ENGLISH);
            httpFile.setFileName(httpFile.getFileName() + (fileExt.startsWith(".") ? fileExt : "." + fileExt));
            final String mediaURL = URLDecoder.decode(PlugUtils.getStringBetween(mediaData, "\"mediaURL\":\"", "\""), "UTF-8").replace("\\", "");
            final String key = URLDecoder.decode(PlugUtils.getStringBetween(mediaData, "key\":\"__gda__\",\"value\":\"", "\""), "UTF-8");
            final HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL).setAction(mediaURL).setParameter("__gda__", key).toGetMethod();
            setClientParameter("considerAsStream", "text/plain");
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
        if (getContentAsString().contains("the requested page was not found")) {
            //    || !getContentAsString().contains("<object")) { // false positive
            throw new URLNotAvailableAnymoreException("File not found");
        }
        if (getContentAsString().contains("An error occurred while processing your request")) {
            throw new ServiceConnectionProblemException("An error occurred while processing your request");
        }
    }

    private void setFamilyFilterOff() throws Exception {
        //addCookie(new Cookie(".metacafe.com", "ffilter", "false", "/", 86400, false)); //setting cookie doesn't work
        client.setReferer(fileURL);
        addCookie(new Cookie(".metacafe.com", "referrer", fileURL, "/", 86400, false));
        final PostMethod method = getPostMethod("http://www.metacafe.com/f/index.php?inputType=filter&controllerGroup=user");
        method.setParameter("filters", "0");
        if (!makeRedirectedRequest(method)) {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void isAtHomePage(HttpMethod method) throws URIException, URLNotAvailableAnymoreException {
        if (method.getURI().toString().matches("http://(?:www.)?metacafe\\.com(?:/\\?m=removed|/?)")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    private boolean isYouTube() {
        return fileURL.matches("http://(?:www.)metacafe\\.com/watch/yt-.+");
    }

    private void processYouTube() throws Exception {
        final String videoId = PlugUtils.getStringBetween(fileURL, "yt-", "/");
        final List<URI> list = new LinkedList<URI>();
        try {
            list.add(new URI("http://www.youtube.com/watch?v=" + videoId));
        } catch (final URISyntaxException e) {
            LogUtils.processException(logger, e);
        }
        getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, list);
        httpFile.setState(DownloadState.COMPLETED);
        httpFile.getProperties().put("removeCompleted", true);
    }

}