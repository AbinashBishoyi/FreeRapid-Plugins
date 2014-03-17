package cz.vity.freerapid.plugins.services.metadivx;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
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
class MetadivxFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(MetadivxFileRunner.class.getName());


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
        try {
            Matcher matcher = getMatcherAgainstContent("\\((\\d*) bytes\\)");
            if( matcher.find() ) {
                httpFile.setFileSize( Long.parseLong(matcher.group(1)) );
            } else {
                PlugUtils.checkFileSize(httpFile, content, "Video Size:", "<small>");
            }
            PlugUtils.checkName(httpFile, content, "Filename: <b>", "</b>");
            httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
        } catch(PluginImplementationException e) {
            checkProblems();
        }
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL); //create GET request
        if( !makeRedirectedRequest(method)) { //we make the main request
            checkProblems();
        }

        final HttpMethod httpMethod = getMethodBuilder().setActionFromFormByName("myForm", true).setAction(fileURL).toPostMethod();
        if( !makeRedirectedRequest(httpMethod)) { //we make the main request
            checkProblems();
        }

        final HttpMethod httpMethod2 = getMethodBuilder().setActionFromFormByName("F1", true).setAction(fileURL).toPostMethod();
        if( !makeRedirectedRequest(httpMethod2)) { //we make the main request
            checkProblems();
        }

        final HttpMethod downloadMethod = getMethodBuilder().setActionFromAHrefWhereATagContains("Download").toGetMethod();
        if (!tryDownloadAndSaveFile(downloadMethod)) {
            checkProblems();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        Matcher matcher = getMatcherAgainstContent("<font.*class=\"err\">([^<]*)");
        if( matcher.find() ) {
            throw new ServiceConnectionProblemException( matcher.group(1) );
        }
        throw new ServiceConnectionProblemException();
    }

}
