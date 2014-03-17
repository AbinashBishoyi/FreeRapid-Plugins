package cz.vity.freerapid.plugins.services.wupload_premium;

import cz.vity.freerapid.plugins.exceptions.BadLoginException;
import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;

import java.util.logging.Logger;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 */
class WuploadFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(WuploadFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize(getContentAsString());
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, content, "<title>Get", "on Wupload.com</title>");
        final String size = PlugUtils.getStringBetween(content, "class=\"size\">", "</span>");
        httpFile.setFileSize(PlugUtils.getFileSizeFromString(size.replace(",", "")));
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        runCheck();
        login();
        setFileStreamContentTypes("\"application/octet-stream\"");
        final HttpMethod method = getGetMethod(fileURL);
        if (!tryDownloadAndSaveFile(method)) {
            checkProblems();
            throw new ServiceConnectionProblemException("Error starting download, account not premium?");
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        if (getContentAsString().contains("this file has been removed") || getContentAsString().contains("Page Not Found")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    private void login() throws Exception {
        synchronized (WuploadFileRunner.class) {
            WuploadServiceImpl service = (WuploadServiceImpl) getPluginService();
            PremiumAccount pa = service.getConfig();
            if (!pa.isSet()) {
                pa = service.showConfigDialog();
                if (pa == null || !pa.isSet()) {
                    throw new BadLoginException("No Wupload account login information!");
                }
            }
            //Sometimes redirected to a country specific domain. Login using that instead of .com.
            HttpMethod method = getGetMethod("http://www.wupload.com");
            if (!makeRedirectedRequest(method)) {
                throw new ServiceConnectionProblemException();
            }
            method = getMethodBuilder()
                    .setBaseURL(method.getURI().toString())
                    .setAction("/account/login")
                    .setParameter("email", pa.getUsername())
                    .setParameter("redirect", "/")
                    .setParameter("password", pa.getPassword())
                    .setParameter("rememberMe", "1")
                    .toPostMethod();
            method.setRequestHeader("Accept", "application/json, text/javascript, */*; q=0.01");
            method.setRequestHeader("X-Requested-With", "XMLHttpRequest");
            setFileStreamContentTypes(new String[0], new String[]{"application/json"});
            if (!makeRedirectedRequest(method)) {
                throw new ServiceConnectionProblemException("Error posting login info");
            }
            if (!getContentAsString().contains("\"status\":\"success\"")) {
                throw new BadLoginException("Invalid Wupload account login information!");
            }
        }
    }

}