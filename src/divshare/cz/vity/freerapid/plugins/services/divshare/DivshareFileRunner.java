package cz.vity.freerapid.plugins.services.divshare;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
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
 * @author Vity
 */
class DivshareFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(DivshareFileRunner.class.getName());


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
        if (!content.contains("Download Original")) {
            if (content.contains("<div class=\"file_name\">")) {
                PlugUtils.checkName(httpFile, content, "<div class=\"file_name\">", "</div>");
            } else if (content.contains(".gif\" valign=\"absmiddle\">")) {
                PlugUtils.checkName(httpFile, content, ".gif\" valign=\"absmiddle\">", "</div>");
            }
        }
        final Matcher matcher = getMatcherAgainstContent("<b>File Size:</b>(.+?)<span class=\"tiny\">(.+?)</span><br />");
        if (matcher.find()) {
            final long size = PlugUtils.getFileSizeFromString(matcher.group(1) + " " + matcher.group(2));
            httpFile.setFileSize(size);
            httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
        } else throw new PluginImplementationException("File size not found");
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
            final HttpMethod httpMethod;
//            if (!contentAsString.contains("Download Original"))
//                httpMethod = getMethodBuilder().setReferer(fileURL).setActionFromAHrefWhereATagContains("Download This File").toHttpMethod();
//            else
            httpMethod = getMethodBuilder().setReferer(fileURL).setActionFromAHrefWhereATagContains("Download").toHttpMethod();
            if (makeRedirectedRequest(httpMethod)) {
                checkProblems();
                HttpMethod hmethod = getMethodBuilder().setReferer(fileURL).setActionFromAHrefWhereATagContains("Skip Ad >").toHttpMethod();
                //downloadTask.sleep(15);
                if (makeRedirectedRequest(hmethod)) {
                    checkProblems();
                    hmethod = getMethodBuilder().setActionFromAHrefWhereATagContains("click here").toHttpMethod();
                    //here is the download link extraction
                    if (!tryDownloadAndSaveFile(hmethod)) {
                        checkProblems();//if downloading failed
                        logger.warning(getContentAsString());//log the info
                        throw new PluginImplementationException();//some unknown problem
                    }
                } else throw new PluginImplementationException("Some waiting problem");
            } else throw new PluginImplementationException("Download this file link problem");

        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("Sorry, we couldn't find this file")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
        if (contentAsString.contains("is not available to free users in China")) {
            throw new URLNotAvailableAnymoreException("This file is not available to free users in China and Southeast Asia."); //let to know user in FRD
        }
        if (contentAsString.contains("download limit")) {
            throw new ServiceConnectionProblemException("Download limit");
        }
    }

    @Override
    protected String getBaseURL() {
        return "http://divshare.com";
    }
}