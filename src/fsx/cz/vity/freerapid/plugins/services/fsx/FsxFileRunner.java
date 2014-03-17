package cz.vity.freerapid.plugins.services.fsx;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpConnection;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author Javi
 */
class FsxFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(FsxFileRunner.class.getName());
    private static String PHPSESSID = "";

    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);//make first request
        if (makeRedirectedRequest(getMethod)) {
            Cookie[] cookies = client.getHTTPClient().getState().getCookies();
            for (Cookie c : cookies) {
                if ("PHPSESSID".equals(c.getName())) {
                    FsxFileRunner.PHPSESSID = c.getValue();
                }
            }
            if (PHPSESSID != null) {
                addCookie(new Cookie("fsx.hu", "PHPSESSID", PHPSESSID, "/", 8640, false));
            }
            client.getHTTPClient().getParams().setContentCharset("ISO-8859-2");
            final GetMethod method = getGetMethod("http://www.fsx.hu/download.php?i=1");
            if (makeRedirectedRequest(method)) {
                checkProblems();
                checkNameAndSize(getContentAsString());//ok let's extract file name and size from the page
            } else {
                throw new ServiceConnectionProblemException();
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, content, "size=\"4\">", "</font></br>");
        PlugUtils.checkFileSize(httpFile, content, "ret:</strong>", "B");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            Cookie[] cookies = client.getHTTPClient().getState().getCookies();
            for (Cookie c : cookies) {
                if ("PHPSESSID".equals(c.getName())) {
                    FsxFileRunner.PHPSESSID = c.getValue();
                }
            }
            if (PHPSESSID != null) {
                addCookie(new Cookie("www.fsx.hu", "PHPSESSID", PHPSESSID, "/", 8640, false));
            }
            final GetMethod gmethod = getGetMethod("http://www.fsx.hu/download.php?i=1");
             //Doesn't work from here,

            if (makeRedirectedRequest(gmethod)) {
                checkProblems();
                checkNameAndSize(getContentAsString());
            } else {
                throw new ServiceConnectionProblemException();
            }
            //gotta reload the page till i have a download link but:
            while (!getContentAsString().contains("elem.href")) {
                if (makeRedirectedRequest(gmethod)) {
                    //this reloads the page and i get to the beggining of the queue again //todo
                Matcher matcher = getMatcherAgainstContent("#FF0000\"><strong>(.*?)</strong></font> felhasz");
                if (matcher.find()) {
                    final String ez = matcher.group(1);
                    logger.info("People b4 you: " + ez);
                    downloadTask.sleep(5);
                }
                } else {
                throw new ServiceConnectionProblemException();
            }
            }
            Matcher matcher = getMatcherAgainstContent("elem.href + \"(.*?)\";");
            final String el, veg;
            if (matcher.find()) {
                el = matcher.group(1);
            } else {
                throw new ServiceConnectionProblemException("Download link not found!");
            }
            matcher = getMatcherAgainstContent("dlink' href=\"(.*?)\"><img src");
            if (matcher.find()) {
                veg = matcher.group(1);
            } else {
                throw new ServiceConnectionProblemException("Download link not found!");
            }
            logger.info("downurl:" + el + veg);
            final HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL).setAction(el + veg).toHttpMethod();

            //here is the download link extraction
            if (!tryDownloadAndSaveFile(httpMethod)) {
                checkProblems();//if downloading failed
                throw new ServiceConnectionProblemException("Error starting download");//some unknown problem
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("nem található")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
        if (contentAsString.contains("Not Found")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }

    }
}