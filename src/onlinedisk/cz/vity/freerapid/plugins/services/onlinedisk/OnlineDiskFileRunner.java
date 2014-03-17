package cz.vity.freerapid.plugins.services.onlinedisk;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author tong2shot
 */
class OnlineDiskFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(OnlineDiskFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        setPageEncoding("windows-1251");
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize(getContentAsString());
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        final Matcher fileNameMatcher = getMatcherAgainstContent("Файл:\\s*<b.*?>(.+?)<");
        if (!fileNameMatcher.find()) {
            throw new PluginImplementationException("File name not found");
        }
        httpFile.setFileName(fileNameMatcher.group(1));
        final long fileSize = PlugUtils.getFileSizeFromString(PlugUtils.getStringBetween(content,"Размер файла (File size) :</b>","<").replace("Гб", "gb").replace("мб", "mb").replace("Кб", "kb").replace("байт", "b"));
        httpFile.setFileSize(fileSize);
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        setPageEncoding("windows-1251");
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            final String contentAsString = getContentAsString();
            checkProblems();
            checkNameAndSize(contentAsString);
            HttpMethod httpMethod;
            while (getContentAsString().contains("class='captcha'")) {
                MethodBuilder methodBuilder = getMethodBuilder()
                        .setReferer(fileURL)
                        .setActionFromFormWhereTagContains("class='captcha'",true)
                        .setAction(fileURL);
                httpMethod = stepCaptcha(methodBuilder);
                if (!makeRedirectedRequest(httpMethod)) {
                    checkProblems();
                    throw new ServiceConnectionProblemException();
                }
            }
            checkProblems();

            httpMethod = getMethodBuilder()
                    .setReferer(fileURL)
                    .setActionFromFormWhereTagContains("name='download'",true)
                    .setAction(fileURL)
                    .toPostMethod();
            if (!tryDownloadAndSaveFile(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException("Error starting download");
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("был удален")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    private HttpMethod stepCaptcha(MethodBuilder methodBuilder) throws Exception {
        final Matcher captchaImageUrlMatcher = getMatcherAgainstContent("<img src='(http://www\\.onlinedisk\\.ru//safe/index\\.php\\?.+?)'");
        if (!captchaImageUrlMatcher.find())
            throw new PluginImplementationException("Captcha not found");
        final String captcha = getCaptchaSupport().getCaptcha(captchaImageUrlMatcher.group(1));
        if (captcha == null) {
            throw new CaptchaEntryInputMismatchException();
        }
        return methodBuilder.setParameter("kaptcha",captcha).toPostMethod();
    }

}