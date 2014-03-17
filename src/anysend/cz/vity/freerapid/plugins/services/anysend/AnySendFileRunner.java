package cz.vity.freerapid.plugins.services.anysend;

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
 * @author birchie
 */
class AnySendFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(AnySendFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);//make first request
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            final GetMethod getMethod2 = getGetMethod(PlugUtils.getStringBetween(getContentAsString(), "f.src=\"", "\""));
            if (makeRedirectedRequest(getMethod2)) {
                checkProblems();
                checkNameAndSize(getContentAsString());//ok let's extract file name and size from the page
            } else {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        final Matcher matchN = PlugUtils.matcher("File Name:</div>\\s+?(.+?)\\s+?<", content);
        if (!matchN.find()) throw new PluginImplementationException("File name not found");
        httpFile.setFileName(matchN.group(1).trim());
        final Matcher matchS = PlugUtils.matcher("File Size:</div>\\s+?(.+?)\\s+?<", content);
        if (!matchS.find()) throw new PluginImplementationException("File size not found");
        httpFile.setFileSize(PlugUtils.getFileSizeFromString(matchS.group(1)));
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod getMethod = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(getMethod)) { //we make the main request
            checkProblems();//check problems
            final String nextPageUrl = PlugUtils.getStringBetween(getContentAsString(), "f.src=\"", "\"");

            final String dlFileKey = PlugUtils.getStringBetween(nextPageUrl, "key=", "&");
            final String dlFileID = fileURL.substring(fileURL.lastIndexOf("/") + 1);
            HttpMethod hmVisId = getMethodBuilder().setAction("http://affiliates.anysend.com/scripts/track.php")
                    .setParameter("accountId", "default1")
                    .setParameter("url", "H_anysend.com//" + dlFileID)
                    .setParameter("referrer", "")
                    .setParameter("getParams", "")
                    .setParameter("anchor", "")
                    .setParameter("isInIframe", "false")
                    .setParameter("cookies", "")
                    .toGetMethod();
            setFileStreamContentTypes(new String[0], new String[]{"application/x-javascript"});
            if (!makeRedirectedRequest(hmVisId)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            final String visId = PlugUtils.getStringBetween(getContentAsString(), "setVisitor('", "');");
            final GetMethod getMethod2 = getGetMethod(nextPageUrl + visId);
            if (makeRedirectedRequest(getMethod2)) {
                final String contentAsString = getContentAsString();//check for response
                checkProblems();//check problems
                checkNameAndSize(contentAsString);//extract file name and size from the page

                final String dlKey = PlugUtils.getStringBetween(getContentAsString(), "dlkey='", "';");
                final HttpMethod hmIpAddr = getMethodBuilder()
                        .setReferer("")
                        .setAjax()
                        .setAction("http://im.anysend.com/check_file.php")
                        .setParameter("key", dlFileKey)
                        .setParameter("callback", "IP")
                        .toGetMethod();
                hmIpAddr.setRequestHeader("Accept", "application/json, text/javascript, */*");
                if (!makeRedirectedRequest(hmIpAddr)) {
                    checkProblems();
                    throw new ServiceConnectionProblemException();
                }
                final String ipAddr = PlugUtils.getStringBetween(getContentAsString(), "IP(\"", "\")");
                final String dlUrl = "http://" + ipAddr + "/anysend/download/" + dlKey + "/" + httpFile.getFileName();

                if (!tryDownloadAndSaveFile(getGetMethod(dlUrl))) {
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
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("Your file is not ready for download yet. Please wait while it gets prepared...")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }

}