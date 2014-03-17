package cz.vity.freerapid.plugins.services.edisk;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;

import java.io.IOException;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author Ladislav Vitasek, Ludek Zika
 */
class EdiskRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(EdiskRunner.class.getName());

    public EdiskRunner() {
        super();
    }

    public void runCheck() throws Exception {
        super.runCheck();
        final HttpMethod httpMethod = getMethodBuilder().setAction(checkURL(fileURL)).toHttpMethod();

        if (makeRedirectedRequest(httpMethod)) {
            checkNameAndSize(getContentAsString());
        } else
            throw new PluginImplementationException();
    }

    public void run() throws Exception {
        super.run();
        final HttpMethod httpMethod = getMethodBuilder().setAction(checkURL(fileURL)).toHttpMethod();
        if (makeRedirectedRequest(httpMethod)) {
            if (getContentAsString().contains("text z obr")) {
                checkNameAndSize(getContentAsString());

                PostMethod method = stepCaptcha(getContentAsString(), true);
                makeRequest(method);

                while (getContentAsString().contains("text z obr")) {
                    PostMethod method2 = stepCaptcha(getContentAsString(), false);

                    makeRequest(method2);

                }
                String finalURL = getContentAsString();
                GetMethod finalMethod = getGetMethod(finalURL);

                if (!tryDownloadAndSaveFile(finalMethod)) {
                    checkProblems();
                    logger.warning(getContentAsString());
                    throw new IOException("File input stream is empty.");
                }

            } else {
                checkProblems();
                logger.info(getContentAsString());
                throw new PluginImplementationException();
            }
        } else
            throw new PluginImplementationException();
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
        PlugUtils.checkFileSize(httpFile, content, "Velikost souboru:", "<br");
        PlugUtils.checkName(httpFile, content, "ut soubor:", "(");

    }


    private PostMethod stepCaptcha(String contentAsString, boolean hack) throws Exception {
        if (contentAsString.contains("text z obr")) {
            String captcha;
            Matcher matcher;

            if (hack) {
                captcha = "5414";
                downloadTask.sleep(5);
            } else {

                CaptchaSupport captchaSupport = getCaptchaSupport();
                String host = "http://" + httpFile.getFileUrl().getHost();
                String s = getMethodBuilder(contentAsString).setActionFromImgSrcWhereTagContains("captcha").getAction();
                s = host + s;
                logger.info("Captcha URL " + s);
                String captchaR = captchaSupport.getCaptcha(s);
                if (captchaR == null) {
                    throw new CaptchaEntryInputMismatchException();
                }
                captcha = captchaR;
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


    private void checkProblems() throws ServiceConnectionProblemException, YouHaveToWaitException, URLNotAvailableAnymoreException {
        if (getContentAsString().contains("neexistuje z ")) {
            throw new URLNotAvailableAnymoreException(String.format("<b>Požadovaný soubor nebyl nalezen.</b><br>"));
        }
        if (getContentAsString().contains("stahovat pouze jeden soubor")) {
            throw new ServiceConnectionProblemException(String.format("<b>Mùžete stahovat pouze jeden soubor naráz</b><br>"));

        }


    }

}