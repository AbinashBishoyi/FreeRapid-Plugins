package cz.vity.freerapid.plugins.services.shareonline;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import cz.vity.freerapid.utilities.Utils;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethod;

import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * @author Ladislav Vitasek, Ludek Zika, ntoskrnl
 */
class ShareonlineRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(ShareonlineRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        addCookie(new Cookie(".share-online.biz", "page_language", "english", "/", 86400, false));
        final HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        final String nfo = decryptFileInfo(PlugUtils.getStringBetween(getContentAsString(), "var nfo=\"", "\";"));
        final String div = PlugUtils.getStringBetween(getContentAsString(), "var div=\"", "\";");
        final String[] file = nfo.split(Pattern.quote(div));
        try {
            httpFile.setFileName(file[3]);
            httpFile.setFileSize(Long.parseLong(file[0]));
        } catch (final Exception e) {
            logger.warning("nfo = " + nfo);
            logger.warning("div = " + div);
            throw new PluginImplementationException("Error parsing file info", e);
        }
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private static String decryptFileInfo(final String nfo) throws ErrorDuringDownloadingException {
        try {
            final String[] a = Utils.reverseString(nfo).split("a\\|b");
            final int length = a[1].length() / 3;
            final char[] result = new char[length];
            for (int i = 0; i < length; i++) {
                // Split a[1] into substrings of 3 characters each
                final int index = Integer.parseInt(a[1].substring(i * 3, (i + 1) * 3), 16);
                result[index] = a[0].charAt(i);
            }
            return new String(result);
        } catch (final Exception e) {
            logger.warning("nfo = " + nfo);
            throw new PluginImplementationException("Error decrypting file info", e);
        }
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        addCookie(new Cookie(".share-online.biz", "page_language", "english", "/", 86400, false));
        HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize();
            method = getMethodBuilder()
                    .setActionFromTextBetween("var url=\"", "\";")
                    .setParameter("dl_free", "1")
                    .setParameter("choice", "free")
                    .toPostMethod();
            final HttpMethod imgMethod = getMethodBuilder()
                    .setAction("http://www.share-online.biz/template/images/corp/uploadking.php?show=last")
                    .toGetMethod();
            makeRedirectedRequest(imgMethod);
            if (makeRedirectedRequest(method)) {
                checkProblems();
                final int wait = PlugUtils.getNumberBetween(getContentAsString(), "var wait=", ";");
                final String dl = new String(Base64.decodeBase64(
                        PlugUtils.getStringBetween(getContentAsString(), "var dl=\"", "\";")), "UTF-8");
                method = getMethodBuilder().setAction(dl).toGetMethod();
                downloadTask.sleep(wait + 1);
                if (!tryDownloadAndSaveFile(method)) {
                    checkProblems();
                    throw new ServiceConnectionProblemException("Error starting download");
                }
            } else {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        if (getContentAsString().contains("The requested file is not available")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        if (getContentAsString().contains("No free slots for free users")) {
            throw new ServiceConnectionProblemException("No free slots for free users");
        }
        if (getContentAsString().contains("No other download thread possible")) {
            throw new ServiceConnectionProblemException("No other download thread possible");
        }
    }

}