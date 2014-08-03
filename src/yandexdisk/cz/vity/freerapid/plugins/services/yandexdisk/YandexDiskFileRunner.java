package cz.vity.freerapid.plugins.services.yandexdisk;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import jlibs.core.net.URLUtil;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author tong2shot
 */
class YandexDiskFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(YandexDiskFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            fileURL = getMethod.getURI().toString();
            requestPageInEn();
            checkProblems();
            checkNameAndSize(getContentAsString());
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, content, "\"og:title\" content=\"", "\"");
        httpFile.setFileName(PlugUtils.unescapeHtml(httpFile.getFileName()));
        PlugUtils.checkFileSize(httpFile, content, "br/>Size:", "<br");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            fileURL = method.getURI().toString();
            requestPageInEn();
            String mainPageContent = getContentAsString();
            checkProblems();
            checkNameAndSize(mainPageContent);

            URL url = new URL(fileURL);
            HttpMethod httpMethod = getMethodBuilder()
                    .setReferer(fileURL)
                    .setAction("https://" + url.getAuthority() + "/handlers.jsx") //https is mandatory
                    .setParameter("_ckey", getCkey(mainPageContent))
                    .setParameter("_name", "getLinkFileDownload")
                    .setParameter("hash", getHash(mainPageContent))
                    .setParameter("tld", "com")
                    .setAjax()
                    .toPostMethod();
            httpMethod.setRequestHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            if (!makeRedirectedRequest(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }

            final String downloadURL;
            String contentType = null;
            try {
                downloadURL = PlugUtils.replaceEntities(PlugUtils.getStringBetween(getContentAsString(), "\"url\":\"", "\""));
            } catch (PluginImplementationException e) {
                throw new PluginImplementationException("Error getting download URL");
            }
            logger.info("Download URL: " + downloadURL);
            try {
                contentType = URLDecoder.decode(URLUtil.getQueryParams(downloadURL, "UTF-8").get("content_type"), "UTF-8");
            } catch (Exception e) {
                //
            }
            if (contentType != null) {
                logger.info("Content type: " + contentType);
                setFileStreamContentTypes(contentType);
            }

            httpMethod = getMethodBuilder()
                    .setReferer(fileURL)
                    .setAction(downloadURL)
                    .toGetMethod();
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
        if (contentAsString.contains("was removed or not found")
                || contentAsString.contains("could not be found")
                || contentAsString.contains("bad formed path")
                || contentAsString.contains("resource not found")
                || contentAsString.contains("may have deleted file")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    private String getCkey(String content) throws Exception {
        /*
        URL url = new URL(fileURL);
        HttpMethod method = getMethodBuilder()
                .setReferer(fileURL)
                .setAction("https://disk.yandex.com/public/auth.jsx")
                .setParameter("locale", "en")
                .setParameter("path", url.getPath())
                .toPostMethod();
        method.setRequestHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        method.setRequestHeader("Origin", url.getProtocol() + "://" + url.getAuthority());
        if (!makeRedirectedRequest(method)) {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
        checkProblems();
        */
        String ckey;
        Matcher matcher = PlugUtils.matcher("\"ckey(?:_local)?\":\"([^\"]*?)\"", content);
        if (!matcher.find() || (ckey = matcher.group(1).trim()).isEmpty()) {
            throw new PluginImplementationException("Error getting ckey");
        }
        logger.info("ckey: " + ckey);
        return ckey;
    }

    private String getHash(String content) throws Exception {
        String hash;
        final Matcher matcher = PlugUtils.matcher("\"hash\":\"([^\"]*?)\"", content);
        if (!matcher.find() || (hash = matcher.group(1).trim()).isEmpty()) {
            throw new PluginImplementationException("Error getting hash");
        }
        logger.info("Hash: " + hash);
        return hash;
    }

    //They don't provide cookie mechanism to choose locale,
    //so we have to request the page in en if redirected to non-en locale.
    private void requestPageInEn() throws ErrorDuringDownloadingException, IOException {
        if (PlugUtils.find("&locale=(?!en)", fileURL)) {
            fileURL = fileURL.replaceFirst("&locale=[a-z]{2}", "&locale=en");
            GetMethod getMethod = getGetMethod(fileURL);
            if (!makeRedirectedRequest(getMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            fileURL = getMethod.getURI().toString();
        }
    }

}