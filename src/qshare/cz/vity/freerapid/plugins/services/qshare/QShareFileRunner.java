package cz.vity.freerapid.plugins.services.qshare;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.InvalidURLOrServiceProblemException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author RickCL
 */
class QShareFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(QShareFileRunner.class.getName());


    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);//make first request
        if (makeRedirectedRequest(getMethod)) {
            checkNameAndSize(getContentAsString());//ok let's extract file name and size from the page
        } else
            checkProblems();
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
            Matcher matcher = getMatcherAgainstContent("\\s([\\d\\w\\.]+)\\s+\\(([\\d\\.]+\\s\\w+)\\)");
            if( matcher.find() ) {
                System.out.println( matcher.group(1) + " " + matcher.group(2));
                httpFile.setFileName( matcher.group(1) );
                httpFile.setFileSize( PlugUtils.getFileSizeFromString(matcher.group(2)) );
                //PlugUtils.checkName(httpFile, content, "Filename: <b>", "</b>");
                httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
            } else {
                checkProblems();
            }
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL); //create GET request
        if( !makeRedirectedRequest(method)) {
            checkProblems();
        }

        final HttpMethod httpMethod = getMethodBuilder().setActionFromAHrefWhereATagContains(" Free ").toGetMethod();
        System.out.println( httpMethod.getURI().toString() );
        if( !makeRedirectedRequest(httpMethod)) {
            checkProblems();
        }

        final HttpMethod downloadMethod = getMethodBuilder().setActionFromAHrefWhereATagContains("Your download link").toGetMethod();
        System.out.println( downloadMethod.getURI().toString() );
        if (!tryDownloadAndSaveFile(downloadMethod)) {
            checkProblems();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        if( getContentAsString().contains("Invalid download link") ) {
            throw new InvalidURLOrServiceProblemException("Invalid download link");
        }
        throw new ServiceConnectionProblemException();
    }

}
