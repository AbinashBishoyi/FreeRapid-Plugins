package cz.vity.freerapid.plugins.services.dailymotion;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import cz.vity.freerapid.utilities.LogUtils;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethod;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author Ramsestom, JPEXS, ntoskrnl
 */
class DailymotionRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(DailymotionRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        addCookie(new Cookie(".dailymotion.com", "family_filter", "off", "/", 86400, false));
        final HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkName();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkName() throws ErrorDuringDownloadingException {
        final Matcher matcher = getMatcherAgainstContent("<span class=\"title\"[^<>]*?>(.+?)</span>");
        if (!matcher.find()) throw new PluginImplementationException("File name not found");
        httpFile.setFileName(PlugUtils.unescapeHtml(matcher.group(1)) + ".mp4");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        addCookie(new Cookie(".dailymotion.com", "family_filter", "off", "/", 86400, false));
        HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkName();
            final String sequence = PlugUtils.getStringBetween(getContentAsString(), ".addVariable(\"sequence\",  \"", "\"");
            final String url = PlugUtils.getStringBetween(sequence, "%22sdURL%22%3A%22", "%22");
            method = getGetMethod(urlDecode(url).replace("\\", ""));
            if (!tryDownloadAndSaveFile(method)) {
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
        if (contentAsString.contains("We can't find the page you're looking for")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    private static String urlDecode(final String url) {
        try {
            return url == null ? "" : URLDecoder.decode(url, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            LogUtils.processException(logger, e);
            return "";
        }
    }

}
