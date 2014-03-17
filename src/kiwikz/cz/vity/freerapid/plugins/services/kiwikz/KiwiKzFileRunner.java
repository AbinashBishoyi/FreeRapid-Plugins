package cz.vity.freerapid.plugins.services.kiwikz;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
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
class KiwiKzFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(KiwiKzFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
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
        PlugUtils.checkName(httpFile, content, "<title>", "</title>");
        final Matcher matcher = getMatcherAgainstContent("<a href.+?>Скачать</a></div>\\s*<div>.*?\\((.+?), (.+?)\\)</div>");
        if (!matcher.find()) {
            throw new PluginImplementationException("File size not found");
        }
        httpFile.setFileName(httpFile.getFileName() + "." + matcher.group(1).trim());
        httpFile.setFileSize(PlugUtils.getFileSizeFromString(matcher.group(2).trim()));
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            final String contentAsString = getContentAsString();
            checkProblems();
            checkNameAndSize(contentAsString);
            HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL).setActionFromAHrefWhereATagContains("Скачать").toGetMethod();
            if (!makeRedirectedRequest(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            checkProblems();
            final String downloadPageURL = httpMethod.getURI().toString();
            final String downloadPageContent = getContentAsString();
            Matcher matcher = getMatcherAgainstContent("<script type=\"text/javascript\" src=\"(http://kiwi.kz/js/watch/download.js.*?)\"></script>");
            if (!matcher.find()) {
                throw new PluginImplementationException("Download javascript not found");
            }
            httpMethod = getMethodBuilder()
                    .setReferer(downloadPageURL)
                    .setAction(matcher.group(1))
                    .toGetMethod();
            setFileStreamContentTypes(new String[0], new String[]{"application/x-javascript", "application/json"});
            if (!makeRedirectedRequest(httpMethod)) {
                throw new ServiceConnectionProblemException();
            }
            final int waitTime = PlugUtils.getNumberBetween(getContentAsString(), "new Timer('download-waiter-remain',", ",");
            httpMethod = getMethodBuilder(downloadPageContent)
                    .setReferer(downloadPageURL)
                    .setActionFromFormByName("download-captcha-form", true)
                    .setAction("/services/watch/download")
                    .toPostMethod();
            httpMethod.setRequestHeader("X-Requested-With", "XMLHttpRequest");
            httpMethod.setRequestHeader("X-Prototype-Version", "1.7");
            httpMethod.setRequestHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            if (!makeRedirectedRequest(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            checkProblems();
            final String downloadURL = PlugUtils.getStringBetween(getContentAsString(), "url\":\"", "\"").replace("\\/", "/");
            downloadTask.sleep(waitTime + 1);
            httpMethod = getMethodBuilder()
                    .setReferer(downloadPageURL)
                    .setAction(downloadURL)
                    .toGetMethod();
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
        if (contentAsString.contains("Данный ролик не существует")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

}