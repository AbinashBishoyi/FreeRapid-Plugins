package cz.vity.freerapid.plugins.services.ulozto;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.httpclient.util.URIUtil;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * @author Ladislav Vitasek, Ludek Zika
 */
class UlozToRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(UlozToRunner.class.getName());

    public UlozToRunner() {
        super();
    }

    public void runCheck() throws Exception {
        super.runCheck();
        final HttpMethod getMethod = getMethodBuilder().setAction(checkURL(fileURL)).toHttpMethod();
        if (makeRedirectedRequest(getMethod)) {
            checkNameAndSize(getContentAsString());
        } else
            throw new PluginImplementationException();
    }

    public void run() throws Exception {
        super.run();
        final HttpMethod getMethod = getMethodBuilder().setAction(checkURL(fileURL)).toHttpMethod();
        getMethod.setFollowRedirects(true);
        if (makeRedirectedRequest(getMethod)) {
            if (getContentAsString().contains("id=\"captcha\"")) {
                checkNameAndSize(getContentAsString());
                boolean saved = false;
                while (getContentAsString().contains("id=\"captcha\"")) {

                    client.getHTTPClient().getParams().setIntParameter(HttpClientParams.MAX_REDIRECTS, 8);
                    HttpMethod method = stepCaptcha(getContentAsString());

                    if (saved = tryDownloadAndSaveFile(method)) break;
                    if (method.getURI().toString().contains("full=y"))
                        throw new ServiceConnectionProblemException("<b>Do�asn�m omezen� FREE stahov�n�, zkuste pozd�ji</b><br>");

                }
                if (!saved) {
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

        return fileURL.replaceFirst("(ulozto\\.net|ulozto\\.cz|ulozto\\.sk)", "uloz.to");

    }

    private void checkNameAndSize(String content) throws Exception {

        if (!content.contains("uloz.to")) {
            logger.warning(getContentAsString());
            throw new InvalidURLOrServiceProblemException("Invalid URL or unindentified service");
        }
        if (getContentAsString().contains("soubor nebyl nalezen")) {
            throw new URLNotAvailableAnymoreException("<b>Po�adovan� soubor nebyl nalezen.</b><br>");
        }
        PlugUtils.checkName(httpFile, content, "|", "| </title>");
        PlugUtils.checkFileSize(httpFile, content, "Velikost souboru je <b>", "</b>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private HttpMethod stepCaptcha(String contentAsString) throws Exception {
        if (contentAsString.contains("id=\"captcha\"")) {
            CaptchaSupport captchaSupport = getCaptchaSupport();
            MethodBuilder captchaMethod = getMethodBuilder().setActionFromImgSrcWhereTagContains("captcha");
            String captcha = captchaSupport.getCaptcha(captchaMethod.getAction());
            if (captcha == null) {
                throw new CaptchaEntryInputMismatchException();
            } else {
                MethodBuilder sendForm = getMethodBuilder().setReferer(fileURL).setActionFromFormByName("dwn", true);
                sendForm.setAction(URIUtil.encodePathQuery(sendForm.getAction()));
                sendForm.setAndEncodeParameter("captcha_user", captcha);
                return sendForm.toPostMethod();
            }
        } else {
            logger.warning(contentAsString);
            throw new PluginImplementationException("Captcha picture was not found");
        }
    }

    private void checkProblems() throws ServiceConnectionProblemException, YouHaveToWaitException, URLNotAvailableAnymoreException {
        String content = getContentAsString();
        if (content.contains("soubor nebyl nalezen")) {
            throw new URLNotAvailableAnymoreException("<b>Po�adovan� soubor nebyl nalezen.</b><br>");
        }
        if (content.contains("stahovat pouze jeden soubor")) {
            throw new ServiceConnectionProblemException("<b>M��ete stahovat pouze jeden soubor nar�z</b><br>");

        }


    }

}
