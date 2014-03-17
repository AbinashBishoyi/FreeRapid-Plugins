package cz.vity.freerapid.plugins.services.gigasize;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author Kajda
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
        String fileId;
        do {
            HttpMethod method = getGetMethod(fileURL);
            if (!makeRedirectedRequest(method)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            checkProblems();
            checkNameAndSize();
            fileId = PlugUtils.getParameter("fileId", getContentAsString());
            final MethodBuilder captchaBuilder = getMethodBuilder().setActionFromFormByName("downloadForm", true);
            method = getMethodBuilder().setActionFromIFrameSrcWhereTagContains("adscaptcha").toGetMethod();
            if (!makeRedirectedRequest(method)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            final String captchaUrl = getMethodBuilder().setActionFromImgSrcWhereTagContains("AdsCaptcha Challenge").getEscapedURI();
            final String challenge = PlugUtils.getStringBetween(getContentAsString(), "<td class=\"code\">", "</td>");
            final String captcha = getCaptchaSupport().getCaptcha(captchaUrl);
            method = captchaBuilder.setParameter("adscaptcha_response_field", captcha).setParameter("adscaptcha_challenge_field", challenge).toPostMethod();
            if (!makeRedirectedRequest(method)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
        } while (!PlugUtils.find("\"status\"\\s*:\\s*1", getContentAsString()));

        downloadTask.sleep(31);
        HttpMethod method = getMethodBuilder().setReferer(fileURL).setAction("/formtoken").toGetMethod();
        if (!makeRedirectedRequest(method)) {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
        final String token = getContentAsString().trim();
        method = getMethodBuilder().setReferer(fileURL).setAction("/getoken").setParameter("fileId", fileId).setParameter("token", token).setParameter("rnd", String.valueOf(System.nanoTime())).toPostMethod();
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
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        if (getContentAsString().contains("The file you are looking for is not available")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

}