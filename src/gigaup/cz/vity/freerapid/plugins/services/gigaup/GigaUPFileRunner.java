package cz.vity.freerapid.plugins.services.gigaup;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 * <p/>
 * WARNING: This plugin will not work until FRD has support for FTP links.
 *
 * @author ntoskrnl
 */
class GigaUPFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(GigaUPFileRunner.class.getName());
    private int captchaCounter = 1, captchaMax = 5;

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        final Matcher matcherName = getMatcherAgainstContent("Nom :</td>\\s+?<td>(.+?)</td>");
        if (!matcherName.find()) throw new PluginImplementationException("File name not found");
        httpFile.setFileName(matcherName.group(1));

        final Matcher size = getMatcherAgainstContent("Taille :</td>\\s+?<td>(.+?)o</td>");
        if (!size.find()) throw new PluginImplementationException("File size not found");
        httpFile.setFileSize(PlugUtils.getFileSizeFromString(size.group(1) + "B")); //they use "o" instead of "B" for byte in french

        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize();

            HttpMethod httpMethod;
            if (getContentAsString().contains("bot_sucker")) {
                while (getContentAsString().contains("bot_sucker")) {
                    httpMethod = stepCaptcha();
                    if (!makeRedirectedRequest(httpMethod)) {
                        throw new ServiceConnectionProblemException("Error posting captcha");
                    }
                }
            } else {
                throw new PluginImplementationException("Captcha not found");
            }
            logger.info("Captcha OK");

            final HttpMethod httpMethod2 = getMethodBuilder().setReferer(fileURL).setActionFromAHrefWhereATagContains("Commencer le téléchargement").toGetMethod();
            if (!tryDownloadAndSaveFile(httpMethod2)) {
                checkProblems();
                throw new PluginImplementationException();
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        if (content.contains("Avec Gigaup, uploadez vos fichiers gratuitement") || content.contains("Not Found")) {
            throw new URLNotAvailableAnymoreException("Page not found, bad URL?");
        }
        if (content.contains("Le fichier que vous tentez de télécharger n'existe pas")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        if (content.contains("Le fichier a été désigné illégal par les administrateurs et donc supprimé")) {
            throw new URLNotAvailableAnymoreException("File not found - illegal content");
        }
        if (content.contains("Fichier supprimé car non utilisé sur une période trop longue")) {
            throw new URLNotAvailableAnymoreException("File not found - too long since last download");
        }
    }

    private HttpMethod stepCaptcha() throws ErrorDuringDownloadingException {
        final CaptchaSupport captchaSupport = getCaptchaSupport();
        final String captchaSrc = "http://www.gigaup.fr/" + PlugUtils.getStringBetween(getContentAsString(), "<img src=\"../../", "\"");
        logger.info("Captcha URL " + captchaSrc);

        String captcha;
        if (captchaCounter <= captchaMax) {
            captcha = PlugUtils.recognize(captchaSupport.getCaptchaImage(captchaSrc), "-d -1 -C 0-9");
            logger.info("OCR attempt " + captchaCounter + " of " + captchaMax + ", recognized " + captcha);
            captchaCounter++;
        } else {
            captcha = captchaSupport.getCaptcha(captchaSrc);
            if (captcha == null) throw new CaptchaEntryInputMismatchException();
        }

        return getMethodBuilder().setReferer(fileURL).setBaseURL("http://www.gigaup.fr/").setActionFromFormWhereTagContains("Télécharger le fichier", true).setParameter("bot_sucker", captcha).toPostMethod();
    }

}