package cz.vity.freerapid.plugins.services.extabit_premium;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class ExtabitPremiumFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(cz.vity.freerapid.plugins.services.extabit_premium.ExtabitPremiumFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, getContentAsString(), "<title>", "download Extabit.com - file hosting</title>");
        PlugUtils.checkFileSize(httpFile, getContentAsString(), "Size:", "</div>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);

        HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize();
            login();

            if (!tryDownloadAndSaveFile(method)) {
                checkDownloadProblems();
                //Redirection download Failed.....Using Button from page
                final String download = PlugUtils.getStringBetween(getContentAsString(), "href=\"", "\" class=\"styledButton\"");
                method = getGetMethod(download);
                if (!tryDownloadAndSaveFile(method)) {
                    checkDownloadProblems();
                    throw new ServiceConnectionProblemException("Error starting download");
                }
            }
        } else {
            checkDownloadProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        if (content.contains("File not found")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        if (content.contains("File is temporary unavailable")) {
            throw new ServiceConnectionProblemException("File is temporarily unavailable");
        }
    }

    private void checkDownloadProblems() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        checkProblems();
        if (content.contains("Next free download from your ip will be available in")) {
            final int waitTime = PlugUtils.getWaitTimeBetween(getContentAsString(), "Next free download from your ip will be available in <b>", " minutes</b>", TimeUnit.MINUTES);
            throw new YouHaveToWaitException("Next free download from your ip will be available in", waitTime);
        }
    }

    private void login() throws Exception {
        synchronized (ExtabitPremiumFileRunner.class) {
            ExtabitPremiumServiceImpl service = (ExtabitPremiumServiceImpl) getPluginService();
            PremiumAccount pa = service.getConfig();
            if (!pa.isSet()) {
                pa = service.showConfigDialog();
                if (pa == null || !pa.isSet()) {
                    throw new BadLoginException("No Extabit password information!");
                }
            }

            final HttpMethod httpMethod = getMethodBuilder()
                    .setAction("http://extabit.com/login.jsp")
                    .setParameter("email", pa.getUsername())
                    .setParameter("pass", pa.getPassword())
                    .setParameter("remember", "1")
                    .toPostMethod();
            if (!makeRedirectedRequest(httpMethod))
                throw new ServiceConnectionProblemException("Error posting login info");

            if (httpMethod.getResponseHeader("Location").getValue().contains("?err="))
                throw new BadLoginException("Invalid Extabit Premium account login information!");
        }
    }

}