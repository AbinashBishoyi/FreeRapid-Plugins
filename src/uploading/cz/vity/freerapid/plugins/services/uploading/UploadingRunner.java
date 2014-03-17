package cz.vity.freerapid.plugins.services.uploading;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.io.IOException;
import java.util.Calendar;
import java.util.Random;
import java.util.logging.Logger;

/**
 * @author Ladislav Vitasek, Ludek Zika
 */
class UploadingRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(UploadingRunner.class.getName());

    public UploadingRunner() {
        super();
    }

    public void runCheck() throws Exception {
        super.runCheck();
        addCookie(new Cookie(".uploading.com", "setlang", "en", "/", 86400, false));
        addCookie(new Cookie(".uploading.com", "_lang", "en", "/", 86400, false));
        addCookie(new Cookie(".uploading.com", "lang", "1", "/", 86400, false));
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkNameAndSize(getContentAsString());
        } else
            throw new PluginImplementationException();
    }

    public void run() throws Exception {
        super.run();
        addCookie(new Cookie(".uploading.com", "setlang", "en", "/", 86400, false));
        addCookie(new Cookie(".uploading.com", "_lang", "en", "/", 86400, false));
        addCookie(new Cookie(".uploading.com", "lang", "1", "/", 86400, false));
        final GetMethod getMethod = getGetMethod(fileURL);
        getMethod.setFollowRedirects(true);
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize(getContentAsString());
            client.setReferer(fileURL);
            HttpMethod method = getMethodBuilder().setReferer(fileURL).setActionFromFormByName("downloadform", true).toHttpMethod();
            if (makeRedirectedRequest(method)) {
                checkProblems();
                final String fileId = PlugUtils.getStringBetween(getContentAsString(), "file_id: ", ", ");
                method = getMethodBuilder().setAction("http://uploading.com/files/get/?JsHttpRequest=" + new Random().nextInt(5000000)).setParameter("file_id", fileId).setParameter("action", "get_link").toHttpMethod();
                final int wait = PlugUtils.getNumberBetween(getContentAsString(), "start_timer(", ")");
                this.downloadTask.sleep(wait + 1);
                if (makeRedirectedRequest(method)) {
                    checkProblems();
                    logger.info("Ajax response:" + getContentAsString());
                    if (!getContentAsString().contains("\"link\""))
                        throw new PluginImplementationException("Download link not found");
                    final String link = "http:" + PlugUtils.getStringBetween(getContentAsString(), "\"link\": \"http:", "\" } }").replaceAll("\\\\/", "/");
                    logger.info("Link:" + link);
                    method = getMethodBuilder().setAction(link).toHttpMethod();
                    if (!tryDownloadAndSaveFile(method)) {
                        checkProblems();
                        logger.warning(getContentAsString());
                        throw new IOException("File input stream is empty.");
                    }
                } else {
                    logger.info(getContentAsString());
                    throw new PluginImplementationException();
                }
            } else {
                logger.info(getContentAsString());
                throw new PluginImplementationException();
            }
        } else
            throw new PluginImplementationException();
    }

    private void checkNameAndSize(String content) throws Exception {

        if (!content.contains("uploading.com")) {
            logger.warning(content);
            throw new InvalidURLOrServiceProblemException("Invalid URL or unindentified service");
        }
        checkProblems();

        PlugUtils.checkFileSize(httpFile, getContentAsString(), "File size: <b>", "</b> <br/>");
        PlugUtils.checkName(httpFile, getContentAsString(), "<h2>", "</h2><br/>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }


    private void checkProblems() throws ServiceConnectionProblemException, YouHaveToWaitException, URLNotAvailableAnymoreException {
        if (getContentAsString().contains("Your IP address is currently downloading")) {
            throw new ServiceConnectionProblemException("Your IP address is currently downloading a file.\n" + "Please wait until the downloading process has been completed.");
        }
        if (getContentAsString().contains("You still need to wait for the start of your download")) {
            throw new YouHaveToWaitException("You still need to wait for the start of your download", 65);
        }
        if (getContentAsString().contains("Requested file not found")) {
            throw new URLNotAvailableAnymoreException("Requested file not found");
        }
        if (getContentAsString().contains("You have reached the daily downloads limit")) {
            int pause = 20 * 60;
            int toMidnight = getSecondToMidnight();
            if (toMidnight > 18 * 3600) pause = toMidnight + 5 * 60;

            throw new YouHaveToWaitException("You have reached the daily downloads limit. Please come back later", pause);
        }
        if (getContentAsString().contains("Requested file not found")) {
            throw new URLNotAvailableAnymoreException("Requested file not found");
        }

    }

    public static int getSecondToMidnight() {
        Calendar now = Calendar.getInstance();
        Calendar midnight = Calendar.getInstance();
        midnight.set(now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH) + 1, 0, 0, 1);
        return (int) ((midnight.getTimeInMillis() - now.getTimeInMillis()) / 1000f);
    }

}