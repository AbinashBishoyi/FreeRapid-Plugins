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
            final String callback = "Adyoulike.g._jsonp_" + new Random().nextInt(100000000);
            final String lang = "fr";

            HttpMethod httpMethod;
            int reqCounter =0;
            do {
                reqCounter++;
                if (reqCounter>8) {
                    throw new PluginImplementationException("Text validation not found");
                }
                httpMethod = getMethodBuilder()
                        .setReferer(fileURL)
                        .setAction("http://api-ayl.appspot.com/challenge")
                        .setParameter("key", key)
                        .setParameter("env", env)
                        .setParameter("callback", callback)
                        .setParameter("lang", lang)
                        .toGetMethod();
                setFileStreamContentTypes(new String[0], new String[]{"application/javascript"});
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
                    .setActionFromFormWhereActionContains("getfile.pl",true)
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

}