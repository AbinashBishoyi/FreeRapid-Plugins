package cz.vity.freerapid.plugins.services.onefichier_premium;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.net.URI;
import java.net.URLDecoder;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class OneFichierFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(OneFichierFileRunner.class.getName());
    private final static String loginUrl = "http://www.1fichier.com/en/login.pl";

    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        setEnglishURL();
        final GetMethod getMethod = getGetMethod(fileURL);//make first request
        final int status = client.makeRequest(getMethod, false);
        if (status / 100 == 3) {
            getAltTempFileName();
        } else if (status == 200) {
            checkProblems();
            try {
                checkNameAndSize(getContentAsString());
            } catch (Exception e) {
                getAltTempFileName();
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void getAltTempFileName() throws Exception {
        final Matcher match = PlugUtils.matcher("https?://(\\w+)\\.(1fichier|desfichiers)\\.com/en/?(.*)", fileURL);
        if (match.find()) {
            String name = match.group(1);
            if (URLDecoder.decode(match.group(3), "UTF-8").replace("\"", "").trim().length() > 0)
                name = URLDecoder.decode(match.group(3), "UTF-8").replace("\"", "").trim();
            httpFile.setFileName(name);
        }
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }


    private void setEnglishURL() {
        if (!fileURL.contains("/en")) {
            String[] temp = fileURL.split(".com");
            fileURL = temp[0] + ".com/en";
            if (temp.length > 1) fileURL += temp[1];
        }
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        if (fileURL.contains("/dir/")) {
            PlugUtils.checkName(httpFile, content, "<title>", "</title>");
        } else {
            PlugUtils.checkName(httpFile, content, "name :</th><td>", "</td>");
            PlugUtils.checkFileSize(httpFile, content, "Size :</th><td>", "</td>");
        }
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        setEnglishURL();
        login();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL); //create GET request
        final int status = client.makeRequest(method, false);
        if (status / 100 == 3) {
            final String dlLink = method.getResponseHeader("Location").getValue();
            httpFile.setFileName(URLDecoder.decode(dlLink.substring(1 + dlLink.lastIndexOf("/")), "UTF-8"));
            if (!tryDownloadAndSaveFile(method)) {
                checkProblems();//if downloading failed
                throw new ServiceConnectionProblemException("Error starting download");//some unknown problem
            }
        } else if (status == 200) {
            if (fileURL.contains("/dir/")) {
                List<URI> list = new LinkedList<URI>();
                final Matcher match = PlugUtils.matcher("<a href=\"(https?://(\\w+)\\.(1fichier|desfichiers)\\..+?)\"", getContentAsString());
                while (match.find()) {
                    list.add(new URI(match.group(1).trim()));
                }
                if (list.isEmpty()) throw new PluginImplementationException("No links found");
                getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, list);
                httpFile.setFileName("Link(s) Extracted !");
                httpFile.setState(DownloadState.COMPLETED);
                httpFile.getProperties().put("removeCompleted", true);
            } else {
                final String contentAsString = getContentAsString();//check for response
                checkProblems();//check problems
                checkNameAndSize(contentAsString);//extract file name and size from the page
                final HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL)
                        .setActionFromFormWhereTagContains("Download the file", true).toPostMethod(); //  2do ?

                //here is the download link extraction
                if (!tryDownloadAndSaveFile(httpMethod)) {
                    checkProblems();//if downloading failed
                    throw new ServiceConnectionProblemException("Error starting download");//some unknown problem
                }
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("file could not be found") ||
                contentAsString.contains("The requested file has been deleted")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
        if (contentAsString.contains("you can download only one file at a time")) {
            final int delay = PlugUtils.getNumberBetween(contentAsString, "wait up to", "minute");
            throw new YouHaveToWaitException("You can download only one file at a time and you must wait up to " + delay + " minutes between each downloads", delay * 60);
        }
    }

    private void login() throws Exception {
        synchronized (OneFichierFileRunner.class) {
            OneFichierServiceImpl service = (OneFichierServiceImpl) getPluginService();
            PremiumAccount pa = service.getConfig();
            if (!pa.isSet()) {
                pa = service.showConfigDialog();
                if (pa == null || !pa.isSet()) {
                    throw new BadLoginException("No 1Fichier account login information!");
                }
            }
            final HttpMethod method = getMethodBuilder()
                    .setAction(loginUrl).setReferer(loginUrl)
                    .setParameter("mail", pa.getUsername())
                    .setParameter("pass", pa.getPassword())
                    .setParameter("Login", "Login")
                    .toPostMethod();
            if (!makeRedirectedRequest(method)) {
                throw new ServiceConnectionProblemException("Error posting login info");
            }
            if (getContentAsString().contains("Invalid username or password") ||
                    getContentAsString().contains("Invalid email address") ||
                    getContentAsString().contains("Invalid password")) {
                throw new BadLoginException("Invalid 1Fichier account login information!");
            }
        }
    }

}