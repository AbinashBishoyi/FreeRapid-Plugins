package cz.vity.freerapid.plugins.services.filepi;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.recaptcha.ReCaptcha;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.Random;
import java.util.logging.Logger;

/**
 * Class which contains main code
 *
 * @author tong2shot
 * @since 0.9u2
 */
class FilePiFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(FilePiFileRunner.class.getName());

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
        try {
            String filename = PlugUtils.getStringBetween(content, "File Name:</p>", "</p>").replace("<p class=\"text-lower\">", "").trim();
            logger.info("File name " + filename);
            httpFile.setFileName(filename);
        } catch (PluginImplementationException e) {
            throw new PluginImplementationException("File name not found");
        }

        try {
            long filesize = PlugUtils.getFileSizeFromString(PlugUtils.getStringBetween(content, "File Size:</p>", "</p>").replace("<p class=\"text-lower\">", "").trim());
            logger.info("File size " + filesize);
            httpFile.setFileSize(filesize);
        } catch (PluginImplementationException e) {
            throw new PluginImplementationException("File size not found");
        }

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
            /*
            String tag = PlugUtils.getStringBetween(getContentAsString(), "tag = '", "';");
            String hash = PlugUtils.getStringBetween(getContentAsString(), "hash = '", "';");
            HttpMethod httpMethod;
            do {
                MethodBuilder mb = getMethodBuilder()
                        .setReferer(fileURL)
                        .setAction("http://filepi.com/ajax-re")
                        .setParameter("tag", tag)
                        .setParameter("pass", hash)
                        .setParameter("hash", hash)
                        .setParameter("Content-Type","application/x-www-form-urlencoded; charset=UTF-8")
                        .setAjax();
                handleCaptcha(mb);
                httpMethod = mb.toPostMethod();
                if (!makeRedirectedRequest(httpMethod)) {
                    checkProblems();
                    throw new ServiceConnectionProblemException();
                }
                checkProblems();
            } while (getContentAsString().contains("\"error\":\"captcha\""));
            String re_time = PlugUtils.getStringBetween(getContentAsString(), "\"re_time\":", ",");
            String re_hash = PlugUtils.getStringBetween(getContentAsString(), "\"re_hash\":\"", "\"");
            httpMethod = getMethodBuilder()
                    .setReferer(fileURL)
                    .setAction("http://filepi.com/ajax-down")
                    .setParameter("tag", tag)
                    .setParameter("pass", hash)
                    .setParameter("re_time", re_time)
                    .setParameter("re_hash", re_hash)
                    .setParameter("Content-Type","application/x-www-form-urlencoded; charset=UTF-8")
                    .setAjax()
                    .toPostMethod();
            downloadTask.sleep(6);
            if (!makeRedirectedRequest(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            checkProblems();
            String downloadUrl = PlugUtils.getStringBetween(getContentAsString(),"\"url\":\"","\"").replace("\\/","/");
            */

            //set referer to it-ebooks to enable direct download.
            //otherwise, uncomment codes above.
            HttpMethod httpMethod = getMethodBuilder()
                    .setReferer(String.format("http://it-ebooks.info/book/%d/", new Random().nextInt(2000) + 1000))
                    .setAction(fileURL)
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

    private void handleCaptcha(MethodBuilder mb) throws Exception {
        final String reCaptchaKey = "6Le-NuISAAAAAAkCn-5FeXkZTLcdkzXIofpDopR7";
        ReCaptcha r = new ReCaptcha(reCaptchaKey, client);
        final String captcha = getCaptchaSupport().getCaptcha(r.getImageURL());
        if (captcha == null) {
            throw new CaptchaEntryInputMismatchException();
        }
        mb.setParameter("cap1", r.getChallenge());
        mb.setParameter("cap2", captcha);
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("File not found or deleted")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

}