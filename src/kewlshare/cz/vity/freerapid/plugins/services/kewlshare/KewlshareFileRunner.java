package cz.vity.freerapid.plugins.services.kewlshare;

import cz.vity.freerapid.plugins.exceptions.*;
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
 * @author Arthur Gunawan
 */
class KewlshareFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(KewlshareFileRunner.class.getName());
    public boolean isFinal;


    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);//make first request
        if (makeRedirectedRequest(getMethod)) {
            checkNameAndSize();//ok let's extract file name and size from the page
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        checkProblems();
        final Matcher matcher = getMatcherAgainstContent("<h1>(.+?) \\|\\| (.+?)</h1>");
        if (!matcher.find()) throw new PluginImplementationException("File name/size not found");
        httpFile.setFileName(matcher.group(1));
        httpFile.setFileSize(PlugUtils.getFileSizeFromString(matcher.group(2)));
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        GetMethod method = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            checkNameAndSize();//extract file name and size from the page
            isFinal = false;
            while (!isFinal) {
                ProcessHTML(getContentAsString());
                //logger.info(getContentAsString());
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }


    private void ProcessHTML(String content) throws Exception {
        int result = 0;

        /*
        if (content.contains("http://img.kewlshare.com/dl/images/contip2.png")) {
            result = 1;
            HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL).setBaseURL(fileURL).setAction(fileURL).setActionFromFormWhereTagContains("http://img.kewlshare.com/dl/images/contip2.png", true).toHttpMethod();
            if (!makeRedirectedRequest(httpMethod)) {
                checkProblems();
                logger.warning(getContentAsString());
                throw new PluginImplementationException("Can't post first page");//some unknown problem
            }
            logger.info("HTML Process 1 OK!");
        }
        if (content.contains("http://kewlshare.com/button/free.gif")) {
            result = 2;
            HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL).setBaseURL(fileURL).setAction(fileURL).setActionFromFormWhereTagContains("http://kewlshare.com/button/free.gif", true).toHttpMethod();
            if (!makeRedirectedRequest(httpMethod)) {
                checkProblems();
                logger.warning(getContentAsString());
                throw new PluginImplementationException("Can't post first page");//some unknown problem
            }
            logger.info("HTML Process 1 OK!");
        }
        if (content.contains("name=\"selection\" value=\"Free\" type=\"hidden\"")) {
            result = 3;
            HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL).setBaseURL(fileURL).setAction(fileURL).setActionFromFormWhereTagContains("input id=\"imageInput\"", true).toHttpMethod();
            if (!makeRedirectedRequest(httpMethod)) {
                checkProblems();
                logger.warning(getContentAsString());
                throw new PluginImplementationException("Can't post first page");//some unknown problem
            }
            logger.info("HTML Process 2 OK!");
        }
        if (content.contains("http://kewlshare.com/img/pod.gif")) {
            result = 4;
            HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL).setBaseURL(fileURL).setAction(fileURL).setActionFromFormWhereTagContains("http://kewlshare.com/img/pod.gif", true).toHttpMethod();
            if (!makeRedirectedRequest(httpMethod)) {
                checkProblems();
                logger.warning(getContentAsString());
                throw new PluginImplementationException("Can't post first page");//some unknown problem
            }
            logger.info("HTML Process 3 OK!");
        }
        if (content.contains("http://kewlshare.com/img/down.gif")) {
            result = 5;
            HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL).setBaseURL(fileURL).setActionFromFormWhereTagContains("http://kewlshare.com/img/down.gif", true).toHttpMethod();
            if (!makeRedirectedRequest(httpMethod)) {
                checkProblems();
                logger.warning(getContentAsString());
                throw new PluginImplementationException("Can't download");//some unknown problem
            }
            logger.info("HTML Process 4 OK!");
        }
        if (content.contains("Click Here If your Download Doesn't Start Automatically")) {
            result = 6;
            String newURL = PlugUtils.getStringBetween(getContentAsString(), "<a href=\"", "\"> <span class=\"stylet\">");
            logger.info("Final URL: " + newURL);

            GetMethod method = getGetMethod(newURL);
            if (!tryDownloadAndSaveFile(method)) {
                checkProblems();
                logger.warning(getContentAsString());
                throw new PluginImplementationException("Can't download");//some unknown problem
            }

            logger.info("HTML Process 5 OK!");
            isFinal = true;
        }
        if (content.contains("setTimeout(\"waitTimer()\", 1000);")) {
            result = 7;
            //downloadTask.sleep(1000);
            HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL).setBaseURL(fileURL).setAction(fileURL).setActionFromFormWhereTagContains("http://img.kewlshare.com/dl/images/proceed.png", true).toHttpMethod();
            if (!makeRedirectedRequest(httpMethod)) {
                checkProblems();
                logger.warning(getContentAsString());
                throw new PluginImplementationException("Can't post first page");//some unknown problem
            }
            logger.info("HTML Process 7 OK!");
        }
        if (content.contains("http://img.kewlshare.com/dl/images/dlnow.png")) {
            result = 8;
            String newURL = PlugUtils.getStringBetween(getContentAsString(), "form action=\"", "\" method=\"get\" name=\"post\"");

            logger.info("Final URL: " + newURL);

            GetMethod method = getGetMethod(newURL);
            if (!tryDownloadAndSaveFile(method)) {
                checkProblems();
                logger.warning(getContentAsString());
                throw new PluginImplementationException("Can't download");//some unknown problem
            }

            logger.info("HTML Process 8 OK!");
            isFinal = true;
        }
        */

        if (content.contains("http://img.kewlshare.com/dl/images/freedl.png")) {
            result = 1;
            HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL).setBaseURL(fileURL).setAction(fileURL).setActionFromFormWhereTagContains("http://img.kewlshare.com/dl/images/freedl.png", true).toHttpMethod();
            if (!makeRedirectedRequest(httpMethod)) {
                checkProblems();
                throw new PluginImplementationException("Can't post page");//some unknown problem
            }
            logger.info("HTML Process 1 OK!");
        }
        if (content.contains("http://img.kewlshare.com/dl/images/prodl.png")) {
            result = 2;
            HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL).setBaseURL(fileURL).setActionFromFormWhereTagContains("http://img.kewlshare.com/dl/images/prodl.png", true).toHttpMethod();
            if (!makeRedirectedRequest(httpMethod)) {
                checkProblems();
                throw new PluginImplementationException("Can't post page");//some unknown problem
            }
            logger.info("HTML Process 2 OK!");
        }
        if (content.contains("http://img.kewlshare.com/dl/images/dlsave.png")) {
            result = 3;
            HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL).setBaseURL(fileURL).setActionFromFormWhereTagContains("http://img.kewlshare.com/dl/images/dlsave.png", true).toHttpMethod();
            if (!tryDownloadAndSaveFile(httpMethod)) {
                checkProblems();
                throw new PluginImplementationException("File input stream is empty");//some unknown problem
            }
            logger.info("HTML Process 3 OK!");
            isFinal = true;
        }

        checkProblems();
        if (result < 1) {
            throw new PluginImplementationException("Unknown server command (plugin issue?)");//some unknown problem
        }

    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("File Not Found") || contentAsString.contains("file you requested is either deleted") || contentAsString.contains("Link You requested not found")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
        if (contentAsString.contains("Download Limit")) {
            throw new YouHaveToWaitException("Download Limit 1 hour is over", 3600); //let to know user in FRD
        }
        if (contentAsString.contains("Please Inform us if you see this Error")) {
            throw new ServiceConnectionProblemException("Kewlshare server error"); //let to know user in FRD
        }
        if (contentAsString.contains("This Server Usage is really high in this moment")) {
            throw new YouHaveToWaitException("High server load", 60); //let to know user in FRD
        }
        if (contentAsString.contains("You can download your next file after")) {
            String swaitTime = PlugUtils.getStringBetween(contentAsString, "You can download your next file after ", "</div>").trim();
            logger.info("Waits: " + swaitTime);
            Matcher mTime = PlugUtils.matcher("([0-9]+):([0-9]+):([0-9]+)", swaitTime);
            if (mTime.find()) {
                int waitTime = new Integer(mTime.group(1)) * 3600 + new Integer(mTime.group(2)) * 60 + new Integer(mTime.group(3));
                logger.info("Wait Time = " + waitTime);
                throw new YouHaveToWaitException("Wait " + waitTime + " seconds", waitTime); //let to know user in FRD
            }
        }
    }

}
