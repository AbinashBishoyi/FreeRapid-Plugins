package cz.vity.freerapid.plugins.services.leteckaposta;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author Ladislav Vitasek
 */
class LeteckaPostaFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(LeteckaPostaFileRunner.class.getName());
    private final static String LETECKA_POSTA_CZ_WEB = "http://leteckaposta.cz";


    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkNameAndSize(getContentAsString());
        } else
            throw new PluginImplementationException();
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        //zov: <strong>nhl.09.part06.rar<br>
        Matcher matcher = PlugUtils.matcher("class='download-link'>(.*?)<", content);
        if (matcher.find()) {
            final String fileName = matcher.group(1).trim(); //method trim removes white characters from both sides of string
            logger.info("File name " + fileName);
            httpFile.setFileName(fileName);
            //: <strong>204800</strong>KB<br>
            matcher = PlugUtils.matcher("Velikost souboru:(.+?)<", content);
            if (matcher.find()) {
                final long size = PlugUtils.getFileSizeFromString(matcher.group(1));
                httpFile.setFileSize(size);
            } else {
                checkProblems();
                logger.warning("File size was not found\n:");
                throw new PluginImplementationException();
            }
        } else {
            checkProblems();
            logger.warning("File name was not found");
            throw new PluginImplementationException();
        }
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            final String contentAsString = getContentAsString();
            checkProblems();
            checkNameAndSize(contentAsString);
            client.setReferer(fileURL);
            final Matcher matcher = getMatcherAgainstContent("href='(.*?)' class='download-link");
            if (matcher.find()) {
                final GetMethod getMethod = getGetMethod(LETECKA_POSTA_CZ_WEB + matcher.group(1));

                if (!tryDownloadAndSaveFile(getMethod)) {
                    checkProblems();
                    logger.warning(getContentAsString());
                    throw new ServiceConnectionProblemException("Vaše adresa IP už stahuje maximální poèet souborù stahnutelných najednou");
                }
            } else throw new PluginImplementationException("Plugin error: Download link not found");
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("Soubor neexistuje")) {
            throw new URLNotAvailableAnymoreException("Soubor neexistuje");
        }
        if (contentAsString.contains("stahuje maxim")) {
            throw new ServiceConnectionProblemException("Vaše adresa IP už stahuje maximální poèet souborù stahnutelných najednou");
        }
    }

}
