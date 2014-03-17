package cz.vity.freerapid.plugins.services.goear;

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
 * @author ntoskrnl
 */
class GoEarFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(GoEarFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            if (method.getURI().toString().endsWith("goear.com/index.php")) {
                throw new URLNotAvailableAnymoreException("File not found");
            }
            checkName();
        } else {
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkName() throws ErrorDuringDownloadingException {
        final String song = PlugUtils.getStringBetween(getContentAsString(), "<span class=\"song_title\">", "</span>");
        final String artist = PlugUtils.getStringBetween(getContentAsString(), "<span class=\"artist_name\">", "</span>");
        if (song.startsWith(artist)) {
            httpFile.setFileName(song + ".mp3");
        } else {
            httpFile.setFileName(artist + " - " + song + ".mp3");
        }
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        runCheck();
        final String soundId = PlugUtils.getStringBetween(getContentAsString(), "var soundid = '", "';");
        HttpMethod method = getGetMethod("http://goear.com/playersong/" + soundId);
        if (makeRedirectedRequest(method)) {
            method = getGetMethod(PlugUtils.getStringBetween(getContentAsString(), "<track href=\"", "\""));
            if (!tryDownloadAndSaveFile(method)) {
                throw new ServiceConnectionProblemException("Error starting download");
            }
        } else {
            throw new ServiceConnectionProblemException();
        }
    }

}