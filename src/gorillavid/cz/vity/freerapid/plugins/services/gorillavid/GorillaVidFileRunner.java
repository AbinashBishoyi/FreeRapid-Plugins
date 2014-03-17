package cz.vity.freerapid.plugins.services.gorillavid;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;
import cz.vity.freerapid.plugins.webclient.DownloadClientConsts;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;

import java.net.URL;
import java.util.logging.Logger;

/**
 * Class which contains main code
 *
 * @author tong2shot
 */
class GorillaVidFileRunner extends XFileSharingRunner {
    private final static Logger logger = Logger.getLogger(GorillaVidFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        if (fileURL.matches("http://(?:www\\.)?gorillavid\\.com/.+")) {
            httpFile.setNewURL(new URL(fileURL.replaceFirst("gorillavid\\.com", "gorillavid.in")));
        }
        super.runCheck();
    }

    @Override
    protected void checkFileSize() throws ErrorDuringDownloadingException {

    }

    @Override
    public void run() throws Exception {
        setLanguageCookie();
        logger.info("Starting download in TASK " + fileURL);
        login();
        HttpMethod method = getGetMethod(fileURL);
        if (!makeRedirectedRequest(method)) {
            checkFileProblems();
            throw new ServiceConnectionProblemException();
        }
        checkFileProblems();
        checkNameAndSize();
        checkDownloadProblems();
        final int waitTime = getWaitTime();
        final long startTime = System.currentTimeMillis();
        sleepWaitTime(waitTime, startTime);
        MethodBuilder methodBuilder = getMethodBuilder()
                .setReferer(fileURL)
                .setActionFromFormWhereTagContains("method_free", true)
                .setAction(fileURL);
        if (!methodBuilder.getParameters().get("method_free").isEmpty()) {
            methodBuilder.removeParameter("method_premium");
        }
        method = methodBuilder.toPostMethod();
        if (!makeRedirectedRequest(method)) {
            checkDownloadProblems();
            logger.warning(getContentAsString());
            throw new ServiceConnectionProblemException();
        }
        checkDownloadProblems();
        methodBuilder = getMethodBuilder()
                .setAction(PlugUtils.getStringBetween(getContentAsString(), "file: \"", "\","));
        stepPassword(methodBuilder);
        setClientParameter(DownloadClientConsts.DONT_USE_HEADER_FILENAME, true);
        method = methodBuilder.toGetMethod();
        if (!tryDownloadAndSaveFile(method)) {
            checkDownloadProblems();
            throw new ServiceConnectionProblemException("Error starting download");
        }
    }
}