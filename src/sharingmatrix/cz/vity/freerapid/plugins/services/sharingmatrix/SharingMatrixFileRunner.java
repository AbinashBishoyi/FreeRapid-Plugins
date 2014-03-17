package cz.vity.freerapid.plugins.services.sharingmatrix;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import cz.vity.freerapid.utilities.LogUtils;
import org.apache.commons.httpclient.HttpMethod;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * @author Kajda, JPEXS, RickCL, codeboy2k
 * @since 0.82
 */
class SharingMatrixFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(SharingMatrixFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final HttpMethod httpMethod = getGetMethod(fileURL);

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
        HttpMethod httpMethod = getGetMethod(fileURL);

        if (makeRedirectedRequest(httpMethod)) {
            checkProblems();
            checkNameAndSize();
            String contentAsString = getContentAsString();
            final String rootURL = PlugUtils.getStringBetween(contentAsString, "URL_ROOT = '", "'");
            final String linkId = PlugUtils.getStringBetween(contentAsString, "link_id = '", "'");
            final String linkName = PlugUtils.getStringBetween(contentAsString, "link_name = '", "'");
            String captchaURL = rootURL + "/images/captcha/";
            boolean waitTimer;
            String dlID = "";
            int forcedWaitTime = 0;

            do {
                waitTimer = false;
                httpMethod = getMethodBuilder().setReferer(fileURL).setAction(rootURL + "/ajax_scripts/download.php?type_membership=free&link_id=" + linkId).toGetMethod();
                if (makeRedirectedRequest(httpMethod)) {
                    // wait time can be 1,5,10,15 or 45 mins (in seconds). After 45 mins, free users must wait a whole day before they can download again
                    int ctjv = PlugUtils.getWaitTimeBetween(getContentAsString(), "ctjv = '", "';", TimeUnit.SECONDS);
                    if (forcedWaitTime != 0) {
                        // when sharing matrix is busy, it tells us to wait 60 seconds, but if we do that, we have to
                        // refresh the captcha, ask for user to input the captcha, and then it fails again
                        // and tells us to wait another 60 seconds.  It's annoying to enter the captcha every 60
                        // seconds for 3 hours or more. This forcedWaitTime recognizes this situation and
                        // forces a 1 hour wait when the sharingmatrix servers are busy.  Then you only
                        // have to enter a captcha every hour (servers may still be busy, but it's far less annoying)
                        downloadTask.sleep(forcedWaitTime);
                        forcedWaitTime = 0;
                    } else {
                        downloadTask.sleep(ctjv);
                    }
                    try {
                        captchaURL = PlugUtils.getStringBetween(getContentAsString(), "CAPTCHA_IMAGE_URL = '", "'");
                    } catch (PluginImplementationException e) {
                        LogUtils.processException(logger, e);
                    }
                    if (getContentAsString().contains("_get2.php")) {
                        dlID = PlugUtils.getStringBetween(getContentAsString(), "showLink(", ", '');");
                        httpMethod = getMethodBuilder().setReferer(fileURL).setAction(rootURL + "/ajax_scripts/_get2.php?link_id=" + linkId + "&link_name=" + linkName + "&dl_id=" + dlID + "&password=").toGetMethod();
                    } else {
                        boolean refreshCaptcha = false;
                        InputStream is;
                        BufferedImage captchaImage = null;
                        CaptchaSupport captchaSupport = getCaptchaSupport();
                        if (getContentAsString().contains("check_timer.php")) {
                            httpMethod = getMethodBuilder().setReferer(fileURL).setAction(rootURL + "/ajax_scripts/check_timer.php?tmp=" + (new Random()).nextInt()).toGetMethod();
                            if (!makeRedirectedRequest(httpMethod)) {
                                throw new ServiceConnectionProblemException();
                            }
                            refreshCaptcha = true;
                        } else {
                            httpMethod = getMethodBuilder().setReferer(fileURL).setAction(rootURL + "/include/crypt/cryptographp.php?cfg=0&").toGetMethod();
                            httpMethod.setFollowRedirects(true);
                            is = client.makeRequestForFile(httpMethod);

                            if (is == null) {
                                throw new PluginImplementationException();
                            }

                            captchaImage = captchaSupport.loadCaptcha(is);
                        }
                        String captchaR;
                        do {
                            if (refreshCaptcha) {
                                httpMethod = getMethodBuilder().setReferer(fileURL).setAction(rootURL + "/ajax_scripts/reload_captcha.php").toGetMethod();
                                if (!makeRedirectedRequest(httpMethod)) {
                                    throw new PluginImplementationException();
                                }
                                httpMethod = getMethodBuilder().setReferer(fileURL).setAction(captchaURL + getContentAsString().trim() + ".jpg").toGetMethod();
                                is = client.makeRequestForFile(httpMethod);
                                if (is == null) {
                                    throw new PluginImplementationException(httpMethod.getURI().toString());
                                }
                                captchaImage = captchaSupport.loadCaptcha(is);
                            }
                            captchaR = captchaSupport.askForCaptcha(captchaImage);
                            if (captchaR == null) {
                                throw new CaptchaEntryInputMismatchException();
                            }

                            httpMethod = getMethodBuilder().setReferer(fileURL).setAction(rootURL + "/ajax_scripts/verifier.php").setParameter("code", captchaR).toPostMethod();
                            if (!makeRedirectedRequest(httpMethod)) {
                                throw new ServiceConnectionProblemException();
                            }

                        } while (!getContentAsString().trim().equals("1"));
                        httpMethod = getMethodBuilder().setReferer(fileURL).setAction(rootURL + "/ajax_scripts/dl.php").toGetMethod();
                        if (makeRedirectedRequest(httpMethod)) {
                            dlID = getContentAsString();
                        }
                        httpMethod = getMethodBuilder().setReferer(fileURL).setAction(rootURL + "/ajax_scripts/_get.php?link_id=" + linkId + "&link_name=" + linkName + "&dl_id=" + dlID + "&password=").toGetMethod();
                    }
                    if (makeRedirectedRequest(httpMethod)) {
                        contentAsString = getContentAsString();
                        if (contentAsString.contains("Wait timer!")) {
                            waitTimer = true;
                        }
                        if (contentAsString.contains("You can not download now") ||
                                contentAsString.contains("quota has been reached")) {
                            waitTimer = true;
                            forcedWaitTime = 3600;
                        }
                    } else {
                        throw new ServiceConnectionProblemException();
                    }
                } else {
                    throw new ServiceConnectionProblemException();
                }
            } while (waitTimer);

            httpMethod = getMethodBuilder()
                    .setReferer(fileURL)
                    .setAction("/download/" + PlugUtils.getStringBetween(contentAsString, "hash:\"", "\"") + "/" + dlID + "/")
                    .setBaseURL(PlugUtils.getStringBetween(contentAsString, "{serv:\"", "\""))
                    .toGetMethod();

            if (!tryDownloadAndSaveFile(httpMethod)) {
                checkProblems();
                logger.warning(getContentAsString());
                throw new ServiceConnectionProblemException("Error starting download");
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();

        if (contentAsString.contains("File not found")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        if (contentAsString.contains("File has been deleted")) {
            throw new URLNotAvailableAnymoreException("File has been deleted");
        }
        if (contentAsString.contains("no available free download slots left for your country")) {
            throw new ServiceConnectionProblemException("There are no available free download slots left for your country today");
        }
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString().replace("&nbsp;", " ");
        try {
            PlugUtils.checkName(httpFile, contentAsString, "Filename:\n<strong>", "</strong>");
        } catch (PluginImplementationException e1) {
            try {
                PlugUtils.checkName(httpFile, contentAsString, "link_name\n=\n'", "'");
            } catch (PluginImplementationException e2) {
                throw new PluginImplementationException("File name not found");
            }
        }
        try {
            PlugUtils.checkFileSize(httpFile, contentAsString, "File size:\n<strong>", "</strong>");
        } catch (PluginImplementationException e1) {
            try {
                PlugUtils.checkFileSize(httpFile, contentAsString, "fsize\n=\n'", "'");
            } catch (PluginImplementationException e2) {
                throw new PluginImplementationException("File size not found");
            }
        }
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }
}
