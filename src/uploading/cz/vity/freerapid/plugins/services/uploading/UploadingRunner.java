package cz.vity.freerapid.plugins.services.uploading;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Calendar;
import java.util.logging.Logger;
import java.util.regex.Matcher;

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
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkNameAndSize(getContentAsString());
        } else
            throw new PluginImplementationException();
    }

    public void run() throws Exception {
        super.run();

        final GetMethod getMethod = getGetMethod(fileURL);
        getMethod.setFollowRedirects(true);
        if (makeRedirectedRequest(getMethod)) {
            if (getContentAsString().contains("downloadform")) {
                checkNameAndSize(getContentAsString());
                client.setReferer(fileURL);
                PostMethod method = getPostMethod(fileURL);
                method.addParameter("free", "1");
                if (makeRedirectedRequest(method)) {
                    if (!getContentAsString().contains("name=\"x\"")) {
                        logger.info(getContentAsString());
                        throw new PluginImplementationException();
                    }
                    int timeToWait = 92;
                    Matcher matcher = getMatcherAgainstContent("<script>\\s*var [^=]+=([0-9]+)");
                    if (matcher.find()) timeToWait = Integer.decode(matcher.group(1));
                    downloadTask.sleep(timeToWait);
                    PostMethod method2 = getPostMethod(fileURL);
                    method2.addParameter("free", "1");
                    method2.addParameter("x", "1");
                    if (!tryDownloadAndSaveFile(method2)) {
                        checkProblems();
                        logger.warning(getContentAsString());
                        throw new IOException("File input stream is empty.");
                    }

                } else {
                    logger.info(getContentAsString());
                    throw new PluginImplementationException();
                }
            } else {
                checkProblems();
                logger.info(getContentAsString());
                throw new PluginImplementationException();
            }
        } else
            throw new PluginImplementationException();
    }

    private String sicherName(String s) throws UnsupportedEncodingException {
        Matcher matcher = PlugUtils.matcher("(.*/)([^/]*)$", s);
        if (matcher.find()) {
            return matcher.group(2);
        }
        return "file01";
    }

    private void checkNameAndSize(String content) throws Exception {

        if (!content.contains("uploading.com")) {
            logger.warning(content);
            throw new InvalidURLOrServiceProblemException("Invalid URL or unindentified service");
        }
        checkProblems();

        Matcher matcher = PlugUtils.matcher("Download file </h.> <b>([^<]+)", content);
        // odebiram jmeno
        String fn;
        if (matcher.find()) {
            fn = matcher.group(1);
        } else fn = sicherName(fileURL);
        logger.info("File name " + fn);
        httpFile.setFileName(fn);
        // konec odebirani jmena

        matcher = PlugUtils.matcher("([0-9.]+ .B)<br/>", content);
        if (matcher.find()) {
            Long a = PlugUtils.getFileSizeFromString(matcher.group(1));
            logger.info("File size " + a);
            httpFile.setFileSize(a);
            httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
        }

    }


    private void checkProblems() throws ServiceConnectionProblemException, YouHaveToWaitException, URLNotAvailableAnymoreException {
        if (getContentAsString().contains("FILE REMOVED")) {
            throw new URLNotAvailableAnymoreException("File removed because of abuse or deleted by owner.");
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