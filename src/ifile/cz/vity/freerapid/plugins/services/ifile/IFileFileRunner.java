package cz.vity.freerapid.plugins.services.ifile;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.recaptcha.ReCaptcha;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author JPEXS, ntoskrnl, tong2shot
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
            final String _recaptchaKey = getVar("__recaptcha_public", true);
            final String _requestUrl = "/new_download-request.json";
            final String _ukey = getVar("__ukey", true);
            final String _ab = getVar("__ab", false);
            logger.info(_ukey + " " + _ab);
            httpMethod = getMethodBuilder()
                    .setReferer(fileURL)
                    .setAction(_requestUrl)
                    .setParameter("ukey", _ukey)
                    .setParameter("ab", _ab)
                    .toPostMethod();
            httpMethod.addRequestHeader("X-Requested-With", "XMLHttpRequest");
            setFileStreamContentTypes(new String[0], new String[]{"application/json"});
            if (makeRedirectedRequest(httpMethod)) {
                while (getContentAsString().contains("\"captcha\":1")) {
                    logger.info(getContentAsString());
                    stepCaptcha(_recaptchaKey, _requestUrl, _ukey, _ab);
                }
                final String ticketURL = PlugUtils.getStringBetween(getContentAsString(), "\"ticket_url\":\"", "\"").replaceAll("\\\\", "");
                httpMethod = getMethodBuilder()
                        .setReferer(fileURL)
                        .setAction(ticketURL)
                        .toGetMethod();
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
        final String fileName = matcher.group(1).trim();
        if (fileName.equals("")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        httpFile.setFileName(matcher.group(1));
        httpFile.setFileSize(PlugUtils.getFileSizeFromString(matcher.group(2)));
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private String getVar(final String name, final boolean inSingleQuote) throws ErrorDuringDownloadingException {
        String regexp;
        if (inSingleQuote) {
            regexp = "var\\s+" + Pattern.quote(name) + "\\s*=\\s*'(.+?)'";
        } else {
            regexp = "var\\s+" + Pattern.quote(name) + "\\s*=\\s*(.+?)";
        }
        final Matcher matcher = getMatcherAgainstContent(regexp);
        if (!matcher.find()) {
            throw new PluginImplementationException("Var '" + name + "' not found");
        }
        return matcher.group(1);
    }

    private void stepCaptcha(final String recaptchaKey, final String requestUrl, final String ukey, final String ab) throws Exception {
        final ReCaptcha r = new ReCaptcha(recaptchaKey, client);
        final String captcha = getCaptchaSupport().getCaptcha(r.getImageURL());
        if (captcha == null) {
            throw new CaptchaEntryInputMismatchException();
        }
        r.setRecognized(captcha);
        final String captchaChallenge = PlugUtils.getStringBetween(r.getResponseParams(), "recaptcha_challenge_field=", "&recaptcha_response_field=");
        final MethodBuilder methodBuilder = getMethodBuilder()
                .setReferer(fileURL)
                .setAction(requestUrl)
                .setParameter("ukey", ukey)
                .setParameter("ab", ab)
                .setParameter("ctype", "recaptcha")
                .setParameter("recaptcha_response", captcha)
                .setParameter("recaptcha_challenge", captchaChallenge);
        final HttpMethod httpMethod = methodBuilder.toPostMethod();
        httpMethod.addRequestHeader("X-Requested-With", "XMLHttpRequest");
        if (!makeRedirectedRequest(httpMethod)) {
            throw new ServiceConnectionProblemException();
        }

    }

}
