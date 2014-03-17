package cz.vity.freerapid.plugins.services.multiupload;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
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
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 */
class MultiUploadFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(MultiUploadFileRunner.class.getName());
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
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize();
            for (final String service : config.getServices()) {
                final String url = checkService(service);
                if (url != null) {
                    httpFile.setNewURL(new URL(url));
                    httpFile.setPluginID("");
                    httpFile.setState(DownloadState.QUEUED);
                }
            }
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

    private String checkService(final String service) throws Exception {
        final Matcher matcher = getMatcherAgainstContent(">(http://www\\.multiupload\\.com/" + getServiceShortName(service) + "_.+?)<");
        if (!matcher.find())
            return null;

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

        logger.info("URL OK: " + url);
        return url;
    }

    private String getServiceShortName(final String service) throws PluginImplementationException {
        if (service.equals("RapidShare.com")) return "RS";
        else if (service.equals("MegaUpload.com")) return "MU";
        else if (service.equals("HotFile.com")) return "HF";
        else if (service.equals("DepositFiles.com")) return "DF";
        else if (service.equals("zShare.net")) return "ZS";
        else if (service.equals("Badongo.com")) return "BD";
        else if (service.equals("Uploading.com")) return "UP";
        else if (service.equals("2shared.com")) return "2S";
        else throw new PluginImplementationException("Unknown service name");
    }

}