package cz.vity.freerapid.plugins.services.kewegofr;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.Random;
import java.util.logging.Logger;

/**
 * Class which contains main code
 *
 * @author tong2shot
 */
class KewegoFrFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(KewegoFrFileRunner.class.getName());
    private final Random random = new Random();

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize(getContentAsString());
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, content, "<h2><p><span>", "</span></p></h2>");
        httpFile.setFileName(httpFile.getFileName() + ".mp4");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            final String contentAsString = getContentAsString();
            checkProblems();
            checkNameAndSize(contentAsString);
            final String playerKey = PlugUtils.getStringBetween(getContentAsString(), "playerKey=", "&");
            final String sig = PlugUtils.getStringBetween(getContentAsString(), "sig=", "&");
            HttpMethod httpMethod = getMethodBuilder()
                    .setReferer(fileURL)
                    .setAction("http://api.kewego.com/config/getStreamInit/")
                    .setParameter("player_type", "kp")
                    .setParameter("playerKey", playerKey)
                    .setParameter("request_verbose", "false")
                    .setParameter("language_code", "fr")
                    .setParameter("sig", sig)
                    .toPostMethod();
            if (!makeRedirectedRequest(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            checkProblems();
            final String appToken = PlugUtils.getStringBetween(getContentAsString(), "<playerAppToken>", "</playerAppToken>");
            final String format = PlugUtils.getStringBetween(getContentAsString(), "<default_format>", "</default_format>");
            final String v = String.valueOf(random.nextInt(10000) + 9000);
            httpMethod = getMethodBuilder()
                    .setReferer(fileURL)
                    .setAction("http://api.kewego.com/video/getStream/")
                    .setParameter("appToken", appToken)
                    .setParameter("sig", sig)
                    .setParameter("format", format)
                    .setParameter("v", v)
                    .toGetMethod();
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
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("pas trouvé de vidéo correspondant à votre requête")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

}