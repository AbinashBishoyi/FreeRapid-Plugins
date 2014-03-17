/*
 * PLEASE, READ THIS
 *
 * This file is in UTF-8 encoding
 *
 * If you want to edit it, you can edit it as ASCII provided
 * you don't touch the UTF-8 characters and don't change the encoding
 *
 * If you want to compile it, set the encoding in your IDE or
 * use the -encoding option to javac
 *
 * If you want to build this plugin, use build.xml which already
 * deals with this
 *
 * Otherwise, don't touch the strings if you can't check their meaning
 */
package cz.vity.freerapid.plugins.services.narod;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.io.IOException;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author Thumb
 */
class NarodFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(NarodFileRunner.class.getName());


    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);//make first request
        if (makeRedirectedRequest(getMethod)) {
            checkFileProblems();
            checkNameAndSize();//ok let's extract file name and size from the page
        } else
            throw new ServiceConnectionProblemException();
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        Matcher nameMatcher = PlugUtils.matcher("<dt class=\"name\">(.*?)</dt>", getContentAsString());
        if (!nameMatcher.find())
            unimplemented();
        Matcher namePartMatcher = PlugUtils.matcher("<[^>]*>|\\s", nameMatcher.group(1));
        httpFile.setFileName(namePartMatcher.replaceAll(""));


        Matcher sizeMatcher = PlugUtils.matcher("Размер:(?:<[^>]*>|\\s)*(.*?)<", getContentAsString());
        if (!sizeMatcher.find())
            unimplemented();
        httpFile.setFileSize(PlugUtils.getFileSizeFromString(sizeMatcher.group(1).replace('\u0411', 'B')));
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);

        runCheck();
        checkDownloadProblems();

        HttpMethod method1 = addCaptchaPrameters(getMethodBuilder()
                .setActionFromFormByName("f-capchaform", true)
                .setAction(fileURL))
                .setParameter("action", "sendcapcha")
                .toPostMethod();

        if (!makeRequest(method1))
            throw new ServiceConnectionProblemException();
        checkProblems();

        HttpMethod method2 = getMethodBuilder()
                .setActionFromAHrefWhereATagContains("http://narod.ru")
                .setBaseURL("http://narod.ru")
                .toGetMethod();

        //here is the download link extraction
        if (!tryDownloadAndSaveFile(method2)) {
            checkProblems();//if downloading failed
            unimplemented();
        }
    }

    private void unimplemented() throws PluginImplementationException {
        logger.warning(getContentAsString());//log the info
        throw new PluginImplementationException();//some unknown problem
    }

    private MethodBuilder addCaptchaPrameters(MethodBuilder builder) throws ErrorDuringDownloadingException, IOException {
        HttpMethod keyRequest = getMethodBuilder()
                .setAction("http://narod.ru/disk/getcapchaxml/?rnd=" + (int) (Math.random() * 777))
                .toGetMethod();

        if (!makeRequest(keyRequest))
            throw new ServiceConnectionProblemException();

        CaptchaSupport cs = getCaptchaSupport();

        Matcher numberMatcher = PlugUtils.matcher("<number\\s+url=\"([^\"]*)\"[^>]*>([^<]*)<", getContentAsString());
        if (!numberMatcher.find())
            unimplemented();

        return builder.setParameter("key", numberMatcher.group(2))
                .setParameter("rep", cs.getCaptcha(numberMatcher.group(1)));

    }

    private void checkFileProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("<td class=\"headCode\">404</td>")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
        if (contentAsString.contains("\u0424\u0430\u0439\u043B \u0443\u0434\u0430\u043B\u0435\u043D \u0441 \u0441\u0435\u0440\u0432\u0438\u0441\u0430."))
            throw new URLNotAvailableAnymoreException("File deleted");
    }

    private void checkDownloadProblems() throws ErrorDuringDownloadingException {
        if (getContentAsString().contains("<b class=\"error-msg\"><strong>\u041E\u0448\u0438\u0431\u043B\u0438\u0441\u044C?</strong> " + "\u041F\u043E\u043F\u0440\u043E\u0431\u0443\u0439\u0442\u0435 \u0435\u0449\u0435" + "&nbsp;\u0440\u0430\u0437</b>"))
            throw new CaptchaEntryInputMismatchException();
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        checkDownloadProblems();
        checkFileProblems();
        if (getContentAsString().contains("\u0412\u043D\u0443\u0442\u0440\u0435\u043D\u043D\u044F\u044F \u043E\u0448\u0438\u0431\u043A\u0430 \u0441\u0435\u0440\u0432\u0438\u0441\u0430.")) //Внутренняя ошибка сервиса.
            throw new ServiceConnectionProblemException();
    }

}
