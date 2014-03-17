package cz.vity.freerapid.plugins.services.shareflare;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.exceptions.YouHaveToWaitException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethod;

import java.util.logging.Logger;


/**
 * @author RickCL, ntoskrnl
 */
class ShareflareRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(ShareflareRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        addCookie(new Cookie(".shareflare.net", "lang", "en", "/", 86400, false));
        final HttpMethod httpMethod = getMethodBuilder().setAction(fileURL).toGetMethod();
        if (makeRedirectedRequest(httpMethod)) {
            checkProblems();
            checkNameAndSize();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize() throws Exception {
        PlugUtils.checkName(httpFile, getContentAsString(), "<p>File:", "</p>");
        PlugUtils.checkFileSize(httpFile, getContentAsString(), "<p>Size:", "</p>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        addCookie(new Cookie(".shareflare.net", "lang", "en", "/", 86400, false));
        final HttpMethod httpMethod = getMethodBuilder().setAction(fileURL).toGetMethod();
        if (!makeRedirectedRequest(httpMethod)) {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
        checkNameAndSize();

        final HttpMethod httpMethod2 = getMethodBuilder().setReferer(fileURL).setActionFromFormByName("dvifree", true).toPostMethod();
        if (!makeRedirectedRequest(httpMethod2)) {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }

        final MethodBuilder methodBuilder = getMethodBuilder().setReferer(fileURL).setActionFromFormByName("dvifree", true);

        if (!makeRedirectedRequest(methodBuilder.toHttpMethod())) {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }

        final HttpMethod httpMethod3 = getMethodBuilder().setReferer(methodBuilder.getEscapedURI()).setActionFromIFrameSrcWhereTagContains("name=\"topFrame\"").toGetMethod();
        if (!makeRedirectedRequest(httpMethod3)) {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }

        final HttpMethod httpMethod4 = getMethodBuilder()
                .setActionFromAHrefWhereATagContains("Your link to file download")
                .setReferer(methodBuilder.getEscapedURI())
                .toGetMethod();
        if (!tryDownloadAndSaveFile(httpMethod4)) {
            checkProblems();
            throw new ServiceConnectionProblemException("Error starting download");
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        //  May produce false positives, eg. if the filename contains the word "Error".

        if (content.contains("You can wait download for")) {
            int wait = PlugUtils.getNumberBetween(getContentAsString(), "You can wait download for", "minutes");
            throw new YouHaveToWaitException(String.format("You could download your next file in %s minutes", (wait)), (wait * 60 + 1));

        }
        if (content.contains("You are currently downloading..")) {
            throw new ServiceConnectionProblemException(String.format("Your IP address is already downloading a file. Please wait until the download is completed."));
        }
        if (content.contains("File not found") || content.contains("deleted for abuse") || content.contains("<h1>404 Not Found</h1>")) {
            throw new URLNotAvailableAnymoreException("The requested file was not found");
        }
    }

    @Override
    protected String getBaseURL() {
        return "http://shareflare.net";
    }

}