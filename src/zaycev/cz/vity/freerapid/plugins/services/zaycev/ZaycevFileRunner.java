package cz.vity.freerapid.plugins.services.zaycev;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class ZaycevFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(ZaycevFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);//make first request
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize(getContentAsString());//ok let's extract file name and size from the page
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        final Matcher match = PlugUtils.matcher("<div class=\"unit-w\"><div class=\"box\"><h6>(.+?) [^ ]+? [^ ]+?</h6>", content);
        if (!match.find())
            throw new PluginImplementationException("File size not found");
        httpFile.setFileName(match.group(1).trim());
        httpFile.setFileSize(PlugUtils.getFileSizeFromString(PlugUtils.getStringBetween(PlugUtils.getStringBetween(content, "<li class=\"track-file-info__size\">", "/li>"), ":", "<")));
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            final String contentAsString = getContentAsString();//check for response
            checkProblems();//check problems
            checkNameAndSize(contentAsString);//extract file name and size from the page

            final Matcher match = PlugUtils.matcher("<h3.*?><a href=\"(/download.php\\?id.+?)\"", contentAsString);
            if (!match.find())
                throw new PluginImplementationException("Validation page not found");
            final HttpMethod hmGetNextPage = getGetMethod("http://zaycev.net" + match.group(1));
            if (!makeRedirectedRequest(hmGetNextPage)) {
                checkProblems();
                throw new PluginImplementationException("Problem loading validation page");
            }
            checkProblems();
            PlugUtils.checkName(httpFile, getContentAsString(), "type=\"hidden\"/><input value=\"", "\" name=\"ass\"");

            do {
                final String captchaID = "" + PlugUtils.getNumberBetween(getContentAsString(), "<input value=\"", "\" name=\"captchaId\"");

                final String captchaImg = "http://zaycev.net/captcha/" + captchaID + "/";
                final CaptchaSupport captchaSupport = getCaptchaSupport();
                final String captchaTxt = captchaSupport.getCaptcha(captchaImg);

                final HttpMethod hmGetLink = getMethodBuilder().setReferer(fileURL)
                        .setActionFromFormWhereActionContains("download.php", true)
                        .setParameter("text_check", captchaTxt)
                        .toGetMethod();
                if (!makeRedirectedRequest(hmGetLink)) {
                    checkProblems();
                    throw new PluginImplementationException("Problem loading validation page");
                }
            } while (getContentAsString().contains("Вы ввели неверный код"));
            checkProblems();

            final HttpMethod httpMethod = getGetMethod(PlugUtils.getStringBetween(getContentAsString(), "<meta content=\"2; URL=", "\""));
            if (!tryDownloadAndSaveFile(httpMethod)) {
                checkProblems();//if downloading failed
                throw new ServiceConnectionProblemException("Error starting download");//some unknown problem
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("Страница не найдена")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }

}