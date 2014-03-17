package cz.vity.freerapid.plugins.services.myuploaddk;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;

/**
 * Class which contains main code
 *
 * @author tong2shot
 */
class MyUploadDkFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(MyUploadDkFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        addCookie(new Cookie(".myupload.dk", "lang", "en", "/", 86400, false));
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            isAtHomepage(getMethod);
            checkNameAndSize(getContentAsString());
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, content, "File name:</td><td class=\"downloadTblRight\">", "</td>");
        PlugUtils.checkFileSize(httpFile, content, "Size:</td><td class=\"downloadTblRight\">", "</td>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        addCookie(new Cookie(".myupload.dk", "lang", "en", "/", 86400, false));
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            final String contentAsString = getContentAsString();
            checkProblems();
            isAtHomepage(method);
            checkNameAndSize(contentAsString);
            final HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL).setActionFromAHrefWhereATagContains("Download now").toGetMethod();

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
        //final String contentAsString = getContentAsString();
    }

    private void isAtHomepage(HttpMethod method) throws Exception {
        final String pageURL = method.getURI().toString();
        if (pageURL.matches("http://(www\\.)?myupload\\.dk/?")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

}