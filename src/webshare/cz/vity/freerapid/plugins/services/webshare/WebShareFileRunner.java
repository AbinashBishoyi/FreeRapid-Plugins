package cz.vity.freerapid.plugins.services.webshare;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author Vity
 */
class WebShareFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(WebShareFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize(getContentAsString());
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        final Matcher match = PlugUtils.matcher("https?://.+?/.+?-(.+)", fileURL);
        if (match.find())
            httpFile.setFileName(match.group(1));
        else
            PlugUtils.checkName(httpFile, content, "class=\"textbox\">", "</div>");
        final int i = content.indexOf("Velikost souboru");
        if (i > 0) {
            final String s = content.substring(i).replaceAll("MiB", "MB").replaceAll("KiB", "KB").replaceAll("GiB", "GB");
            PlugUtils.checkFileSize(httpFile, s, "class=\"textbox\">", "</div>");
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

            final String l = PlugUtils.getStringBetween(getContentAsString(), "var l =", ";").trim().replace("'", "").replace("\"", "");
            final String downloadLink = new String(Base64.decodeBase64(l));
            final HttpMethod httpMethod = getMethodBuilder()
                    .setReferer(fileURL)
                    .setAction(downloadLink)
                    .toHttpMethod();

            if (!tryDownloadAndSaveFile(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException("Error starting download");
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("soubor nebyl nalezen") || contentAsString.contains("Soubor nenalezen")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

}