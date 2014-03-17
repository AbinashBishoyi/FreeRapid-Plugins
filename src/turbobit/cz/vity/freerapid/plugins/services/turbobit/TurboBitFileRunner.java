package cz.vity.freerapid.plugins.services.turbobit;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;


/**
 * Class which contains main code
 *
 * @author Arthur Gunawan
 */
class TurboBitFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(TurboBitFileRunner.class.getName());
    private final static String mRef = "http://www.turbobit.net/en";


    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        client.setReferer(mRef);

        HttpMethod httpMethod = getMethodBuilder().setReferer(mRef).setAction(fileURL).toGetMethod();
        client.setReferer(mRef);
        if (makeRequest(httpMethod)) {
            checkProblems();
            checkNameAndSize(getContentAsString());//ok let's extract file name and size from the page
        } else
            throw new PluginImplementationException();
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, content, "&nbsp;</span><b>", "</b></h1>");
        PlugUtils.checkFileSize(httpFile, content, ":</b>", "</div>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);

        checkNameAndSize(getContentAsString());//ok let's extract file name and size from the page
        HttpMethod httpMethod = getMethodBuilder().setReferer(mRef).setAction(parseURL(fileURL)).toGetMethod();


        logger.info(getContentAsString());

        if (!tryDownloadAndSaveFile(httpMethod)) {
            checkProblems();//if downloading failed
            logger.warning(getContentAsString());//log the info
            throw new PluginImplementationException();//some unknown problem
        }

    }

    private void checkFileProblems() throws ErrorDuringDownloadingException {
        Matcher err404Matcher = PlugUtils.matcher("<div class=\"text-404\">(.*?)</div", getContentAsString());
        if (err404Matcher.find()) {
            if (err404Matcher.group(1).contains("Запро�?енный док�?мент не найден"))
                throw new URLNotAvailableAnymoreException(err404Matcher.group(1));
        }
        if (getContentAsString().contains("\u0424\u0430\u0439\u043B \u043D\u0435 \u043D\u0430\u0439\u0434\u0435\u043D."))
            throw new URLNotAvailableAnymoreException();
    }

    private void checkDownloadProblems() throws ErrorDuringDownloadingException {
        try {
            //TODO fix error message in unicode => MS Word - alt+x after each character + \\u before it
            Matcher waitMatcher = PlugUtils.matcher("Попроб�?йте\\s+повторить.*<span id='timeout'>([^>]*)<", getContentAsString());
            if (waitMatcher.find()) {
                throw new YouHaveToWaitException("You have to wait", Integer.valueOf(waitMatcher.group(1)));
            }
            Matcher errMatcher = PlugUtils.matcher("<div[^>]*class='error'[^>]*>([^<]*)<", getContentAsString());
            if (errMatcher.find() && !errMatcher.group(1).isEmpty()) {
                if (errMatcher.group(1).contains("Неверный ответ"))
                    throw new CaptchaEntryInputMismatchException();
                throw new PluginImplementationException();
            }
            if (getContentAsString().contains("С�?ылка про�?рочена")) // it's unlikely we get this...
                throw new YouHaveToWaitException("Trying again...", 10);
        } catch (NumberFormatException e) {
            throw new PluginImplementationException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        checkFileProblems();
        checkDownloadProblems();
    }

    private String parseURL(String myURL) throws Exception {

        Matcher matcher = PlugUtils.matcher("http://turbobit\\.net/([a-z0-9]+)\\.html", myURL);
        if (!matcher.find()) {
            checkProblems();
            throw new PluginImplementationException();
        }
        String myAction = "http://turbobit.net/download/timeout/" + matcher.group(1) + "/";


        HttpMethod httpMethod = getMethodBuilder().setReferer(mRef).setAction(myAction).toGetMethod();
        client.setReferer(mRef);
        String getRef = client.getReferer();
        logger.info("Get Referer : " + getRef);

        if (!makeRedirectedRequest(httpMethod)) {
            checkProblems();
            throw new PluginImplementationException();
        }

        String contentAsString = getContentAsString();
        if (!contentAsString.contains("download/redirect/")) {
            checkProblems();
            throw new PluginImplementationException();
        }

        String finURL = "http://turbobit.net/download/redirect/" + PlugUtils.getStringBetween(contentAsString, "/download/redirect/", "'");
        logger.info("Final URL: " + finURL);

        return finURL;


    }


}