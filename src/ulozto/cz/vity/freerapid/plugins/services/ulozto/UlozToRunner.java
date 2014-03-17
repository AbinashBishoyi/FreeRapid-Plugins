package cz.vity.freerapid.plugins.services.ulozto;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.RedirectException;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpClientParams;

import java.io.UnsupportedEncodingException;
import java.util.logging.Logger;
import java.util.regex.Matcher;

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
        fileURL = checkURL(fileURL);
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkNameAndSize(getContentAsString());
        } else
            throw new PluginImplementationException();
    }

    public void run() throws Exception {
        super.run();
        fileURL = checkURL(fileURL);       
        final GetMethod getMethod = getGetMethod(fileURL);
        getMethod.setFollowRedirects(true);
        if (makeRedirectedRequest(getMethod)) {
            if (getContentAsString().contains("id=\"captcha\"")) {
                checkNameAndSize(getContentAsString());
                while (getContentAsString().contains("id=\"captcha\"")) {
                    client.getHTTPClient().getParams().setIntParameter(HttpClientParams.MAX_REDIRECTS, 8);
                    GetMethod method = stepCaptcha(getContentAsString());
                    
                        if (tryDownloadAndSaveFile(method)) break;
                    if(method.getURI().toString().contains("full=y"))         
                        throw new ServiceConnectionProblemException("<b>Doèasném omezení FREE stahování, zkuste pozdìji</b><br>");

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

    private String sicherName(String s) throws UnsupportedEncodingException {
        Matcher matcher = PlugUtils.matcher("(.*/)([^/]*)$", s);
        if (matcher.find()) {
            return matcher.group(2);
        }
        return "file01";
    }

    private void checkNameAndSize(String content) throws Exception {

        if (!content.contains("uloz.to")) {
            logger.warning(getContentAsString());
            throw new InvalidURLOrServiceProblemException("Invalid URL or unindentified service");
        }
        if (getContentAsString().contains("soubor nebyl nalezen")) {
            throw new URLNotAvailableAnymoreException("<b>Požadovaný soubor nebyl nalezen.</b><br>");
        }

        Matcher matcher = PlugUtils.matcher("\\|\\s*([^|]+) \\| </title>", content);
        // odebiram jmeno
        String fn;
        if (matcher.find()) {
            fn = matcher.group(1);
        } else fn = sicherName(fileURL);
        logger.info("File name " + fn);
        httpFile.setFileName(fn);
        // konec odebirani jmena

        matcher = PlugUtils.matcher("([0-9.]+ .B)</b>", content);
        if (matcher.find()) {
            Long a = PlugUtils.getFileSizeFromString(matcher.group(1));
            logger.info("File size " + a);
            httpFile.setFileSize(a);
            httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
        }

    }

    private GetMethod stepCaptcha(String contentAsString) throws Exception {
        if (contentAsString.contains("id=\"captcha\"")) {
            CaptchaSupport captchaSupport = getCaptchaSupport();
            Matcher matcher = PlugUtils.matcher("src=\"([^\"]*captcha[^\"]*)\"", contentAsString);
            if (matcher.find()) {
                String s = matcher.group(1);

                logger.info("Captcha URL " + s);
                String captcha = captchaSupport.getCaptcha(s);
                if (captcha == null) {
                    throw new CaptchaEntryInputMismatchException();
                } else {

                    String captcha_nb = PlugUtils.getParameter("captcha_nb", contentAsString);

                    matcher = PlugUtils.matcher("form name=\"dwn\" action=\"([^\"]*)\"", contentAsString);
                    if (!matcher.find()) {
                        logger.info(getContentAsString());
                        throw new PluginImplementationException();
                    }
                    String postTargetURL;
                    postTargetURL = matcher.group(1);
                    logger.info("Captcha target URL " + postTargetURL);
                    client.setReferer(fileURL);
//                    final PostMethod postMethod = getPostMethod(postTargetURL);
                    final GetMethod gMethod = getGetMethod(postTargetURL);
                    gMethod.addRequestHeader("captcha_nb", captcha_nb);
                    gMethod.addRequestHeader("captcha_user", captcha);
                    client.getHTTPClient().getState().addCookie(new Cookie(".uloz.to", "captcha_nb", captcha_nb, "/", 86400, false));
                     client.getHTTPClient().getState().addCookie(new Cookie(".uloz.to", "captcha_user", captcha, "/", 86400, false));
//                    postMethod.addParameter("captcha_nb", captcha_nb);
//                    postMethod.addParameter("captcha_user", captcha);
                    //                 postMethod.addParameter("download", PlugUtils.unescapeHtml("Stáhnout free"));
                    return gMethod;

                }
            } else {
                logger.warning(contentAsString);
                throw new PluginImplementationException("Captcha picture was not found");
            }
        }
        return null;
    }

    private void checkProblems() throws ServiceConnectionProblemException, YouHaveToWaitException, URLNotAvailableAnymoreException {
        Matcher matcher;
        matcher = getMatcherAgainstContent("soubor nebyl nalezen");
        if (matcher.find()) {
            throw new URLNotAvailableAnymoreException("<b>Požadovaný soubor nebyl nalezen.</b><br>");
        }
        matcher = getMatcherAgainstContent("stahovat pouze jeden soubor");
        if (matcher.find()) {
            throw new ServiceConnectionProblemException("<b>Mùžete stahovat pouze jeden soubor naráz</b><br>");

        }


    }

}
