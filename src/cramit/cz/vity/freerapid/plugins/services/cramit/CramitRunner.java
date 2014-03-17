package cz.vity.freerapid.plugins.services.cramit;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.io.IOException;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author RickCL
 */
public class CramitRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(CramitRunner.class.getName());

    @Override
    public void run() throws Exception {
        super.run();

        logger.info("Starting run task " + fileURL);
        final GetMethod getMethod = getGetMethod(fileURL);

        if (makeRedirectedRequest(getMethod)) {
            final HttpMethod httpMethod = getMethodBuilder().setActionFromFormWhereTagContains("method_free", true).setAction(fileURL).toPostMethod();
            if (!makeRedirectedRequest(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }

            Matcher matcher = getMatcherAgainstContent("Wait.*\">(\\d*)</span>");
            if (!matcher.find()) {
                throw new PluginImplementationException("Waiting time not found");
            }

            downloadTask.sleep(Integer.parseInt(matcher.group(1)) + 1);

            final HttpMethod httpMethod2 = getMethodBuilder().setActionFromFormWhereTagContains("method_free", true).setAction(fileURL).toPostMethod();
            if (!makeRedirectedRequest(httpMethod2)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }

            final HttpMethod httpMethod3 = getMethodBuilder().setActionFromAHrefWhereATagContains("<h2>Click Here to Download</h2>").toGetMethod();
            if (!tryDownloadAndSaveFile(httpMethod3)) {
                checkProblems();
                logger.warning(getContentAsString());
                throw new IOException("File input stream is empty");
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    @Override
    public void runCheck() throws Exception {
        super.runCheck();

        final HttpMethod httpMethod = getMethodBuilder().setAction(fileURL).toHttpMethod();

        if (makeRedirectedRequest(httpMethod)) {
            checkProblems();
            checkNameAndSize();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize() throws Exception {
        final String content = getContentAsString();
        PlugUtils.checkName(httpFile, content, "<h2>Download File ", "</h2>");
        PlugUtils.checkFileSize(httpFile, content, "</font> (", ")</font>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        if (content.contains("<h2>File Not Found</h2>") || content.contains("No such file with this filename")) {
            throw new URLNotAvailableAnymoreException("The requested file was not found");
        }
    }

}
