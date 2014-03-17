package cz.vity.freerapid.plugins.services.ulozto;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.ulozto.captcha.SoundReader;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadClientConsts;
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

/**
 * @author Ladislav Vitasek
 * @author Ludek Zika
 * @author JPEXS (captcha)
 * @author birchie
 * @author tong2shot
 */
class UlozToRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(UlozToRunner.class.getName());
    private int captchaCount = 0;
    private final Random random = new Random();

    private void ageCheck(String content) throws Exception {
        if (content.contains("confirmContent")) { //eroticky obsah vyzaduje potvruemo
            final PostMethod confirmMethod = (PostMethod) getMethodBuilder()
                    .setActionFromFormWhereActionContains("askAgeForm", true)
                    .removeParameter("disagree")
                    .setReferer(fileURL)
                    .setEncodeParameters(true)
                    .toPostMethod();
            makeRedirectedRequest(confirmMethod);
            if (getContentAsString().contains("confirmContent")) {
                throw new PluginImplementationException("Cannot confirm age");
            }
        }
    }

    private boolean isPasswordProtected() {
        return getContentAsString().contains("passwordProtectedFile");
    }

    private void passwordProtectedCheck() throws Exception {
        while (getContentAsString().contains("passwordProtectedFile")) {
            final String password = getDialogSupport().askForPassword("Ulozto password protected file");
            if (password == null) {
                throw new PluginImplementationException("This file is secured with a password");
            }
            HttpMethod hm = getMethodBuilder()
                    .setActionFromFormWhereActionContains("passwordProtected", true)
                    .setReferer(fileURL)
                    .setParameter("password", password)
                    .toPostMethod();
            if (!makeRedirectedRequest(hm)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
        }
    }

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        checkURL();
        setClientParameter(DownloadClientConsts.USER_AGENT, "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:26.0) Gecko/20100101 Firefox/26.0");
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            if (!isPasswordProtected()) {
                ageCheck(getContentAsString());
                checkNameAndSize(getContentAsString());
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    @Override
    public void run() throws Exception {
        super.run();
        checkURL();
        setClientParameter(DownloadClientConsts.USER_AGENT, "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:26.0) Gecko/20100101 Firefox/26.0");
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            passwordProtectedCheck();
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
            setFileStreamContentTypes("text/plain", "text/texmacs");
            if (!tryDownloadAndSaveFile(method)) {
                checkProblems();
                throw new ServiceConnectionProblemException("Error starting download");
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkURL() {
        fileURL = fileURL.replaceFirst("(ulozto\\.net|ulozto\\.cz|ulozto\\.sk)", "uloz.to").replaceFirst("http://www\\.uloz\\.to", "http://uloz.to");
    }

    @Override
    protected String getBaseURL() {
        return "http://uloz.to";
    }

    private void checkNameAndSize(String content) throws Exception {
        if (!content.contains("uloz.to")) {
            logger.warning(getContentAsString());
            throw new InvalidURLOrServiceProblemException("Invalid URL or unindentified service");
        }
        if (getContentAsString().contains("soubor nebyl nalezen")) {
            throw new URLNotAvailableAnymoreException("Pozadovany soubor nebyl nalezen");
        }
        PlugUtils.checkName(httpFile, content, "<title>", " | Ulo");
        String size;
        try {
            //tady nema byt id=, jinak to prestane fungovat
            size = PlugUtils.getStringBetween(content, "<span class=\"fileSize\">", "</span>");
            if (size.contains("|")) {
                size = size.substring(size.indexOf("|") + 1).trim();
            }
            httpFile.setFileSize(PlugUtils.getFileSizeFromString(size));
        } catch (PluginImplementationException ex) {
            //u online videi neni velikost
            //throw new PluginImplementationException("File size not found");
        }
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private HttpMethod stepCaptcha() throws Exception {
        if (getContentAsString().contains("Please click here to continue")) {
            logger.info("Using HTML redirect");
            return getMethodBuilder().setReferer(fileURL).setActionFromAHrefWhereATagContains("Please click here to continue").toGetMethod();
        }
        final CaptchaSupport captchaSupport = getCaptchaSupport();
        final MethodBuilder sendForm = getMethodBuilder()
                .setReferer(fileURL)
                .setActionFromFormWhereActionContains("do=downloadDialog-freeDownloadForm-submit", true);
        final Matcher captchaUrlMatcher = getMatcherAgainstContent("src=\"(http://xapca[^\"<>]+?/image\\.gif)\"");
        if (!captchaUrlMatcher.find()) {
            throw new PluginImplementationException("Captcha URL not found");
        }
        final String captchaImg = captchaUrlMatcher.group(1);
        final String captchaSnd = captchaImg.replace("image.gif", "sound.wav");
        String captchaTxt;
        //captchaCount = 9; //for test purpose
        if (captchaCount++ < 8) {
            logger.info("captcha url: " + captchaSnd);
            final SoundReader captchaReader = new SoundReader(); //load fingerprint from file to test, don't forget to change it back
            final HttpMethod methodSound = getMethodBuilder()
                    .setReferer(fileURL)
                    .setAction(captchaSnd)
                    .toGetMethod();
            try {
                captchaTxt = captchaReader.parse(client.makeRequestForFile(methodSound));
                logger.info("Auto recog attempt : " + captchaCount);
                logger.info("Captcha recognized : " + captchaTxt);
            } catch (Exception e) {
                final StringBuilder captchaTxtBuilder = new StringBuilder(4);
                for (int i = 0; i < 4; i++) {
                    captchaTxtBuilder.append(Character.toChars(random.nextInt(26) + 97)); //throw random chars
                }
                captchaTxt = captchaTxtBuilder.toString();
                logger.info("Generated random captcha : " + captchaTxt);
            } finally {
                methodSound.releaseConnection();
            }
        } else {
            captchaTxt = captchaSupport.getCaptcha(captchaImg);
        }
        if (captchaTxt == null) {
            throw new CaptchaEntryInputMismatchException();
        } else {
            sendForm.setParameter("captcha_value", captchaTxt);
            return sendForm.toPostMethod();
        }
    }

    //"Prekro�en pocet FREE slotu, pouzijte VIP download
    private void checkProblems() throws Exception {
        final String content = getContentAsString();
        if (content.contains("Soubor byl sma") || content.contains("Soubor byl zak")) {
            throw new URLNotAvailableAnymoreException("Soubor byl smazan");
        }
        if (content.contains("soubor nebyl nalezen")) {
            throw new URLNotAvailableAnymoreException("Pozadovany soubor nebyl nalezen");
        }
        if (content.contains("stahovat pouze jeden soubor")) {
            throw new ServiceConnectionProblemException("Muzete stahovat pouze jeden soubor naraz");
        }
        if (content.contains("Majitel souboru si nepřeje soubor zveřejnit a označil jej jako soukromý")) {
            throw new NotRecoverableDownloadException("Majitel souboru si nepřeje soubor zveřejnit a označil jej jako soukromý");
        }
        if (content.contains("et FREE slot") && content.contains("ijte VIP download")) {
            logger.warning(getContentAsString());
            throw new YouHaveToWaitException("Nejsou dostupne FREE sloty", 40);
        }
    }

}
