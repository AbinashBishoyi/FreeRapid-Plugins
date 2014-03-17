package cz.vity.freerapid.plugins.services.file2box;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author Vity
 */
class File2BoxFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(File2BoxFileRunner.class.getName());


    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);//make first request
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize(getContentAsString());//ok let's extract file name and size from the page
        } else
            throw new PluginImplementationException();
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, content, "Filename:</b></td><td nowrap>", "</td></tr>");
        PlugUtils.checkFileSize(httpFile, content, "<small>(", ")</small>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            final String contentAsString = getContentAsString();//check for response
            checkProblems();//check problems
            checkNameAndSize(contentAsString);//extract file name and size from the page
            final int sleep = PlugUtils.getNumberBetween(getContentAsString(), "countdown\">", "</span>");

            final Matcher matcher = getMatcherAgainstContent("px;'>(\\d)</span>");
            int start = 0;
            StringBuilder builder = new StringBuilder();
            while (matcher.find(start)) {
                builder.append(matcher.group(1));
                start = matcher.end();
            }
            final String captcha = builder.toString();
            if (captcha.isEmpty())
                throw new PluginImplementationException("Captcha not found");
            logger.info("Captcha:" + captcha);
            logger.info("File url:" + fileURL);
            final MethodBuilder methodBuilder = getMethodBuilder().setReferer(fileURL).setActionFromFormByName("F1", true).setAction(fileURL);

            methodBuilder.setParameter("code", captcha);
            methodBuilder.setParameter("referer", fileURL);
            methodBuilder.setParameter("btn_download", "Sending File...");

            final HttpMethod httpMethod = methodBuilder.toPostMethod();

            this.downloadTask.sleep(sleep + 1);

            //here is the download link extraction
            if (!tryDownloadAndSaveFile(httpMethod)) {
                checkProblems();//if downloading failed
                logger.warning(getContentAsString());//log the info
                throw new PluginImplementationException();//some unknown problem
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("not found")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
        if (contentAsString.contains("No such user exist")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
        if (contentAsString.contains("Wrong captcha")) {
            throw new PluginImplementationException("Wrong captcha");
        }
    }

}