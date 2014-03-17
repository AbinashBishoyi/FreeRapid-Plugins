package cz.vity.freerapid.plugins.services.luckyshare_premium;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethod;

import java.net.URLDecoder;
import java.util.logging.Logger;

/**
 * Class which contains main code
 *
 * @author tong2shot
 */
class LuckyShareFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(LuckyShareFileRunner.class.getName());

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        login();
        HttpMethod httpMethod = getMethodBuilder()
                .setReferer(fileURL)
                .setAction(fileURL)
                .toGetMethod();
        final int httpStatus = client.makeRequest(httpMethod, false);
        if (httpStatus / 100 == 3) {
            final Header locationHeader = httpMethod.getResponseHeader("Location");
            if (locationHeader == null) {
                throw new PluginImplementationException("Invalid redirect");
            }
            final String action = locationHeader.getValue();
            httpFile.setFileName(URLDecoder.decode(findName(action), "UTF-8"));
            httpMethod = getMethodBuilder()
                    .setReferer(fileURL)
                    .setAction(action)
                    .toGetMethod();
        } else {
            checkProblems();
            throw new PluginImplementationException("Error getting file");
        }
        setFileStreamContentTypes("text/plain");
        if (!tryDownloadAndSaveFile(httpMethod)) {
            checkProblems();
            throw new ServiceConnectionProblemException("Error starting download");
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("There is no such file available")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    public void login() throws Exception {
        LuckyShareServiceImpl service = (LuckyShareServiceImpl) getPluginService();
        PremiumAccount pa = service.getConfig();
        synchronized (LuckyShareFileRunner.class) {
            if (!pa.isSet()) {
                pa = service.showConfigDialog();
                if (pa == null || !pa.isSet()) {
                    throw new BadLoginException("No LuckyShare Premium account login information!");
                }
            }
        }
        HttpMethod method = getMethodBuilder()
                .setReferer("http://luckyshare.net/")
                .setAction("http://luckyshare.net/auth/login")
                .toGetMethod();
        if (!makeRedirectedRequest(method)) {
            throw new ServiceConnectionProblemException("Error getting login page");
        }
        final String token = PlugUtils.getStringBetween(getContentAsString(), "name=\"token\" value=\"", "\"");
        method = getMethodBuilder()
                .setReferer("http://luckyshare.net/auth/login")
                .setAction("http://luckyshare.net/auth/login")
                .setParameter("username", pa.getUsername())
                .setParameter("password", pa.getPassword())
                .setParameter("token", token)
                .toPostMethod();
        method.setRequestHeader("Content-Type", "application/x-www-form-urlencoded");
        if (!makeRedirectedRequest(method)) {
            throw new ServiceConnectionProblemException("Error posting login info");
        }
        if (getContentAsString().contains("Invalid username or password")) {
            throw new BadLoginException("Invalid LuckyShare Premium account login information!");
        }
    }

    private static String findName(final String url) {
        final String[] strings = url.split("/");
        for (int i = strings.length - 1; i >= 0; i--) {
            final String s = strings[i].trim();
            if (!s.isEmpty()) {
                return s;
            }
        }
        String s = url.replaceAll(":", "_").trim();
        if (s.startsWith("?"))
            s = s.substring(1);
        if (s.isEmpty()) {
            s = "unknown";
        }
        return s;
    }

}