package cz.vity.freerapid.plugins.services.cramit;

import java.io.IOException;
import java.util.logging.Logger;
import java.util.regex.Matcher;

import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.exceptions.YouHaveToWaitException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;

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
        logger.info(fileURL);

        if (makeRequest(getMethod)) {
            final HttpMethod httpMethod = getMethodBuilder().setActionFromFormWhereTagContains("method_free", true).setAction(fileURL).toPostMethod();
            if (!makeRedirectedRequest(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }

            Matcher matcher = getMatcherAgainstContent("Wait.*\">(\\d*)</span>");
            if( !matcher.find() ) {
                throw new ServiceConnectionProblemException();
            }

            downloadTask.sleep( Integer.parseInt( matcher.group(1) ) + 1 );

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
            throw new ServiceConnectionProblemException();
        }
    }

    @Override
    public void runCheck() throws Exception {
        super.runCheck();

        final HttpMethod httpMethod = getMethodBuilder().setAction(fileURL).toHttpMethod();

        if (makeRequest(httpMethod)) {
            checkProblems();
            checkNameAndSize();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize() throws Exception {
        httpFile.setFileName( fileURL.substring(fileURL.lastIndexOf('/') + 1) );

        Matcher matcher = getMatcherAgainstContent("You have requested.*\\(([\\d\\.\\s\\w]*)\\)");
        if( !matcher.find() ) {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
        httpFile.setFileSize(PlugUtils.getFileSizeFromString( matcher.group(1)) );

        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private void checkProblems() throws ServiceConnectionProblemException, YouHaveToWaitException,
            URLNotAvailableAnymoreException {
        final String content = getContentAsString();
        if ( content.contains("No such file with this filename") ) {
            throw new URLNotAvailableAnymoreException("The requested file was not found");
        }
    }

}
