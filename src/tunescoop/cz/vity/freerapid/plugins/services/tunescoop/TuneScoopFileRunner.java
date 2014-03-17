package cz.vity.freerapid.plugins.services.tunescoop;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 */
class TuneScoopFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(TuneScoopFileRunner.class.getName());

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

    private void checkNameAndSize() throws Exception {
        final String content = getContentAsString();
        // httpFile.setFileName(getFileName(content));

        PlugUtils.checkName(httpFile, content, "<b>Song Name:</b>", "</font>");

        PlugUtils.checkFileSize(httpFile, content, "<b>Size:</b>", "</font>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private String getFileName(final String content) throws Exception {
        final String fileName = PlugUtils.unescapeHtml(PlugUtils.getStringBetween(content, "<div style=\"font-size:24px\"><b>", "</b></div>").trim());
        final String artist = PlugUtils.unescapeHtml(PlugUtils.getStringBetween(content, "<b>Artist:</b>", "</font>").trim());
        final String song = PlugUtils.unescapeHtml(PlugUtils.getStringBetween(content, "<b>Song Name:</b>", "</font>").trim());

        final int index = fileName.lastIndexOf('.');
        final String ext = index > -1 ? fileName.substring(index) : "";

        final TuneScoopSettingsConfig config = ((TuneScoopServiceImpl) getPluginService()).getConfig();
        if (!config.getIsCustom()) return fileName;
        final String customName = config.getCustomName();

        if ((customName == null || customName.isEmpty()) || (customName.matches("(?i)%ARTIST%") && (artist == null || artist.isEmpty())) || (customName.matches("(?i)%SONG%") && (song == null || song.isEmpty()))) {
            return fileName;
        } else {
            return customName.replaceAll("(?i)%ARTIST%", artist).replaceAll("(?i)%SONG%", song) + ext;
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
            HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL).setBaseURL(fileURL).setActionFromFormByName("dform", true).toHttpMethod();
            if (makeRedirectedRequest(httpMethod)) {
                fileURL = PlugUtils.getStringBetween(getContentAsString(), "report?url=", "\">");

                httpMethod = getMethodBuilder().setReferer(fileURL).setActionFromFormByName("dform", true).toHttpMethod();


                if (!tryDownloadAndSaveFile(httpMethod)) {
                    checkProblems();
                    logger.warning(getContentAsString());
                    throw new ServiceConnectionProblemException("Error starting download");
                }
            } else {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("File Not Found")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

}