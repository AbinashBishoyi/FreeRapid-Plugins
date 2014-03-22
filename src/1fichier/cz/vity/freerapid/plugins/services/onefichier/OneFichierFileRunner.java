package cz.vity.freerapid.plugins.services.onefichier;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.exceptions.YouHaveToWaitException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.net.URLDecoder;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class OneFichierFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(OneFichierFileRunner.class.getName());

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
        final Matcher match = PlugUtils.matcher("http://(\\w+)\\.(1fichier|desfichiers)\\.com/en/?(.*)", fileURL);
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
        PlugUtils.checkName(httpFile, content, "name :</th><td>", "</td>");
        PlugUtils.checkFileSize(httpFile, content, "Size :</th><td>", "</td>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        setEnglishURL();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL); //create GET request
        final int status1 = client.makeRequest(method, false);
        if (status1 / 100 == 3) {
            final String dlLink = method.getResponseHeader("Location").getValue();
            httpFile.setFileName(URLDecoder.decode(dlLink.substring(1 + dlLink.lastIndexOf("/")), "UTF-8"));
            if (!tryDownloadAndSaveFile(method)) {
                checkProblems();//if downloading failed
                throw new ServiceConnectionProblemException("Error starting download");//some unknown problem
            }
        } else if (status1 == 200) {
            while (true) {
                checkProblems();//check problems
                try {
                    checkNameAndSize(getContentAsString());//extract file name and size from the page
                } catch (Exception e) {/**/}
                final HttpMethod hMethod = getMethodBuilder().setReferer(fileURL).setActionFromFormWhereTagContains("ownload", true).toPostMethod();
                final int status = client.makeRequest(hMethod, false);
                if (status / 100 == 3) {
                    if (!tryDownloadAndSaveFile(getGetMethod(hMethod.getResponseHeader("Location").getValue()))) {
                        checkProblems();//if downloading failed
                        throw new ServiceConnectionProblemException("Error starting download");//some unknown problem
                    }
                    return;
                } else if (status != 200) {
                    checkProblems();
                    throw new ServiceConnectionProblemException();
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
        if (contentAsString.contains("you can download only one file at a timeg")) {
            int delay = 15;
            try {
                delay = PlugUtils.getNumberBetween(contentAsString, "wait up to", "minute");
            } catch (Exception e) {/**/}
            throw new YouHaveToWaitException("You can download only one file at a time and you must wait up to " + delay + " minutes between each downloads", delay * 60);
        }
    }

}