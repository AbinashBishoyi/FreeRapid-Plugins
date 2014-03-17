package cz.vity.freerapid.plugins.services.webshare;

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
 * @author Vity
 */
class WebShareFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(WebShareFileRunner.class.getName());

    final String baseUrl = "https://webshare.cz/";
    final String urlFileInfo = "https://webshare.cz/api/file_info/";
    final String urlFileLink = "https://webshare.cz/api/file_link/";

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        if (fileURL.contains("/#/")) fileURL = fileURL.replace("/#/", "/");
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize(getMethod);
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private String getFileIdent(GetMethod method) throws Exception {
        String urlIdent = method.getURI().getURI();
        urlIdent = urlIdent.substring(urlIdent.indexOf("/file/") + 6);
        if (urlIdent.contains("/"))
            return urlIdent.substring(0, urlIdent.indexOf("/"));
        else
            return urlIdent;
    }

    private void checkNameAndSize(GetMethod method) throws Exception {
        final HttpMethod httpMethod = getMethodBuilder()
                .setReferer(baseUrl)
                .setAction(urlFileInfo)
                .setParameter("ident", getFileIdent(method))
                .setParameter("wst", "")
                .toPostMethod();
        httpMethod.addRequestHeader("X-Requested-With", "XMLHttpRequest");
        if (!makeRedirectedRequest(httpMethod)) {
            throw new ServiceConnectionProblemException("Error retrieving file name/size");
        }
        checkProblems();
        httpFile.setFileName(PlugUtils.getStringBetween(getContentAsString(), "<name>", "</name>"));
        httpFile.setFileSize(PlugUtils.getFileSizeFromString(PlugUtils.getStringBetween(getContentAsString(), "<size>", "</size>")));
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        if (fileURL.contains("/#/")) fileURL = fileURL.replace("/#/", "/");
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize(method);

            final HttpMethod httpMethod = getMethodBuilder()
                    .setReferer(baseUrl)
                    .setAction(urlFileLink)
                    .setParameter("ident", getFileIdent(method))
                    .setParameter("wst", "")
                    .toPostMethod();
            httpMethod.addRequestHeader("X-Requested-With", "XMLHttpRequest");
            if (!makeRedirectedRequest(httpMethod)) {
                throw new ServiceConnectionProblemException("Error retrieving download link");
            }
            checkProblems();
            final String dlUrl = PlugUtils.getStringBetween(getContentAsString(), "<link>", "</link>");

            if (!tryDownloadAndSaveFile(getGetMethod(dlUrl))) {
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
        if (contentAsString.contains("soubor nebyl nalezen") || contentAsString.contains("Soubor nenalezen")
                || contentAsString.contains("File not found")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

}