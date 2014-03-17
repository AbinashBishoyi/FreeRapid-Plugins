package cz.vity.freerapid.plugins.services.prefiles;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.recaptcha.ReCaptcha;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;

/**
 * Class which contains main code
 *
 * @author CrazyCoder
 */
class PreFilesFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(PreFilesFileRunner.class.getName());

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
        PlugUtils.checkName(httpFile, content, "Filename:</td><td><strong>", "</strong></td></tr>");
        PlugUtils.checkFileSize(httpFile, content, "Size:</td><td><strong>", "</strong>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            String content = getContentAsString();
            checkProblems();
            checkNameAndSize(content);

            fileURL = method.getURI().toString();

            final String id = PlugUtils.getStringBetween(content, "<input type=\"hidden\" name=\"id\" value=\"", "\">");
            final String fname = PlugUtils.getStringBetween(content, "<input type=\"hidden\" name=\"fname\" value=\"", "\">");

            HttpMethod post = getMethodBuilder()
                    .setParameter("op", "download1")
                    .setParameter("usr_login", "")
                    .setParameter("id", id)
                    .setParameter("fname", fname)
                    .setParameter("referer", fileURL)
                    .setParameter("method_free", "method_free")
                    .setAction(fileURL)
                    .setReferer(fileURL)
                    .toPostMethod();

            if (!makeRedirectedRequest(post)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }

            checkProblems();
            content = getContentAsString();

            long now = System.currentTimeMillis();
            int waitTime = 99;
            try {
                final String spanId = PlugUtils.getStringBetween(content, "<span id=\"countdown_str\">Please wait <span id=\"", "\"");
                logger.info("span id: " + spanId);
                waitTime = PlugUtils.getNumberBetween(content, "<span id=\"" + spanId + "\">", "</span>");
                logger.info("wait: " + waitTime);
            } catch (PluginImplementationException ignored) {
                logger.warning("Can't get wait time, using default (99 seconds)");
            }

            final String captchaKey = PlugUtils.getStringBetween(content, "challenge?k=", "\">");
            logger.info("Captcha key: " + captchaKey);

            // show captcha
            final ReCaptcha r = new ReCaptcha(captchaKey, client);
            final String captcha = getCaptchaSupport().getCaptcha(r.getImageURL());
            if (captcha == null) {
                throw new CaptchaEntryInputMismatchException();
            }
            r.setRecognized(captcha);

            final String rand = PlugUtils.getStringBetween(content, "<input type=\"hidden\" name=\"rand\" value=\"", "\">");

            final MethodBuilder methodBuilder = getMethodBuilder()
                    .setParameter("op", "download2")
                    .setParameter("id", id)
                    .setParameter("fname", fname)
                    .setParameter("referer", fileURL)
                    .setParameter("rand", rand)
                    .setParameter("method_free", "method_free")
                    .setParameter("method_premium", "")
                    .setParameter("down_script", "1")
                    .setParameter("submit", "btn_download")
                    .setReferer(fileURL)
                    .setAction(fileURL);

            post = r.modifyResponseMethod(methodBuilder).toPostMethod();

            long timeSpent = System.currentTimeMillis() - now;
            waitTime -= timeSpent / 1000;

            if (waitTime > 0) {
                logger.info("wait left: " + waitTime);
                downloadTask.sleep(waitTime);
            }

            if (client.makeRequest(post, false) != HttpStatus.SC_MOVED_TEMPORARILY) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }

            final Header hLocation = post.getResponseHeader("Location");

            if (hLocation == null) {
                throw new ServiceConnectionProblemException("Error starting download");
            }

            final String location = hLocation.getValue();
            logger.info("Location: " + location);

            post = getMethodBuilder().setReferer(fileURL).setAction(location).toGetMethod();
            setFileStreamContentTypes("\"application/octet-stream\"");
            if (!tryDownloadAndSaveFile(post)) {
                checkProblems();
                throw new ServiceConnectionProblemException("Error starting download");
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private int getWaitTime(String content) {
        try {
            int minutes = PlugUtils.getNumberBetween(content, "You have to wait", "minutes");
            int seconds = PlugUtils.getNumberBetween(content, "minutes,", "seconds");
            return minutes * 60 + seconds;
        } catch (PluginImplementationException ignored) {
            try {
                return PlugUtils.getNumberBetween(content, "You have to wait", "seconds");
            } catch (PluginImplementationException ignore) {
                return -1;
            }
        }
    }


    private void checkProblems() throws ErrorDuringDownloadingException {
        final String c = getContentAsString();
        if (c.contains("File Not Found")) {
            throw new URLNotAvailableAnymoreException("File not found");
        } else if (c.contains("You have to wait")) {
            throw new YouHaveToWaitException("You have to wait", getWaitTime(c));
        } else if (c.contains("Wrong captcha")) {
            throw new CaptchaEntryInputMismatchException();
        } else if (c.contains("PREMIUM-Member only") || c.contains("You can download files up to")) {
            throw new NotSupportedDownloadByServiceException("PREMIUM file");
        }
    }
}
