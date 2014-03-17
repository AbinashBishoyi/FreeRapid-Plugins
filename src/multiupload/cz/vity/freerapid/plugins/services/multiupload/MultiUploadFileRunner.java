package cz.vity.freerapid.plugins.services.multiupload;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.net.URL;
import java.util.HashMap;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 */
class MultiUploadFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(MultiUploadFileRunner.class.getName());
    private final HashMap<String, String> serviceShortName = new HashMap<String, String>();
    private final HashMap<String, String[]> serviceErrorMessages = new HashMap<String, String[]>();
    private MultiUploadSettingsConfig config;

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
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
        setConfig();
        prepareMaps();
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize();
            for (final String service : config.getServices()) {
                if (checkService(service)) return;
            }
            throw new URLNotAvailableAnymoreException("File not available anymore; All links expired");
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

    private void prepareMaps() {
        serviceShortName.put("RapidShare.com", "RS");
        serviceShortName.put("MegaUpload.com", "MU");
        serviceShortName.put("HotFile.com", "HF");
        serviceShortName.put("DepositFiles.com", "DF");
        serviceShortName.put("zShare.net", "ZS");
        serviceShortName.put("Badongo.com", "BD");
        serviceShortName.put("Uploading.com", "UP");
        serviceShortName.put("2shared.com", "2S");

        serviceErrorMessages.put("RapidShare.com", new String[]{"has removed file", "file could not be found", "illegal content", "file has been removed", "limit is reached", "Currently a lot of users", "no more download slots", "our servers are overloaded", "You have reached the", "is already", "you either need a Premium Account", "momentarily not available", "This file is larger than"});
        serviceErrorMessages.put("MegaUpload.com", new String[]{"trying to access is temporarily unavailable", "Download limit exceeded", "All download slots", "to download is larger than", "the link you have clicked is not available", "We have detected an elevated number of requests"});
        serviceErrorMessages.put("HotFile.com", new String[]{"404 - Not Found", "File not found", "removed due to copyright", "document.getElementById('dwltmr"});
        serviceErrorMessages.put("DepositFiles.com", new String[]{"file does not exist", "already downloading", "Please try in", "All downloading slots"});
        serviceErrorMessages.put("zShare.net", new String[]{"The file you were looking for could not be found"});
        serviceErrorMessages.put("Badongo.com", new String[]{"This file has been deleted", "File not found", "FREE MEMBER WAITING PERIOD", "You have exceeded your Download Quota"});
        serviceErrorMessages.put("Uploading.com", new String[]{"Requested file not found", "The requested file is not found", "Service Not Available", "Download Limit", "You have reached the daily downloads limit", "Your IP address is currently downloading"});
        serviceErrorMessages.put("2shared.com", new String[]{"The file link that you requested is not valid.", "User downloading session limit is reached."});
    }

    private boolean checkService(final String service) throws Exception {
        final Matcher matcher = getMatcherAgainstContent(">(http://www\\.multiupload\\.com/" + serviceShortName.get(service) + "_.+?)<");
        if (!matcher.find())
            return false;

        final HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL).setAction(matcher.group(1)).toGetMethod();
        if (client.makeRequest(httpMethod, false) / 100 != 3)
            throw new ServiceConnectionProblemException("Problem with redirection");
        final Header locationHeader = httpMethod.getResponseHeader("Location");
        if (locationHeader == null)
            throw new ServiceConnectionProblemException("Could not find redirect location");
        final String url = locationHeader.getValue();
        if (url.equals("http://www.multiupload.com/"))
            return false;

        if (config.getCheckDownloadService()) {
            logger.info("Checking for errors on download service");
            if (!checkDownloadService(service, url))
                return false;
        } else {
            logger.info("Skipping check of download service");
        }

        logger.info("New URL: " + url);
        httpFile.setNewURL(new URL(url));
        httpFile.setPluginID("");
        httpFile.setState(DownloadState.QUEUED);
        return true;
    }

    private boolean checkDownloadService(final String service, final String url) throws Exception {
        if (!makeRedirectedRequest(getGetMethod(url)))
            return false;
        final String content = getContentAsString();

        for (final String errorMessage : serviceErrorMessages.get(service)) {
            if (content.contains(errorMessage))
                return false;
        }
        return true;
    }

}