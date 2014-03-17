package cz.vity.freerapid.plugins.services.partagefacile;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadClientConsts;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;

/**
 * Class which contains main code
 *
 * @author tong2shot
 */
class PartageFacileFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(PartageFacileFileRunner.class.getName());

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
        String fileName;
        try {
            fileName = PlugUtils.getStringBetween(content, "Un commentaire pour :", "</h4>").trim();
        } catch (PluginImplementationException e) {
            fileName = PlugUtils.getStringBetween(content, "Nom :</th>", "</td>").replace("<td>", "").trim();
        }
        final String fileSize = PlugUtils.getStringBetween(content, "Taille :</th>", "</td>").replace("<td>", "").replace("o", "b").trim();
        httpFile.setFileName(fileName);
        httpFile.setFileSize(PlugUtils.getFileSizeFromString(fileSize));
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
            HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL).setActionFromFormWhereTagContains("freedl", true).setAction(fileURL).toPostMethod();
            if (!makeRedirectedRequest(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            checkProblems();
            httpMethod = getMethodBuilder().setReferer(fileURL).setActionFromFormWhereActionContains("dl-view", true).toPostMethod();
            setClientParameter(DownloadClientConsts.DONT_USE_HEADER_FILENAME, true); //server adds ";" at the end of filename
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
        if (contentAsString.contains("meta http-equiv=\"refresh\" content=\"0;url=http://www.partage-facile.com\"")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

}