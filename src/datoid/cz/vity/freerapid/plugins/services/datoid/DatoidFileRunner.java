package cz.vity.freerapid.plugins.services.datoid;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
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
class DatoidFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(DatoidFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception { //this method validates file
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

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        Matcher match = PlugUtils.matcher("<h1>(?:\\s*?<[^>]+>)?\\s*?(.+?)\\s*?</h1>", content);
        if (!match.find())
            throw new PluginImplementationException("File name not found");
        httpFile.setFileName(match.group(1).trim());
        match = PlugUtils.matcher("Velikost:</th>\\s*?<td>(.+?)</td>", content);
        if (!match.find())
            throw new PluginImplementationException("File size not found");
        httpFile.setFileSize(PlugUtils.getFileSizeFromString(match.group(1)));
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
            login();

            final Matcher match = PlugUtils.matcher("<a.+?href=\"(.*?/f/.+?)\".*?>\\s*?.*?[Ss]t.hnout", getContentAsString());
            if (!match.find())
                throw new PluginImplementationException("Download options not found");
            String linkUrl = match.group(1);
            if (!linkUrl.contains("request="))
                linkUrl = linkUrl + "?request=1";
            final HttpMethod linkMethod = getMethodBuilder().setAction(linkUrl).toHttpMethod();
            if (!makeRedirectedRequest(linkMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            checkProblems();
            final String content = getContentAsString();
            final int wait = Integer.parseInt(getValue(content, "wait"));
            final String dlUrl = getValue(content, "download_link").replace("\\", "");
            downloadTask.sleep(wait);
            final HttpMethod httpMethod = getGetMethod(dlUrl);
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
        final String content = getContentAsString();
        if (content.contains("<h1 class=\"error-404\">Omlouv")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
        if (content.contains("error\":\"IP in use")) {
            throw new ServiceConnectionProblemException("IP address is already downloading");
        }
        if (content.contains("error\":\"User in use")) {
            throw new ServiceConnectionProblemException("File already downloading");
        }
        if (content.contains("error\":\"No free slots")) {
            throw new ServiceConnectionProblemException("No free slots");
        }
        if (content.contains("error\":\"No anonymous free slots")) {
            throw new ServiceConnectionProblemException("No anonymous free slots");
        }
        if (content.contains("error\":\"Wrong request")) {
            throw new ServiceConnectionProblemException("File too large for you to download");
        }
        if (content.contains("error\":\"")) {
            throw new PluginImplementationException(PlugUtils.getStringBetween(content, "error\":\"", "\""));
        }
    }

    private String getValue(final String content, final String name) throws Exception {
        final Matcher match = PlugUtils.matcher("\"" + name + "\":\"?(.+?)\"?[,}]", content);
        if (!match.find())
            throw new PluginImplementationException("Value for " + name + " not found");
        return match.group(1);
    }


    private void login() throws Exception {
        synchronized (DatoidFileRunner.class) {
            final DatoidServiceImpl service = (DatoidServiceImpl) getPluginService();
            final PremiumAccount pa = service.getConfig();
            if (pa.isSet()) {
                final HttpMethod method = getMethodBuilder()
                        .setActionFromFormWhereActionContains("signInForm", true)
                        .setParameter("username", pa.getUsername())
                        .setParameter("password", pa.getPassword())
                        .setReferer(fileURL)
                        .toPostMethod();
                if (!makeRedirectedRequest(method)) {
                    throw new ServiceConnectionProblemException("Error posting login info");
                }
                if (getContentAsString().contains("Zadali jste špatné přihlašovací údaje.")) {
                    throw new BadLoginException("Invalid Datoid.cz account login information!");
                }
                logger.info("Logged in.");
            }

        }
    }

}