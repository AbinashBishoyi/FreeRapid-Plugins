package cz.vity.freerapid.plugins.services.brontofile;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.exceptions.YouHaveToWaitException;
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
class BrontoFileFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(BrontoFileFileRunner.class.getName());
    private final static String SERVICE_COOKIE_DOMAIN = ".brontofile.com";

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        addCookie(new Cookie(SERVICE_COOKIE_DOMAIN, "mfh_mylang", "en", "/", 86400, false));
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
        PlugUtils.checkName(httpFile, content, "<h2 class=\"float-left\">", "</h2>");
        PlugUtils.checkFileSize(httpFile, content, "<li class=\"col-w50\">", "</li>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        addCookie(new Cookie(SERVICE_COOKIE_DOMAIN, "mfh_mylang", "en", "/", 86400, false));
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            final String contentAsString = getContentAsString();
            checkProblems();
            isAtHomepage(method);
            checkNameAndSize(contentAsString);

            HttpMethod httpMethod;
            String referer = fileURL;
            if (!getContentAsString().contains("entryform1")) { //skip this if "entryform1" found
                httpMethod = getMethodBuilder()
                        .setReferer(referer)
                        .setActionFromAHrefWhereATagContains("Download")
                        .toHttpMethod();
                if (!makeRedirectedRequest(httpMethod)) {
                    checkProblems();
                    throw new ServiceConnectionProblemException();
                }
                checkProblems();
                referer = httpMethod.getURI().toString();
            }

            httpMethod = getMethodBuilder()
                    .setReferer(referer)
                    .setActionFromFormByName("entryform1", true)
                    .toPostMethod();
            if (!makeRedirectedRequest(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            checkProblems();

            final int waitTime = PlugUtils.getNumberBetween(getContentAsString(), "var timeout='", "'");
            downloadTask.sleep(waitTime + 1);
            httpMethod = getGetMethod(PlugUtils.getStringBetween(getContentAsString(), "document.location='", "';"));
            if (!tryDownloadAndSaveFile(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException("Error starting download");
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void isAtHomepage(HttpMethod method) throws Exception {
        final String pageURL = method.getURI().toString();
        if (pageURL.matches("http://(www\\.)?brontofile\\.com/?")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("file is not found")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        if (contentAsString.contains("AccessKey is expired")) {
            throw new YouHaveToWaitException("AccessKey is expired", 10);
        }
    }

}