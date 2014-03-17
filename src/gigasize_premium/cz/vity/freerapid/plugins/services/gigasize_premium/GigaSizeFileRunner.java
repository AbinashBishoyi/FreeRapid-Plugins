package cz.vity.freerapid.plugins.services.gigasize_premium;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 */
class GigaSizeFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(GigaSizeFileRunner.class.getName());

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
        final String content = getContentAsString();
        PlugUtils.checkName(httpFile, content, "<title>", "- GigaSize.com");
        PlugUtils.checkFileSize(httpFile, content, "File size: <strong>", "</strong>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        login();
        HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize();
            final String fileId = PlugUtils.getParameter("fileId", getContentAsString());
            method = getMethodBuilder().setActionFromFormByName("downloadForm", true).toPostMethod();
            if (!makeRedirectedRequest(method)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            method = getMethodBuilder()
                    .setReferer(fileURL)
                    .setAction("/getoken")
                    .setParameter("fileId", fileId)
                    .setParameter("token", getToken())
                    .setParameter("rnd", String.valueOf(System.nanoTime()))
                    .toPostMethod();
            if (!makeRedirectedRequest(method)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            final Matcher matcher = getMatcherAgainstContent("\"redirect\"\\s*:\\s*\"(.+?)\"");
            if (!matcher.find()) {
                throw new PluginImplementationException("Download URL not found");
            }
            final String url = matcher.group(1).replace("\\/", "/");
            method = getMethodBuilder().setReferer(fileURL).setAction(url).toGetMethod();
            if (!tryDownloadAndSaveFile(method)) {
                checkProblems();
                throw new ServiceConnectionProblemException("Error starting download");
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        if (getContentAsString().contains("The file you are looking for is not available")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    private void login() throws Exception {
        synchronized (GigaSizeFileRunner.class) {
            GigaSizeServiceImpl service = (GigaSizeServiceImpl) getPluginService();
            PremiumAccount pa = service.getConfig();
            if (!pa.isSet()) {
                pa = service.showConfigDialog();
                if (pa == null || !pa.isSet()) {
                    throw new BadLoginException("No GigaSize account login information!");
                }
            }

            final HttpMethod method = getMethodBuilder()
                    .setAction("/signin")
                    .setParameter("func", "")
                    .setParameter("token", getToken())
                    .setParameter("signRem", "1")
                    .setParameter("email", pa.getUsername())
                    .setParameter("password", pa.getPassword())
                    .toPostMethod();
            method.removeRequestHeader("Referer");
            method.setRequestHeader("X-Requested-With", "XMLHttpRequest");
            if (!makeRedirectedRequest(method)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }

            if (!PlugUtils.find("\"status\"\\s*:\\s*1", getContentAsString())) {
                throw new BadLoginException("Invalid GigaSize account login information!");
            }
            if (!PlugUtils.find("\"premium\"\\s*:\\s*1", getContentAsString())) {
                throw new BadLoginException("Account not premium!");
            }
        }
    }

    private String getToken() throws Exception {
        final HttpMethod method = getMethodBuilder().setReferer(fileURL).setAction("/formtoken").toGetMethod();
        if (!makeRedirectedRequest(method)) {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
        return getContentAsString().trim();
    }

}