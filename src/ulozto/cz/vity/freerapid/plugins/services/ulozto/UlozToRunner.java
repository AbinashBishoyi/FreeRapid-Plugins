package cz.vity.freerapid.plugins.services.ulozto;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.ulozto.captcha.SoundReader;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.params.HttpClientParams;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Ladislav Vitasek, Ludek Zika, JPEXS (captcha)
 */
class UlozToRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(UlozToRunner.class.getName());
    private int captchaCount = 0;
    private static SoundReader captchaReader = null;

    public UlozToRunner() {
        super();
    }

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final HttpMethod getMethod = getMethodBuilder().setAction(checkURL(fileURL)).toHttpMethod();
        if (makeRedirectedRequest(getMethod)) {
            checkNameAndSize(getContentAsString());
        } else
            throw new PluginImplementationException();
    }

    @Override
    public void run() throws Exception {
        super.run();
        final HttpMethod getMethod = getMethodBuilder().setAction(checkURL(fileURL)).toHttpMethod();
        getMethod.setFollowRedirects(true);
        if (makeRedirectedRequest(getMethod)) {
            if (getContentAsString().contains("captcha_user")) {

                checkNameAndSize(getContentAsString());
                boolean saved = false;
                captchaCount = 0;
                while (getContentAsString().contains("captcha_user")) {
                    client.getHTTPClient().getParams().setIntParameter(HttpClientParams.MAX_REDIRECTS, 8);
                    HttpMethod method = stepCaptcha();
                    method.setFollowRedirects(false);
                    httpFile.setState(DownloadState.GETTING);
                    final InputStream inputStream = client.makeFinalRequestForFile(method, httpFile, false);
                    if (inputStream != null) {
                        downloadTask.saveToFile(inputStream);
                        return;
                    }
                    if (method.getStatusCode() == 302) {
                        String nextUrl = method.getResponseHeader("Location").getValue();
                        method = getMethodBuilder().setReferer(method.getURI().toString()).setAction(nextUrl).toHttpMethod();
                        if (nextUrl.contains("captcha=no#cpt")) {
                            makeRequest(method);
                            logger.warning("Wrong captcha code");
                            continue;
                        }
                        if (nextUrl.contains("full=y"))
                            throw new YouHaveToWaitException("Nejsou dostupne FREE sloty", 40);
                        if (saved = tryDownloadAndSaveFile(method)) break;
                    }
                    checkProblems();
                }
                if (!saved) {
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
            throw new URLNotAvailableAnymoreException("Pozadovany soubor nebyl nalezen");
        }
        Matcher m = Pattern.compile("<h2 class=\"nadpis\"[^>]*><a href=\"[^\"]*\">([^<]+)</a>").matcher(content);
        if (!m.find()) throw new PluginImplementationException("Cannot find filename");

        httpFile.setFileName(m.group(1));
        m = Pattern.compile("<div class=\"info_velikost\"[^>]*>\\s*<div>\\s*(?:.+\\| *)?([^<\\|]+)\\s*</div>").matcher(content);
        if (!m.find()) throw new PluginImplementationException("Cannot find filesize");
        httpFile.setFileSize(PlugUtils.getFileSizeFromString(m.group(1)));
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private HttpMethod stepCaptcha() throws Exception {
        CaptchaSupport captchaSupport = getCaptchaSupport();
        MethodBuilder captchaMethod = getMethodBuilder().setActionFromImgSrcWhereTagContains("captcha");
        String captcha = "";
        if (captchaCount++ < 3) {
            logger.info("captcha url:" + captchaMethod.getAction());
            Matcher m = Pattern.compile("uloz\\.to/captcha/([0-9]+)\\.png").matcher(captchaMethod.getAction());
            if (m.find()) {
                String number = m.group(1);
                if (captchaReader == null) {
                    captchaReader = new SoundReader();
                }
                HttpMethod methodSound = getMethodBuilder().setAction("http://img.uloz.to/captcha/sound/" + number + ".mp3").toGetMethod();
                captcha = captchaReader.parse(client.makeRequestForFile(methodSound));
            }
        } else {
            captcha = captchaSupport.getCaptcha(captchaMethod.getAction());
        }
        if (captcha == null) {
            throw new CaptchaEntryInputMismatchException();
        } else {
            MethodBuilder sendForm = getMethodBuilder().setReferer(fileURL).setActionFromFormByName("dwn", true);
            sendForm.setEncodePathAndQuery(true);
            sendForm.setAndEncodeParameter("captcha_user", captcha);
            return sendForm.toPostMethod();
        }
    }

    //"P�ekro�en po�et FREE slot�, pou�ijte VIP download

    private void checkProblems() throws ServiceConnectionProblemException, YouHaveToWaitException, URLNotAvailableAnymoreException {
        String content = getContentAsString();
        if (content.contains("Soubor byl sma")) {
            throw new URLNotAvailableAnymoreException("Soubor byl smazan");
        }
        if (content.contains("soubor nebyl nalezen")) {
            throw new URLNotAvailableAnymoreException("Pozadovany soubor nebyl nalezen");
        }
        if (content.contains("stahovat pouze jeden soubor")) {
            throw new ServiceConnectionProblemException("Muzete stahovat pouze jeden soubor naraz");

        }
        if (content.contains("et FREE slot") && content.contains("ijte VIP download")) {
            logger.warning(getContentAsString());
            throw new YouHaveToWaitException("Nejsou dostupne FREE sloty", 40);
        }


    }

}
