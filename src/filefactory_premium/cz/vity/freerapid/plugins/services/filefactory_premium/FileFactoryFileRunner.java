package cz.vity.freerapid.plugins.services.filefactory_premium;

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
 * @author ntoskrnl
 */
class FileFactoryFileRunner extends AbstractRunner {
    private static final Logger logger = Logger.getLogger(FileFactoryFileRunner.class.getName());
    private static final String SERVICE_WEB = "http://www.filefactory.com";
    private boolean badConfig = false;

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);

        login();

        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            final String contentType = method.getResponseHeader("Content-Type").getValue().toLowerCase();
            if (!contentType.contains("html")) {
                //try downloading directly in case direct downloads are enabled
                if (tryDownloadAndSaveFile(method)) return;
            }

            checkProblems();
            checkNameAndSize();

            if (getContentAsString().contains("Download with FileFactory Basic"))
                throw new BadLoginException("Problem logging in, account not premium?");

            final HttpMethod httpMethod = getMethodBuilder()
                    .setReferer(fileURL)
                    .setActionFromAHrefWhereATagContains("Download with FileFactory")
                    .toGetMethod();
            if (!tryDownloadAndSaveFile(httpMethod)) {
                checkProblems();
                logger.warning(getContentAsString());
                throw new ServiceConnectionProblemException("Error starting download");
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        PlugUtils.checkName(httpFile, content, "class=\"last\">", "</span");
        PlugUtils.checkFileSize(httpFile, content, "<span>", "file uploaded");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        if (content.contains("this file is no longer available") || content.contains("id=\"uploader\"")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        if (content.contains("You have presently exceeded your Download allowance")) {
            final Matcher matcher = getMatcherAgainstContent("reset in (\\d+?) hour");
            if (!matcher.find())
                throw new PluginImplementationException("You have presently exceeded your download allowance, but waiting time was not found");
            throw new YouHaveToWaitException("You have presently exceeded your download allowance", 3600 * Integer.valueOf(matcher.group(1)) + 60 * 5);
        }
    }

    private void login() throws Exception {
        synchronized (FileFactoryFileRunner.class) {
            FileFactoryServiceImpl service = (FileFactoryServiceImpl) getPluginService();
            PremiumAccount pa = service.getConfig();
            if (!pa.isSet() || badConfig) {
                pa = service.showConfigDialog();
                if (pa == null || !pa.isSet()) {
                    throw new BadLoginException("No FileFactory Premium account login information!");
                }
                badConfig = false;
            }

            final HttpMethod httpMethod = getMethodBuilder()
                    .setBaseURL(SERVICE_WEB)
                    .setAction("/member/login.php")
                    .setParameter("email", pa.getUsername())
                    .setParameter("password", pa.getPassword())
                    .toPostMethod();
            if (!makeRedirectedRequest(httpMethod))
                throw new ServiceConnectionProblemException("Error posting login info");

            if (getContentAsString().contains("The email or password you have entered is incorrect"))
                throw new BadLoginException("Invalid FileFactory Premium account login information!");
        }
    }

}