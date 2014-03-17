package cz.vity.freerapid.plugins.services.ulozto;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.PlugUtils;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;

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
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRequest(getMethod)) {
            checkNameAndSize(getContentAsString());
        } else
            throw new PluginImplementationException("Problem with a connection to service.\nCannot find requested page content");
    }

    public void run() throws Exception {
        super.run();

        final GetMethod getMethod = getGetMethod(fileURL);
        getMethod.setFollowRedirects(true);
        if (makeRequest(getMethod)) {
            if (getContentAsString().contains("id=\"captcha\"")) {
                checkNameAndSize(getContentAsString());
                while (getContentAsString().contains("id=\"captcha\"")) {
                    PostMethod method = stepCaptcha(getContentAsString());
                    //    method.setFollowRedirects(true);
                    if (tryDownload(method)) break;
                }
            } else {
                checkProblems();
                logger.info(getContentAsString());
                throw new PluginImplementationException("Problem with a connection to service.\nCannot find requested page content");
            }
        } else
            throw new PluginImplementationException("Problem with a connection to service.\nCannot find requested page content");
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
        Matcher matcher = getMatcherAgainstContent("soubor nebyl nalezen");
        if (matcher.find()) {
            throw new URLNotAvailableAnymoreException(String.format("<b>Požadovaný soubor nebyl nalezen.</b><br>"));
        }

        matcher = PlugUtils.matcher("\\|\\s*([^|]+) \\| </title>", content);
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

    private PostMethod stepCaptcha(String contentAsString) throws Exception {
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
                        throw new PluginImplementationException("Problem with a connection to service.\nCannot find requested page content");
                    }
                    String postTargetURL;
                    postTargetURL = matcher.group(1);
                    logger.info("Captcha target URL " + postTargetURL);
                    client.setReferer(fileURL);
                    final PostMethod postMethod = getPostMethod(postTargetURL);
                    postMethod.addParameter("captcha_nb", captcha_nb);
                    postMethod.addParameter("captcha_user", captcha);
                    postMethod.addParameter("download", PlugUtils.unescapeHtml("--%3E+St%C3%A1hnout+soubor+%3C--"));
                    return postMethod;

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
            throw new URLNotAvailableAnymoreException(String.format("<b>Požadovaný soubor nebyl nalezen.</b><br>"));
        }
        matcher = getMatcherAgainstContent("stahovat pouze jeden soubor");
        if (matcher.find()) {
            throw new ServiceConnectionProblemException(String.format("<b>Mùžete stahovat pouze jeden soubor naráz</b><br>"));

        }


    }

}
