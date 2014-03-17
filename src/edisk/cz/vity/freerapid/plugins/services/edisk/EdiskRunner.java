package cz.vity.freerapid.plugins.services.edisk;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.edisk.captcha.SoundReader;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.PostMethod;

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Ladislav Vitasek, Ludek Zika, JPEXS (Sound Captcha)
 */
class EdiskRunner extends AbstractRunner {
    public static final String SERVICE_WEB = "http://www.edisk.cz";
    private final static Logger logger = Logger.getLogger(EdiskRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final HttpMethod httpMethod = getMethodBuilder().setAction(checkURL(fileURL)).toHttpMethod();
        if (makeRedirectedRequest(httpMethod)) {
            checkNameAndSize(getContentAsString());
            checkProblems();
        } else {
            throw new ServiceConnectionProblemException();
        }
    }

    @Override
    public void run() throws Exception {
        super.run();
        HttpMethod httpMethod = getMethodBuilder().setAction(checkURL(fileURL)).toHttpMethod();
        if (makeRedirectedRequest(httpMethod)) {
            checkProblems();
            httpMethod = getGetMethod(fileURL.replace("/stahni/", "/stahni-pomalu/"));
            if (makeRedirectedRequest(httpMethod)) {
                checkProblems();
                if (getContentAsString().contains("hnout pouze 1 soubor denn")) {
                    throw new ServiceConnectionProblemException("Zdarma je možné stáhnout pouze 1 soubor denně");
                }
                String action;
                try {
                    action = PlugUtils.getStringBetween(getContentAsString(), "countDown('", "',");
                } catch (PluginImplementationException e) {
                    throw new PluginImplementationException("Cannot find action string");
                }
                try {
                    downloadTask.sleep(PlugUtils.getWaitTimeBetween(getContentAsString(), "var waitSecs = ", ";", TimeUnit.SECONDS));
                } catch (Exception e) {
                    throw new PluginImplementationException("Cannot find waiting time");
                }
                httpMethod = getMethodBuilder().setAction("/x-download/" + action).setBaseURL(SERVICE_WEB).setParameter("action", action).toPostMethod();
                if (makeRedirectedRequest(httpMethod)) {
                    checkProblems();
                    final HttpMethod finalMethod = getMethodBuilder().setAction(getContentAsString()).toGetMethod();
                    if (!tryDownloadAndSaveFile(finalMethod)) {
                        checkProblems();
                        logger.warning(getContentAsString());
                        throw new ServiceConnectionProblemException("Error starting download");
                    }
                } else {
                    checkProblems();
                    logger.info(getContentAsString());
                    throw new ServiceConnectionProblemException();
                }
            } else {
                checkProblems();
                logger.info(getContentAsString());
                throw new ServiceConnectionProblemException();
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private String checkURL(String fileURL) {
        return fileURL.replaceFirst("edisk.sk", "edisk.cz");

    }

    private void checkNameAndSize(String content) throws Exception {
        if (!content.contains("edisk.cz")) {
            logger.warning(getContentAsString());
            throw new InvalidURLOrServiceProblemException("Invalid URL or unindentified service");
        }
        if (content.contains("neexistuje z ")) {
            throw new URLNotAvailableAnymoreException(String.format("<b>Požadovaný soubor nebyl nalezen.</b><br>"));
        }
        PlugUtils.checkFileSize(httpFile, content, "Velikost souboru: <strong>", "</strong>");
        PlugUtils.checkName(httpFile, content, "nout soubor:&nbsp;<h1>", " (");

    }

    private PostMethod stepCaptcha(String contentAsString, boolean hack) throws Exception {
        if (contentAsString.contains("text z obr")) {
            String captcha = "";
            Matcher matcher;
            if (hack) {
                captcha = "5414";
                downloadTask.sleep(5);
            } else {
                CaptchaSupport captchaSupport = getCaptchaSupport();
                String host = "http://" + httpFile.getFileUrl().getHost();
                String captchaImgUrl = getMethodBuilder(contentAsString).setActionFromImgSrcWhereTagContains("captcha").getAction();
                captchaImgUrl = host + captchaImgUrl;
                Matcher m = Pattern.compile("/([0-9]+)$").matcher(captchaImgUrl);
                String captchaNumber;
                logger.info("Captcha Image URL " + captchaImgUrl);
                if (m.find()) {
                    captchaNumber = m.group(1);
                    String captchaSoundUrl = "http://www.edisk.cz/x-generate-member-audio-captcha/" + captchaNumber;
                    logger.info("Captcha Sound URL " + captchaSoundUrl);
                    //request for image must be done first in order to download sound
                    HttpMethod imageMethod = getMethodBuilder().setAction(captchaImgUrl).toGetMethod();
                    client.makeRequestForFile(imageMethod);
                    imageMethod.releaseConnection();

                    //download sound
                    HttpMethod soundMethod = getMethodBuilder().setAction(captchaSoundUrl).toGetMethod();
                    soundMethod.setFollowRedirects(true);
                    captcha = SoundReader.readWav(client.makeRequestForFile(soundMethod));
                    soundMethod.releaseConnection();
                    logger.info("Captcha read: " + captcha);
                }
                if ((captcha == null) || (captcha.length() < 4)) {
                    String captchaR = captchaSupport.getCaptcha(captchaImgUrl);
                    if (captchaR == null) {
                        throw new CaptchaEntryInputMismatchException();
                    }
                    captcha = captchaR;
                }
            }
            matcher = PlugUtils.matcher("form method=\"post\"\\s*action=\"([^\"]*)\"", contentAsString);
            if (!matcher.find()) {
                logger.info(getContentAsString());
                throw new PluginImplementationException();
            }
            String postTargetURL;
            postTargetURL = matcher.group(1);
            client.setReferer(postTargetURL);
            String type = "";
            if (postTargetURL.contains("stahnout-soubor")) {
                postTargetURL = postTargetURL.replace("stahnout-soubor", "x-download");
                type = "member";
            }
            if (postTargetURL.contains("stahni")) {
                postTargetURL = postTargetURL.replace("stahni", "x-download");
                type = "quick";
            }

            logger.info("Captcha target URL " + postTargetURL);
            //   client.setReferer(fileURL);
            final PostMethod postMethod = getPostMethod(postTargetURL);
            postMethod.addParameter("captchaCode", captcha);
            postMethod.addParameter("type", type);
            postMethod.addRequestHeader("X-Requested-With", "XMLHttpRequest");
            return postMethod;

        } else {
            logger.warning(contentAsString);
            throw new PluginImplementationException("Captcha picture was not found");
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        if (getContentAsString().contains("neexistuje z ")) {
            throw new URLNotAvailableAnymoreException(String.format("<b>Po?adovan? soubor nebyl nalezen.</b><br>"));
        }
        if (getContentAsString().contains("stahovat pouze jeden soubor")) {
            throw new ServiceConnectionProblemException(String.format("<b>M?ete stahovat pouze jeden soubor nar?z</b><br>"));

        }
    }

}