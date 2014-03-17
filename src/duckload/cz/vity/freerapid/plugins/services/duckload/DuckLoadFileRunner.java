package cz.vity.freerapid.plugins.services.duckload;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethod;

import java.net.URLDecoder;
import java.util.Random;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 */
class DuckLoadFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(DuckLoadFileRunner.class.getName());
    private final static Random random = new Random();

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        addCookie(new Cookie(".duckload.com", "dl_set_lang", "en", "/", 86400, false));
        final HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize() throws Exception {
        Matcher matcher = getMatcherAgainstContent("<title>(.+?) @ DuckLoad\\.com</title>");
        if (matcher.find()) {
            httpFile.setFileName(matcher.group(1));
        } else {
            matcher = PlugUtils.matcher("/([^/]+)$", fileURL);
            if (!matcher.find()) {
                throw new PluginImplementationException("File name not found");
            }
            httpFile.setFileName(URLDecoder.decode(matcher.group(1), "UTF-8"));
        }
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        addCookie(new Cookie(".duckload.com", "dl_set_lang", "en", "/", 86400, false));
        setClientParameter("dontUseHeaderFilename", true);
        HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize();
            downloadTask.sleep(21);
            method = getMethodBuilder().setReferer(fileURL).setBaseURL(fileURL).setActionFromFormByName("form", true).toPostMethod();
            if (makeRedirectedRequest(method)) {
                Matcher matcher = getMatcherAgainstContent("\\(<i>(.+?)</i> <strong>(.+?)</strong>\\)");
                if (matcher.find()) {
                    httpFile.setFileSize(PlugUtils.getFileSizeFromString(matcher.group(1) + matcher.group(2)));
                }
                final String url;
                matcher = getMatcherAgainstContent("<embed src=\"(.+?)\"");
                if (matcher.find()) {
                    url = matcher.group(1);
                } else {
                    final int wait = PlugUtils.getNumberBetween(getContentAsString(), "timetowait=", "&");
                    final String ident = PlugUtils.getStringBetween(getContentAsString(), "ident=", "&");
                    final String token = PlugUtils.getStringBetween(getContentAsString(), "token=", "&");
                    final String filename = PlugUtils.getStringBetween(getContentAsString(), "filename=", "&");
                    url = "/api/as2/link/" + ident + "/" + token + "/" + filename;
                    downloadTask.sleep(wait + 1);
                }
                addGACookies();
                method = getMethodBuilder().setReferer(fileURL).setAction(url).toGetMethod();
                if (!tryDownloadAndSaveFile(method)) {
                    checkProblems();
                    throw new ServiceConnectionProblemException("Error staring download");
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
        final String content = getContentAsString();
        if (content.contains("Datei wurde nicht gefunden")
                || content.contains("<h1>404 - Not Found</h1>")
                || content.contains("download.notfound")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    private void addGACookies() {
        addCookie(new Cookie(".duckload.com", "__utma", String.valueOf(random.nextLong()), "/", 86400, false));
        addCookie(new Cookie(".duckload.com", "__utmb", String.valueOf(random.nextLong()), "/", 86400, false));
        addCookie(new Cookie(".duckload.com", "__utmc", String.valueOf(random.nextLong()), "/", 86400, false));
        addCookie(new Cookie(".duckload.com", "__utmz", String.valueOf(random.nextLong()), "/", 86400, false));
    }

    @Override
    protected String getBaseURL() {
        return "http://www.duckload.com";
    }

}