package cz.vity.freerapid.plugins.services.dlfreefr;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.Locale;
import java.util.Random;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author ntoskrnl,tong2shot
 */
class DlFreeFrFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(DlFreeFrFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        checkURL();
        logger.info("runCheck " + fileURL);
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    @Override
    public void run() throws Exception {
        super.run();
        checkURL();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            final String contentType = method.getResponseHeader("Content-Type").getValue().toLowerCase(Locale.ENGLISH);
            if (!contentType.contains("html")) {
                //try downloading directly
                if (tryDownloadAndSaveFile(method)) return;
            }

            checkProblems();
            checkNameAndSize();

            final String formSubmitContent = getContentAsString();
            final String key = PlugUtils.getStringBetween(getContentAsString(), "\"key\":\"", "\"");
            final String env = "prod";
            final String lang = "fr";
            setFileStreamContentTypes(new String[0], new String[]{"application/javascript"});

            HttpMethod httpMethod;
            int reqCounter = 0;
            do {
                if (reqCounter++ > 8) {
                    throw new PluginImplementationException("Text validation not found");
                }
                final String callback = "Adyoulike.g._jsonp_" + new Random().nextInt(100000000);
                //Adyoulike.g._jsonp_6968539184587093({"translations":{"fr":{"instructions_visual":"Recopiez « poulain » ci-dessous :"}},"site_under":false,"clickable":false,"pixels":{"VIDEO_050":[],"DISPLAY":[],"VIDEO_000":[],"VIDEO_100":[],"VIDEO_025":[],"VIDEO_075":[]},"medium_type":"image/adyoulike","iframes":{"big":"<script language=\"Javascript\"><!--\r\n  amgdgt_clkurl = \"[CLICK_URL_UNESC]\";\r\n  amgdgt_p=\"8231\";\r\n  amgdgt_pl=\"d3bbf68e\"; \r\n  amgdgt_t = \"i\";\r\n//-->\r\n</script><script type=\"text/javascript\" src=\"http://cdn.amgdgt.com/base/js/v1/amgdgt.js\"></script>\r\n<noscript><a href=\"[CLICK_URL_UNESC]http://ad.amgdgt.com/ads/?t=c&c=q6eXNI\" target=\"_blank\"><img src=\"http://ad.amgdgt.com/ads/?t=i&f=h&p=8231&pl=d3bbf68e&c=q6eXNI&rnd=[cache_buster]\" width=\"300\" height=\"250\" border=\"0\" /></a></noscript>\r\n"},"shares":{},"id":285,"token":"jRADmcjxtyer6pssEX1POTvHnB1~MjA1","formats":{"small":{"y":300,"x":0,"w":300,"h":60},"big":{"y":0,"x":0,"w":300,"h":250},"hover":{"y":440,"x":0,"w":300,"h":60}},"tid":"WeGeAKoDEmmuPDLjFgu56wO2V1H8pW5H"})
                //Adyoulike.g._jsonp_9199199453999704({"translations":{"fr":{"instructions_visual":"Recopier le texte qui est entre guillemets"}},"site_under":true,"clickable":true,"pixels":{"VIDEO_050":[],"DISPLAY":[],"VIDEO_000":[],"VIDEO_100":[],"VIDEO_025":[],"VIDEO_075":[]},"medium_type":"video/youtube","iframes":{},"video_id":"Qh-uIJMegsw","shares":{},"id":288,"token":"KVUVWwPpkxdqqkJkPPQk8fxsJdz~MjA1","formats":{"small":{"y":300,"x":0,"w":300,"h":60},"big":{"y":0,"x":0,"w":300,"h":250},"hover":{"y":440,"x":0,"w":300,"h":60}},"tid":"WeGeAKoDEmmuPDLjFgu56wO2V1H8pW5H"})
                httpMethod = getMethodBuilder()
                        .setReferer(fileURL)
                        .setAction("http://api-ayl.appspot.com/challenge")
                        .setParameter("key", key)
                        .setParameter("env", env)
                        .setParameter("callback", callback)
                        .setParameter("lang", lang)
                        .toGetMethod();
                if (!makeRedirectedRequest(httpMethod)) {
                    checkProblems();
                    throw new ServiceConnectionProblemException();
                }
            } while (!getContentAsString().contains(" » ci-dessous"));
            checkProblems();

            final String validationResponse = PlugUtils.getStringBetween(getContentAsString(), "\"Recopiez « ", " » ci-dessous");
            final String tokenChallenge = PlugUtils.getStringBetween(getContentAsString(), "\"token\":\"", "\"");
            final String tid = PlugUtils.getStringBetween(getContentAsString(), "\"tid\":\"", "\"");

            httpMethod = getMethodBuilder(formSubmitContent)
                    .setReferer(fileURL)
                    .setActionFromFormWhereActionContains("getfile.pl", true)
                    .setParameter("_ayl_captcha_engine", "adyoulike")
                    .setParameter("_ayl.focus", "")
                    .setParameter("_ayl_response", validationResponse)
                    .setParameter("_ayl_utf8_ie_fix", "?")
                    .setParameter("_ayl_env", env)
                    .setParameter("_ayl_token_challenge", tokenChallenge)
                    .setParameter("_ayl_tid", tid)
                    .toPostMethod();

            if (!tryDownloadAndSaveFile(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException("Error starting download");
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        final Matcher name = getMatcherAgainstContent("Fichier:</td>\\s*?<td.*?>(.+?)</td>");
        if (!name.find()) throw new PluginImplementationException("File name not found");
        httpFile.setFileName(name.group(1));

        //they use 'o' instead of 'b' in French
        final Matcher size = getMatcherAgainstContent("Taille:</td>\\s*?<td.*?>(.+?)o");
        if (!size.find()) throw new PluginImplementationException("File size not found");
        httpFile.setFileSize(PlugUtils.getFileSizeFromString(size.group(1) + "b"));

        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        if (content.contains("Fichier inexistant") || content.contains("Erreur 404") || content.contains("Appel incorrect")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        if (content.contains("\"disabled\":true")) {
            throw new PluginImplementationException("Server rejected your request");
        }
    }

    private void checkURL() {
        if (!fileURL.contains("getfile.pl")) {
            fileURL = fileURL.replaceAll("dl\\.free\\.fr/.", "dl.free.fr/getfile.pl?file=/"); //yes you read it right, delete 1 char after /
        }
    }

}