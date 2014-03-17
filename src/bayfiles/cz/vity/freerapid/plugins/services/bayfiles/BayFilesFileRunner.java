package cz.vity.freerapid.plugins.services.bayfiles;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.exceptions.YouHaveToWaitException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Class which contains main code
 *
 * @author tong2shot
 */
class BayFilesFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(BayFilesFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception { //this method validates file
        checkUrl();
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);//make first request
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize(getContentAsString());//ok let's extract file name and size from the page
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkUrl() {
        if (fileURL.contains("bayfiles.com"))
            fileURL = fileURL.replaceFirst("bayfiles.com", "bayfiles.net");
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, content, "<p title=\"", "\">");
        PlugUtils.checkFileSize(httpFile, content, "<strong>", "</strong></p>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        checkUrl();
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            String contentAsString = getContentAsString();//check for response

            checkProblems();//check problems
            checkSecondaryProblems();
            checkNameAndSize(contentAsString);//extract file name and size from the page

            final String vfid = PlugUtils.getStringBetween(contentAsString, "var vfid = ", ";");
            final int waitTime = PlugUtils.getWaitTimeBetween(contentAsString, "id=\"countDown\">", "</strong>", TimeUnit.SECONDS);
            logger.info("vfid = " + vfid);

            MethodBuilder ajaxMethodBuilder = getMethodBuilder()
                    .setAction("http://bayfiles.net/ajax_download")
                    .setParameter("action", "startTimer")
                    .setParameter("vfid", vfid);
            HttpMethod ajaxMethod = ajaxMethodBuilder.toGetMethod();

            if (makeRequest(ajaxMethod)) {
                contentAsString = getContentAsString();
                String token = PlugUtils.getStringBetween(contentAsString, "\"token\":\"", "\",");
                logger.info("token = " + token);
                downloadTask.sleep(waitTime);

                ajaxMethodBuilder = getMethodBuilder()
                        .setAction("http://bayfiles.net/ajax_download")
                        .setParameter("action", "getLink")
                        .setParameter("vfid", vfid)
                        .setParameter("token", token);
                ajaxMethod = ajaxMethodBuilder.toGetMethod();

                if (makeRequest(ajaxMethod)) {
                    contentAsString = getContentAsString();
                    final String fileURLToDownload = PlugUtils.getStringBetween(contentAsString, "window.location.href = '", "';\"");
                    logger.info("fileURLToDownload = " + fileURLToDownload);

                    final HttpMethod httpMethod = getMethodBuilder()
                            .setReferer(fileURL)
                            .setAction(fileURLToDownload)
                            .toHttpMethod();

                    //here is the download link extraction
                    if (!tryDownloadAndSaveFile(httpMethod)) {
                        checkProblems();//if downloading failed
                        throw new ServiceConnectionProblemException("Error starting download");//some unknown problem
                    }
                } else {
                    checkProblems();
                    throw new ServiceConnectionProblemException();
                }
            } else {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("The link is incorrect")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }

    private void checkSecondaryProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("has recently downloaded a file")) {
            final String sWaitTime = PlugUtils.getStringBetween(contentAsString, "Upgrade to premium or wait", "min");
            int iWaitTime = 300;
            if (sWaitTime.contains(":")) {
                final int minWait = Integer.parseInt(sWaitTime.substring(0, sWaitTime.indexOf(":")));
                final int secWait = Integer.parseInt(sWaitTime.substring(1 + sWaitTime.indexOf(":"), sWaitTime.length()));
                iWaitTime = minWait * 60 + secWait;
            }
            throw new YouHaveToWaitException("Need to wait " + iWaitTime + " sec between downloads", iWaitTime);
        } else if (contentAsString.contains("is already downloading")) {
            throw new ServiceConnectionProblemException("Your IP address is already downloading a file");
        }
    }

}