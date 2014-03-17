package cz.vity.freerapid.plugins.services.ifile;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;

import java.net.URLEncoder;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author JPEXS, ntoskrnl
 * @since 0.83
 */
class IFileFileRunner extends AbstractRunner {

    private final static Logger logger = Logger.getLogger(IFileFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final HttpMethod httpMethod = getMethodBuilder().setAction(fileURL).toGetMethod();
        if (makeRedirectedRequest(httpMethod)) {
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
        HttpMethod httpMethod = getMethodBuilder().setAction(fileURL).toGetMethod();
        if (makeRedirectedRequest(httpMethod)) {
            checkProblems();
            checkNameAndSize();
            final String recaptchaKey = getVar("__recaptcha_public");
            final String requestUrl = "/download-request.json?ukey=" + getVar("__ukey");
            setFileStreamContentTypes(new String[0], new String[]{"application/json"});
            httpMethod = getMethodBuilder().setAction(requestUrl).toPostMethod();
            if (makeRedirectedRequest(httpMethod)) {
                logger.info(getContentAsString());
                while (getContentAsString().contains("\"captcha\":1")) {
                    stepCaptcha(recaptchaKey, requestUrl);
                }
                httpMethod = getMethodBuilder().setAction(fileURL).toGetMethod();
                //download page twice
                if (makeRedirectedRequest(httpMethod) && makeRedirectedRequest(httpMethod)) {
                    httpMethod = getMethodBuilder().setActionFromAHrefWhereATagContains("click here").toGetMethod();
                    if (!tryDownloadAndSaveFile(httpMethod)) {
                        checkProblems();
                        throw new ServiceConnectionProblemException("Error starting download");
                    }
                } else {
                    checkProblems();
                    throw new ServiceConnectionProblemException();
                }
            } else {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        if (content.contains("file removed") || content.contains("no such file") || content.contains("file expired")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        final Matcher matcher = getMatcherAgainstContent(
                "<span style=\"cursor: default; font-size: 110%; color: gray;\">\\s*([^<>]+?)\\s*&nbsp;\\s*<strong>\\s*([^<>]+?)\\s*</strong>");
        if (!matcher.find()) {
            throw new PluginImplementationException("File name/size not found");
        }
        httpFile.setFileName(matcher.group(1));
        httpFile.setFileSize(PlugUtils.getFileSizeFromString(matcher.group(2)));
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private String getVar(final String name) throws ErrorDuringDownloadingException {
        final Matcher matcher = getMatcherAgainstContent("var\\s+" + Pattern.quote(name) + "\\s*=\\s*'(.+?)'");
        if (!matcher.find()) {
            throw new PluginImplementationException("Var '" + name + "' not found");
        }
        return matcher.group(1);
    }

    private void stepCaptcha(final String recaptchaKey, final String requestUrl) throws Exception {
        HttpMethod httpMethod = getMethodBuilder().setAction("http://www.google.com/recaptcha/api/challenge?k=" + recaptchaKey).toGetMethod();
        if (!makeRedirectedRequest(httpMethod)) {
            throw new ServiceConnectionProblemException();
        }
        final Matcher matcher = PlugUtils.matcher("challenge\\s*?:\\s*?'(.+?)'", getContentAsString());
        if (!matcher.find()) {
            throw new PluginImplementationException("ReCaptcha challenge not found");
        }
        final String challenge = matcher.group(1);
        final String captcha = getCaptchaSupport().getCaptcha("http://www.google.com/recaptcha/api/image?c=" + challenge);
        if (captcha == null) {
            throw new CaptchaEntryInputMismatchException();
        }
        httpMethod = getMethodBuilder()
                .setAction(requestUrl)
                .setParameter("ctype", "recaptcha")
                .setParameter("recaptcha_response", URLEncoder.encode(captcha, "UTF-8"))
                .setParameter("recaptcha_challenge", challenge)
                .toPostMethod();
        if (!makeRedirectedRequest(httpMethod)) {
            throw new ServiceConnectionProblemException();
        }
    }

}
