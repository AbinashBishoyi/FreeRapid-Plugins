package cz.vity.freerapid.plugins.services.przeklej;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class which contains main code
 *
 * @author Eterad
 */
class PrzeklejFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(PrzeklejFileRunner.class.getName());


    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);//make first request
        if (makeRedirectedRequest(getMethod)) {
            checkNameAndSize(getContentAsString());//ok let's extract file name and size from the page
        } else
            throw new PluginImplementationException();
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, content, "title=\"Pobierz plik\">", "</a></h1><span class=\"size");
        PlugUtils.checkFileSize(httpFile, content, "<span class=\"size\" style=\"font-weight: normal;\"> (", ")</span>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        GetMethod method = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            final String contentAsString = getContentAsString();//check for response
            checkProblems();//check problems
            checkNameAndSize(contentAsString);//extract file name and size from the page
            //Getting download url
            String finalURL = "http://www.przeklej.pl" + PlugUtils.getStringBetween(contentAsString, "<h1><a href=\"", "\" title=\"Pobierz plik\"");
            //If file is password protected
            if (contentAsString.contains("Wprowadź hasło")) {
                logger.info(">>>>> The file is password protected <<<<<");
                while (getContentAsString().contains("Wprowadź hasło")) {
                    final PostMethod post = getPostMethod(finalURL);
                    post.setParameter("haslo[haslo]", getPassword());
                    client.setReferer(fileURL);
                    final int resultCode = client.makeRequest(post, false);
                    logger.info("----------- " + getContentAsString() + "---------------");
                    if (!isRedirect(resultCode))
                        throw new PluginImplementationException("Redirect not found");
                    Cookie[] cookies = client.getHTTPClient().getState().getCookies();
                    for (Cookie c : cookies) {
                        logger.info(">>>>> " + c.getName() + " : " + c.getValue() + " <<<<<");
                    }
                    logger.info("~~~~~~~~~~~~~   " + "   ~~~~~~~~~~~~~~");
                    final Header responseLocation = post.getResponseHeader("Location");//Location does not return correct URL
                    if (responseLocation == null)
                        throw new PluginImplementationException("Location header not found");
                    logger.info(">>>>> Location: " + responseLocation.getValue() + " <<<<<");
                    //Getting correct file name from redirect URL
                    Pattern p = Pattern.compile("http://.*\\.przeklej.pl/[a-z0-9]+/[a-z0-9]+/(.*)\\?przid=.*");
                    Matcher m = p.matcher(responseLocation.getValue());
                    if (!m.find()) {
                        p = Pattern.compile("/(plik)/.*");
                        m = p.matcher(responseLocation.getValue());
                        //If there is no download url then check if there is redirect to bad password page
                        if (!m.find()) {
                            logger.warning(getContentAsString());
                            throw new PluginImplementationException("Error while preparing download");
                        } else throw new BadLoginException("Incorrect password");
                    }
                    logger.info(">>>>> File name: " + m.group(1).trim() + " <<<<<");
                    //Setting correct file name in case site will cut some part of it
                    httpFile.setFileName(m.group(1).trim());
                    final HttpMethod finalHttpMethod = getMethodBuilder().setReferer(finalURL).setAction(responseLocation.getValue()).toHttpMethod();
                    if (!tryDownloadAndSaveFile(finalHttpMethod)) {
                        checkProblems();
                        logger.warning(getContentAsString());
                        throw new PluginImplementationException();
                    }
                }
            } else {
                logger.info(">>>>> The file is unprotected <<<<<");
                final HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL).setAction(finalURL).toHttpMethod();
                //here is the download link extraction
                if (!tryDownloadAndSaveFile(httpMethod)) {
                    checkProblems();//if downloading failed
                    logger.warning(getContentAsString());//log the info
                    throw new PluginImplementationException();//some unknown problem
                }
            }

        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("Plik nie istnieje!")) {//
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }

    private String getPassword() throws Exception {

        PrzeklejPasswordUI ps = new PrzeklejPasswordUI();
        if (getDialogSupport().showOKCancelDialog(ps, "Secured file on Przeklej.pl")) {
            return (ps.getPassword());
        } else throw new NotRecoverableDownloadException("This file is password secured");

    }

    protected boolean isRedirect(int statuscode) {
        return (statuscode == HttpStatus.SC_MOVED_TEMPORARILY) ||
                (statuscode == HttpStatus.SC_MOVED_PERMANENTLY) ||
                (statuscode == HttpStatus.SC_SEE_OTHER) ||
                (statuscode == HttpStatus.SC_TEMPORARY_REDIRECT);
    }

}
