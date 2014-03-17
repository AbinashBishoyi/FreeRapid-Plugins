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
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 */
class MultiUploadFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(MultiUploadFileRunner.class.getName());
    private final Map<String, String> serviceShortName = new HashMap<String, String>();
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
        prepareMap();
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
        if (content.contains("Please select file") || content.contains("<h1>Not Found</h1>")) {
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

    private void prepareMap() {
        serviceShortName.put("RapidShare.com", "RS");
        serviceShortName.put("MegaUpload.com", "MU");
        serviceShortName.put("HotFile.com", "HF");
        serviceShortName.put("DepositFiles.com", "DF");
        serviceShortName.put("zShare.net", "ZS");
        serviceShortName.put("Badongo.com", "BD");
        serviceShortName.put("Uploading.com", "UP");
        serviceShortName.put("2shared.com", "2S");
    }

    private boolean checkService(final String service) throws Exception {
        final Matcher matcher = getMatcherAgainstContent(">(http://www\\.multiupload\\.com/" + serviceShortName.get(service) + "_.+?)<");
        if (!matcher.find())
            return false;

        /*
         * TODO
         * This should also check if the file exists on the download server, but I don't know how to implement it properly.
         */

        final HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL).setAction(matcher.group(1)).toGetMethod();
        if (client.makeRequest(httpMethod, false) / 100 != 3)
            throw new ServiceConnectionProblemException("Problem with redirection");
        final Header locationHeader = httpMethod.getResponseHeader("Location");
        if (locationHeader == null)
            throw new ServiceConnectionProblemException("Could not find redirect location");
        final String url = locationHeader.getValue();

        logger.info("New URL: " + url);
        httpFile.setNewURL(new URL(url));
        httpFile.setPluginID("");
        httpFile.setState(DownloadState.QUEUED);
        return true;
    }

}