package cz.vity.freerapid.plugins.services.data_premium;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
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
class Data_PremiumFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(Data_PremiumFileRunner.class.getName());
    private boolean badConfig = false;


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
        PlugUtils.checkName(httpFile, content, "Fájl letöltés: <div class=\"download_filename\">", "</div>");
        PlugUtils.checkFileSize(httpFile, content.replace("1,000.0 MB", "1 GB"), "fájlméret: <div class=\"download_filename\">", "</div>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);

        final HttpMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize(getContentAsString());
            Matcher matcher = getMatcherAgainstContent("class=\"login_button\"");
            if (matcher.find()) {
                logger.info("Starting login");
                Login();
            }

            makeRedirectedRequest(getMethod);
            matcher = getMatcherAgainstContent("Nincs érvényes prémium elõfizetésed!");
            if (matcher.find()) {
                logger.info("No premium");
                throw new NotRecoverableDownloadException("Not premium account!");
            }
            matcher = getMatcherAgainstContent("window.location.href='(.*?)';");
            if (matcher.find()) {
                String downURL = matcher.group(1);
                logger.info("downURL: " + downURL);
                final GetMethod getmethod = getGetMethod(downURL);
                httpFile.setState(DownloadState.GETTING);
                if (!tryDownloadAndSaveFile(getmethod)) {
                    checkProblems();
                    logger.info(getContentAsString());
                    throw new PluginImplementationException();
                }
            } else {
                checkProblems();
                logger.info(getContentAsString());
                throw new PluginImplementationException();
            }

        } else
            throw new ServiceConnectionProblemException();
    }

    private void Login() throws Exception {
        synchronized (Data_PremiumFileRunner.class) {
            Data_PremiumServiceImpl service = (Data_PremiumServiceImpl) getPluginService();
            PremiumAccount pa = service.getConfig();
            if (!pa.isSet() || badConfig) {
                pa = service.showConfigDialog();
                if (pa == null || !pa.isSet()) {
                    throw new NotRecoverableDownloadException("No data.hu login information!");
                }
                badConfig = false;
            }
            Matcher redir = PlugUtils.matcher("name=\"target\" value=\"([^\"]+)\"", getContentAsString());
            if (!redir.find()) {
                throw new PluginImplementationException();
            }
            String redirname = redir.group(1);

            Matcher matcher = PlugUtils.matcher("<form name=\"([^\"]+)\" id=\"([^\"]+)\" action=\"([^\"]+)\" method=\"post\" autocomplete=\"off\">", getContentAsString());
            if (!matcher.find()) {
                throw new PluginImplementationException();
            }
            String postURL = matcher.group(3);
            Matcher pass = PlugUtils.matcher("<input type=\"password\" name=\"([^\"]+)\"", getContentAsString());
            if (!pass.find()) {
                throw new PluginImplementationException();
            }
            String passname = pass.group(1);

            PostMethod postmethod = getPostMethod("http://data.hu/" + postURL);

            postmethod.addParameter("act", "dologin");
            postmethod.addParameter("login_passfield", passname);
            postmethod.addParameter("t", "");
            postmethod.addParameter("id", "");
            postmethod.addParameter("data", "");
            postmethod.addParameter("username", pa.getUsername());
            postmethod.addParameter(passname, pa.getPassword());
            postmethod.addParameter("target", redirname);
            postmethod.addParameter("remember", "on");
            postmethod.addParameter("url_for_login", redirname);
            logger.info(redirname);
            if (makeRedirectedRequest(postmethod)) {
                matcher = getMatcherAgainstContent("class=\"login_button\"");
                if (matcher.find()) {
                    badConfig = true;
                    logger.info("bad info");
                    throw new NotRecoverableDownloadException("Bad data.hu login information!");
                }
            } else {
                logger.info(postURL);
                throw new PluginImplementationException("Bad login URL");
            }
        }

    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("nem létezik")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }

}