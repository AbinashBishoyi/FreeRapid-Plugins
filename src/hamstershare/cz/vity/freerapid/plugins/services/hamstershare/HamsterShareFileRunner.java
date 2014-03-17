package cz.vity.freerapid.plugins.services.hamstershare;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author Vookimedlo
 */
class HamsterShareFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(HamsterShareFileRunner.class.getName());


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
        PlugUtils.checkName(httpFile, content, "<div id=\"h2\">", "</div>");

        //nice try, but checkFileSize is stupid, since it not handles russian units :)
        //PlugUtils.checkFileSize(httpFile, content, "Размер файла:", "</b>");

        //Let's do it in old fashion
        //Hamster always returns size in kB
        Matcher matcher = PlugUtils.matcher("Размер\\s*файла:\\s*([0-9]+)", content);
        if (matcher.find()) {
            final long size = Long.parseLong(matcher.group(1)) * 1024;
            httpFile.setFileSize(size);
        }

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

            // Cannot be used for hamstershare service
            //final HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL).setActionFromFormWhereTagContains("Download!", true).toHttpMethod();//TODO

            client.setReferer(fileURL); //set referer
            final Matcher matcher = getMatcherAgainstContent("id=\"downloadBtn\" onclick=\"window.document.location.href='([^']+)'");
            if (matcher.find()) {
                final GetMethod httpMethod = getGetMethod(matcher.group(1));
                //here is the download link extraction
                if (!tryDownloadAndSaveFile(httpMethod)) {
                    checkProblems();//if downloading failed
                    logger.warning(getContentAsString());//log the info
                    throw new PluginImplementationException();//some unknown problem
                }
            } else throw new PluginImplementationException("Plugin error: Download link not found");
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("Файл не существует")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }

}