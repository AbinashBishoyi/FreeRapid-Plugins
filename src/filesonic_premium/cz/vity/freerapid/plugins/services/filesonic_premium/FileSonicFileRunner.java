package cz.vity.freerapid.plugins.services.filesonic_premium;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class which contains main code
 *
 * @author valankar
 */
class FileSonicFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(FileSonicFileRunner.class.getName());

    private String ensureENLanguage(String url){
        Matcher m=Pattern.compile("http://(www\\.)?filesonic.com/([^/]*/)?(file/.*)").matcher(url);
        if(m.matches()){
            return "http://www.filesonic.com/en/"+m.group(3);
        }
        return url;
    }

    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        final GetMethod getMethod = getGetMethod(ensureENLanguage(fileURL));//make first request
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize(getContentAsString());//ok let's extract file name and size from the page
        } else
            throw new PluginImplementationException();
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, content, "<span>Filename: </span> <strong>", "</strong>");
        PlugUtils.checkFileSize(httpFile, content, "<span class=\"size\">", "</span>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        login();
        final GetMethod method = getGetMethod(fileURL);
        if (!tryDownloadAndSaveFile(method)) {
            checkProblems();//if downloading failed
            logger.warning(getContentAsString());//log the info
            throw new PluginImplementationException();//some unknown problem
        }
    }

    public void login() throws Exception {
        FileSonicServiceImpl service = (FileSonicServiceImpl) getPluginService();
        PremiumAccount pa = service.getConfig();
        if (!pa.isSet()) {
            synchronized (FileSonicFileRunner.class) {
                pa = service.showConfigDialog();
                if (pa == null || !pa.isSet()) {
                    throw new NotRecoverableDownloadException("No FileSonic Premium account login information!");
                }
            }
        }
        final PostMethod pm = getPostMethod("http://www.filesonic.com/user/login");
        pm.addParameter("email", pa.getUsername());
        pm.addParameter("password", pa.getPassword());

        logger.info("Logging to FileSonic...");
        if (!makeRedirectedRequest(pm)) {
            throw new ServiceConnectionProblemException("Error posting login info");
        }

        // "view" is purposely misspelled.
        if (getContentAsString().contains("You must be logged in to veiw this page")) {
            throw new NotRecoverableDownloadException("Invalid FileSonic Premium account login information!");
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("File not found")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }
}