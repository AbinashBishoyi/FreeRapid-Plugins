package cz.vity.freerapid.plugins.services.sharingmatrix;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author Kajda, JPEXS
 * @since 0.82
 */
class SharingMatrixFileRunner extends AbstractRunner {

    private final static Logger logger = Logger.getLogger(SharingMatrixFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final HttpMethod httpMethod = getMethodBuilder().setAction(fileURL).toHttpMethod();

        if (makeRedirectedRequest(httpMethod)) {
            checkSeriousProblems();
            checkNameAndSize();
        } else {
            throw new InvalidURLOrServiceProblemException("Invalid URL or service problem");
        }
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        HttpMethod httpMethod = getMethodBuilder().setAction(fileURL).toHttpMethod();

        if (makeRedirectedRequest(httpMethod)) {
            checkAllProblems();
            checkNameAndSize();
            String contentAsString = getContentAsString();
            final String rootURL = PlugUtils.getStringBetween(contentAsString, "URL_ROOT = '", "'");
            final String linkId = PlugUtils.getStringBetween(contentAsString, "link_id = '", "'");
            final String linkName = PlugUtils.getStringBetween(contentAsString, "link_name = '", "'");

            httpMethod = getMethodBuilder().setReferer(fileURL).setAction(rootURL + "/ajax_scripts/download.php?type_membership=free&link_id=" + linkId).toHttpMethod();
            if (makeRedirectedRequest(httpMethod)) {
                int ctjv = PlugUtils.getWaitTimeBetween(getContentAsString(), "ctjv = '", "';", TimeUnit.SECONDS);
                downloadTask.sleep(ctjv);
                String dlID = "";
                if (getContentAsString().contains("_get2.php")) {
                    dlID = PlugUtils.getStringBetween(getContentAsString(), "showLink(", ", '');");
                    httpMethod = getMethodBuilder().setReferer(fileURL).setAction(rootURL + "/ajax_scripts/_get2.php?link_id=" + linkId + "&link_name=" + linkName + "&dl_id=" + dlID + "&password=").toHttpMethod();
                } else {
                    boolean refreshCaptcha = false;
                    InputStream is;
                    BufferedImage captchaImage = null;
                    CaptchaSupport captchaSupport = getCaptchaSupport();
                    if( getContentAsString().contains("check_timer.php") ) {
                        httpMethod = getMethodBuilder().setReferer(fileURL).setAction(rootURL + "/ajax_scripts/check_timer.php?tmp=" + (new Random()).nextInt() ).toGetMethod();
                        if(!makeRequest(httpMethod)) {
                            throw new PluginImplementationException();
                        }
                        //Matcher matcher = getMatcherAgainstContent("img:([^$]*)");
                        refreshCaptcha = true;
                    } else {
                        httpMethod = getMethodBuilder().setReferer(fileURL).setAction(rootURL + "/include/crypt/cryptographp.php?cfg=0&").toHttpMethod();
                        httpMethod.setFollowRedirects(true);
                        is = client.makeRequestForFile(httpMethod);

                        if (is == null) {
                            throw new PluginImplementationException();
                        }

                        captchaImage = captchaSupport.loadCaptcha(is);
                    }
                    String captchaR = null;
                    do {
                        if( refreshCaptcha ) {
                            httpMethod = getMethodBuilder().setReferer(fileURL).setAction(rootURL + "/ajax_scripts/reload_captcha.php").toHttpMethod();
                            if(!makeRequest(httpMethod)) {
                                throw new PluginImplementationException();
                            }
                            httpMethod = getMethodBuilder().setReferer(fileURL).setAction(rootURL + "/images/captcha/"+getContentAsString().trim()+".jpg").toHttpMethod();
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
                    httpMethod = getMethodBuilder().setReferer(fileURL).setAction(rootURL + "/ajax_scripts/dl.php").toHttpMethod();
                    if (makeRedirectedRequest(httpMethod)) {
                        dlID=getContentAsString();
                    }
                    httpMethod = getMethodBuilder().setReferer(fileURL).setAction(rootURL + "/ajax_scripts/_get.php?link_id=" + linkId + "&link_name=" + linkName + "&dl_id=" + dlID + "&password=").toHttpMethod();
                }
                if (makeRedirectedRequest(httpMethod)) {
                    contentAsString = getContentAsString();
                    httpMethod = getMethodBuilder().setReferer(fileURL).setAction("/download/" + PlugUtils.getStringBetween(contentAsString, "hash:\"", "\"") + "/" + dlID + "/").setBaseURL(PlugUtils.getStringBetween(contentAsString, "{serv:\"", "\"")).toHttpMethod();

                    if (!tryDownloadAndSaveFile(httpMethod)) {
                        checkAllProblems();
                        logger.warning(getContentAsString());
                        throw new IOException("File input stream is empty");
                    }
                } else {
                    throw new ServiceConnectionProblemException();
                }
            } else {
                throw new ServiceConnectionProblemException();
            }
        } else {
            throw new InvalidURLOrServiceProblemException("Invalid URL or service problem");
        }
    }

    private void checkSeriousProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();

        if (contentAsString.contains("File not found")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        if (contentAsString.contains("no available free download slots left for your country")) {
            throw new URLNotAvailableAnymoreException("We are very sorry, but for this file no available free download slots left for your country today");
        }
    }

    private void checkAllProblems() throws ErrorDuringDownloadingException {
        checkSeriousProblems();
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString().replaceAll("&nbsp;", " ");
        try {
            PlugUtils.checkName(httpFile, contentAsString, "Filename:</th>\n<td>", "<");
        } catch(PluginImplementationException e) {
            PlugUtils.checkName(httpFile, contentAsString, "<div id=\"fname\">", "</div>");
        }
        try {
            PlugUtils.checkFileSize(httpFile, contentAsString, "Size:</th>\n<td>", "<");
        } catch(Exception e) {
            PlugUtils.checkFileSize(httpFile, contentAsString, "<div id=\"fsize_div\">", "</div>");
        }
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }
}
