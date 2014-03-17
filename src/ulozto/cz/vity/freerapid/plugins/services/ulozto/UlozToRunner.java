package cz.vity.freerapid.plugins.services.ulozto;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.ulozto.captcha.SoundReader;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;

import java.util.Random;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Ladislav Vitasek, Ludek Zika, JPEXS (captcha)
 */
class UlozToRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(UlozToRunner.class.getName());
    private final static String SERVICE_BASE_URL = "http://uloz.to";
    private int captchaCount = 0;

    private void ageCheck(String content) throws Exception {
        if (content.contains("confirmContent")) { //eroticky obsah vyzaduje potvruemo
            String confirmUrl = fileURL + "?do=askAgeForm-submit";
            PostMethod confirmMethod = (PostMethod) getMethodBuilder()
                    .setAction(confirmUrl)
                    .setEncodePathAndQuery(true)
                    .setAndEncodeParameter("agree", "Souhlasím")
                    .toPostMethod();
            makeRedirectedRequest(confirmMethod);
            if (getContentAsString().contains("confirmContent")) {
                throw new PluginImplementationException("Cannot confirm age");
            }
        }
    }

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        checkURL();
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            ageCheck(getContentAsString());
            checkNameAndSize(getContentAsString());
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    @Override
    public void run() throws Exception {
        super.run();
        checkURL();
        setClientParameter(cz.vity.freerapid.plugins.webclient.DownloadClientConsts.USER_AGENT, "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:10.0.2) Gecko/20100101 Firefox/10.0.2");

        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            ageCheck(getContentAsString());
            checkNameAndSize(getContentAsString());

            captchaCount = 0;
            HttpMethod method = null;
            while (getContentAsString().contains("captchaContainer") || getContentAsString().contains("?captcha=no")) {
                //client.getHTTPClient().getParams().setIntParameter(HttpClientParams.MAX_REDIRECTS, 8);
                method = stepCaptcha();
                downloadTask.sleep(new Random().nextInt(4) + new Random().nextInt(3));
                makeRequest(method);
                if ((method.getStatusCode() == 302) || (method.getStatusCode() == 303)) {
                    String nextUrl = method.getResponseHeader("Location").getValue();
                    method = getMethodBuilder().setReferer(fileURL).setAction(nextUrl).toGetMethod();
                    //downloadTask.sleep(new Random().nextInt(15) + new Random().nextInt(3));
                    logger.info("Download file location : " + nextUrl);
                    break;
                }
                checkProblems();
            }
            if (!tryDownloadAndSaveFile(method)) {
                checkProblems();
                if (method != null) {
                    logger.warning("Final download link URI: " + method.getURI().toString());
                }
                logger.warning(getContentAsString());
                throw new ServiceConnectionProblemException("Error starting download");
            }
            if (method != null) {
                logger.warning("Final download link URI: " + method.getURI().toString());
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkURL() {
        fileURL = fileURL.replaceFirst("(ulozto\\.net|ulozto\\.cz|ulozto\\.sk)", "uloz.to").replaceFirst("http://www\\.uloz\\.to", "http://uloz.to");
    }

    private void checkNameAndSize(String content) throws Exception {
        if (!content.contains("uloz.to")) {
            logger.warning(getContentAsString());
            throw new InvalidURLOrServiceProblemException("Invalid URL or unindentified service");
        }
        if (getContentAsString().contains("soubor nebyl nalezen")) {
            throw new URLNotAvailableAnymoreException("Pozadovany soubor nebyl nalezen");
        }
        PlugUtils.checkName(httpFile, content, "class=\"jsShowDownload\">", "</a>");
        String size;
        try {
            size = PlugUtils.getStringBetween(content, "<span id=\"fileSize\">", "</span>");
            if (size.contains("|")) {
                size = size.substring(size.indexOf("|") + 1).trim();
            }
            httpFile.setFileSize(PlugUtils.getFileSizeFromString(size));
        } catch (PluginImplementationException ex) {
            //u online videi neni velikost
            throw new PluginImplementationException("File size not found");
        }
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private HttpMethod stepCaptcha() throws Exception {
        if (getContentAsString().contains("Please click here to continue")) {
            logger.info("Using HTML redirect");
            return getMethodBuilder().setReferer(fileURL).setActionFromAHrefWhereATagContains("Please click here to continue").toGetMethod();
        }
        CaptchaSupport captchaSupport = getCaptchaSupport();
        MethodBuilder captchaMethod = getMethodBuilder().setActionFromImgSrcWhereTagContains("class=\"captcha\"");

        Matcher captchaIdMatcher = PlugUtils.matcher("/([^/]+)\\.\\w+$", captchaMethod.getAction());
        if (!captchaIdMatcher.find()) {
            throw new PluginImplementationException("Captcha id not found");
        }
        final String captchaId = captchaIdMatcher.group(1);

        String captcha = "";
        //precteni
        //captchaCount = 9; //for test purpose
        if (captchaCount++ < 6) {
            logger.warning("captcha url:" + captchaMethod.getAction());
            Matcher m = Pattern.compile("uloz\\.to/captcha/([0-9]+)\\.png").matcher(captchaMethod.getAction());
            if (m.find()) {
                String number = m.group(1);
                SoundReader captchaReader = new SoundReader();
                HttpMethod methodSound = getMethodBuilder()
                        .setReferer(fileURL)
                        .setAction("http://img.uloz.to/captcha/sound/" + number + ".mp3")
                        .toGetMethod();
                captcha = captchaReader.parse(client.makeRequestForFile(methodSound));
                methodSound.releaseConnection();
            }
        } else {
            captcha = captchaSupport.getCaptcha(captchaMethod.getAction());
        }
        if (captcha == null) {
            throw new CaptchaEntryInputMismatchException();
        } else {
            MethodBuilder sendForm = getMethodBuilder()
                    .setBaseURL(SERVICE_BASE_URL)
                    .setReferer(fileURL)
                    .setActionFromFormWhereActionContains("do=downloadDialog-freeDownloadForm-submit", true)
                    .setParameter("captcha[text]", captcha)
                    .setParameter("captcha[id]", captchaId);

            return sendForm.toPostMethod();
        }
    }

    //"Prekro�en pocet FREE slotu, pouzijte VIP download
    private void checkProblems() throws ServiceConnectionProblemException, YouHaveToWaitException, URLNotAvailableAnymoreException {
        String content = getContentAsString();
        if (content.contains("Soubor byl sma") || content.contains("Soubor byl zak")) {
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
