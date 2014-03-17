package cz.vity.freerapid.plugins.services.soundcloud;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;

import java.util.logging.Logger;

/**
 * Class which contains main code
 *
 * @author Vity
 * @author ntoskrnl
 * @author Abinash Bishoyi
 */
class SoundcloudFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(SoundcloudFileRunner.class.getName());
    private final static String CLIENT_ID = "b45b1aa10f1ac2941910a7f0d10f8e28";

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        HttpMethod method = getGetMethod("https://api.sndcdn.com/resolve?url=" + fileURL.replace(":", "%3A") + "&_status_format=json&client_id=" + CLIENT_ID);
        if (!makeRedirectedRequest(method)) {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
        checkNameAndSize();
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        final boolean json = isJson();
        final String name;
        if (json) {
            name = PlugUtils.unescapeUnicode(PlugUtils.getStringBetween(getContentAsString(), "\"title\"", "\","));
        } else {
            name = PlugUtils.unescapeHtml(PlugUtils.getStringBetween(getContentAsString(), "<title>", "</title>"));
        }
        httpFile.setFileName(name + ".mp3");
        if (isDownloadable()) {
            if (json) {
                PlugUtils.checkFileSize(httpFile, getContentAsString(), "\"original_content_size\":", ",");
            } else {
                PlugUtils.checkFileSize(httpFile, getContentAsString(), "<original-content-size type=\"integer\">", "</original-content-size>");
            }
        } else {
            httpFile.setFileSize(0);
        }
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private boolean isJson() {
        return getContentAsString().contains("{\"");
    }

    private boolean isDownloadable() {
        return getContentAsString().contains("\"downloadable\":true")
                || getContentAsString().contains("<downloadable type=\"boolean\">true</downloadable>");
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        runCheck();
        final boolean json = isJson();
        final String url;
        if (isDownloadable()) {
            if (json) {
                url = PlugUtils.getStringBetween(getContentAsString(), "\"download_url\":\"", "\"");
            } else {
                url = PlugUtils.getStringBetween(getContentAsString(), "<download-url>", "</download-url>");
            }
        } else {
            if (json) {
                url = PlugUtils.getStringBetween(getContentAsString(), "\"stream_url\":\"", "\"");
            } else {
                url = PlugUtils.getStringBetween(getContentAsString(), "<stream-url>", "</stream-url>");
            }
        }
        HttpMethod method = getMethodBuilder()
                .setReferer(fileURL)
                .setAction(url.replace("soundcloud.com", "sndcdn.com"))
                .setParameter("client_id", CLIENT_ID)
                .toGetMethod();
        if (!tryDownloadAndSaveFile(method)) {
            checkProblems();
            throw new ServiceConnectionProblemException("Error starting download");
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        if (getContentAsString().contains("404 - Not Found")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

}