package cz.vity.freerapid.plugins.services.multiupload;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import cz.vity.freerapid.utilities.LogUtils;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 */
class MultiUploadFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(MultiUploadFileRunner.class.getName());
    private final String[][] serviceErrorMessages = new String[9][];
    private MultiUploadSettingsConfig config;

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        if (PlugUtils.find("/.._", fileURL)) {
            httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
            return;
        }
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod getMethod = getGetMethod(fileURL);
        if (PlugUtils.find("/.._", fileURL)) {
            makeRequest(getMethod);
            if (getMethod.getStatusCode() / 100 != 3) {
                throw new PluginImplementationException("Invalid redirect");
            }
            final Header locationHeader = getMethod.getResponseHeader("Location");
            if (locationHeader == null) {
                throw new PluginImplementationException("Invalid redirect");
            }
            final String s = locationHeader.getValue();
            if (s.equals("http://www.multiupload.com/")) {
                throw new URLNotAvailableAnymoreException("File not found");
            }
            this.httpFile.setNewURL(new URL(s));
            this.httpFile.setPluginID("");
            this.httpFile.setState(DownloadState.QUEUED);
            return;
        }
        setConfig();
        prepareErrorMessages();
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize();
            final List<URL> urlList = getMirrors();
            if (urlList.isEmpty()) throw new URLNotAvailableAnymoreException("No available working mirrors");
            getPluginService().getPluginContext().getQueueSupport().addLinkToQueueUsingPriority(httpFile, urlList);
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        if (content.contains("the link you have clicked is not available") || content.contains("Please select file") || content.contains("<h1>Not Found</h1>")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        PlugUtils.checkName(httpFile, content, "color:#000000;\">", "<font");
        PlugUtils.checkFileSize(httpFile, content, ">(", ")<");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private void setConfig() throws Exception {
        MultiUploadServiceImpl service = (MultiUploadServiceImpl) getPluginService();
        config = service.getConfig();
    }

    private void prepareErrorMessages() {
        serviceErrorMessages[0] = /* RapidShare.com */ new String[]{"has removed file", "file could not be found", "illegal content", "file has been removed", "limit is reached", "Currently a lot of users", "no more download slots", "our servers are overloaded", "You have reached the", "is already", "you either need a Premium Account", "momentarily not available", "This file is larger than"};
        serviceErrorMessages[1] = /* MegaUpload.com */ new String[]{"trying to access is temporarily unavailable", "Download limit exceeded", "All download slots", "to download is larger than", "the link you have clicked is not available", "We have detected an elevated number of requests"};
        serviceErrorMessages[2] = /* HotFile.com */ new String[]{"404 - Not Found", "File not found", "removed due to copyright", "document.getElementById('dwltmr"};
        serviceErrorMessages[3] = /* DepositFiles.com */ new String[]{"file does not exist", "already downloading", "Please try in", "All downloading slots"};
        serviceErrorMessages[4] = /* zShare.net */ new String[]{"The file you were looking for could not be found"};
        serviceErrorMessages[5] = /* Badongo.com */ new String[]{"This file has been deleted", "File not found", "You have exceeded your Download Quota"};
        serviceErrorMessages[6] = /* Uploading.com */ new String[]{"Requested file not found", "The requested file is not found", "Service Not Available", "Download Limit", "You have reached the daily downloads limit", "Your IP address is currently downloading"};
        serviceErrorMessages[7] = /* SharingMatrix.com */ new String[]{"File not found", "File has been deleted", "no available free download slots left for your country"};
        serviceErrorMessages[8] = /* 2shared.com */ new String[]{"The file link that you requested is not valid.", "User downloading session limit is reached."};
    }

    private List<URL> getMirrors() throws Exception {
        final Matcher matcher = getMatcherAgainstContent(">(http://www\\.multiupload\\.com/.._.+?)<");
        final List<URL> urlList = new LinkedList<URL>();
        while (matcher.find()) {
            final HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL).setAction(matcher.group(1)).toGetMethod();
            if (client.makeRequest(httpMethod, false) / 100 != 3)
                throw new ServiceConnectionProblemException("Problem with redirection");
            final Header locationHeader = httpMethod.getResponseHeader("Location");
            if (locationHeader == null)
                throw new ServiceConnectionProblemException("Could not find redirect location");
            final String url = locationHeader.getValue();
            if (!url.equals("http://www.multiupload.com/") && checkDownloadService(url)) {
                try {
                    urlList.add(new URL(url));
                } catch (MalformedURLException e) {
                    LogUtils.processException(logger, e);
                }
            }
        }
        return urlList;
    }

    private boolean checkDownloadService(final String url) throws Exception {
        if (!config.getCheckDownloadService()) {
            logger.info("Skipping check of download service");
            return true;
        }
        logger.info("Checking for errors on download service");

        if (!makeRedirectedRequest(getGetMethod(url)))
            return false;
        final String content = getContentAsString();

        for (final String[] errorMessageArray : serviceErrorMessages) {
            for (final String errorMessage : errorMessageArray) {
                if (content.contains(errorMessage))
                    return false;
            }
        }
        return true;
    }

}