package cz.vity.freerapid.plugins.services.megavideo_premium;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.net.URL;
import java.net.URLDecoder;
import java.util.logging.Logger;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 */
class MegaVideoFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(MegaVideoFileRunner.class.getName());
    private String HTTP_SITE = "http://www.megavideo.com";
    private boolean badConfig = false;

    @Override
    public void run() throws Exception {
        super.run();

        if (fileURL.contains("megaporn.com")) {
            HTTP_SITE = "http://www.megaporn.com/video";
        }
        if (fileURL.contains("d=")) {
            fileURL = fileURL.replace("megavideo.com", "megaupload.com").replace("/video/", "/");
            httpFile.setNewURL(new URL(fileURL));
            httpFile.setPluginID("");
            httpFile.setState(DownloadState.QUEUED);
            return;
        }

        logger.info("Starting download in TASK " + fileURL);
        addCookie(new Cookie(".megavideo.com", "l", "en", "/", 86400, false));
        addCookie(new Cookie(".megaporn.com", "l", "en", "/", 86400, false));

        login();

        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();

            String user = null;
            for (final Cookie cookie : client.getHTTPClient().getState().getCookies()) {
                if (cookie.getName().equals("user")) {
                    user = cookie.getValue();
                    break;
                }
            }
            if (user == null) throw new PluginImplementationException("Issue with user cookie");

            final String video = fileURL.substring(fileURL.lastIndexOf("v=") + 2);

            final String action = HTTP_SITE + "/xml/player_login.php?u=" + user + "&v=" + video;

            final HttpMethod xmlMethod = getMethodBuilder().setReferer(fileURL).setAction(action).toGetMethod();
            if (!makeRedirectedRequest(xmlMethod))
                throw new ServiceConnectionProblemException("Error fetching XML");

            final String content = getContentAsString();
            if (content.contains("type=\"regular\""))
                throw new NotRecoverableDownloadException("Account is not premium!");
            if (content.contains("type=\"none\""))
                throw new NotRecoverableDownloadException("Invalid MegaVideo Premium account login information!");
            if (!content.contains("downloadurl=\""))
                throw new URLNotAvailableAnymoreException("File not found");

            final String downloadURL = URLDecoder.decode(PlugUtils.getStringBetween(content, "downloadurl=\"", "\""), "UTF-8");

            logger.info("Download URL: " + downloadURL);

            httpFile.setFileName(downloadURL.substring(downloadURL.lastIndexOf("/") + 1));

            final HttpMethod downloadMethod = getMethodBuilder().setReferer(fileURL).setAction(downloadURL).toGetMethod();
            if (!tryDownloadAndSaveFile(downloadMethod)) {
                checkProblems();
                logger.warning(getContentAsString());
                throw new ServiceConnectionProblemException("Error starting download");
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        if (content.contains("This video is unavailable") || content.contains("<h1>Not Found</h1>")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        if (content.contains("trying to access is temporarily unavailable")) {
            throw new ServiceConnectionProblemException("The file you are trying to access is temporarily unavailable");
        }
        if (content.contains("We have detected an elevated number of requests")) {
            throw new ServiceConnectionProblemException("We have detected an elevated number of requests");
        }
    }

    private void login() throws Exception {
        synchronized (MegaVideoFileRunner.class) {
            MegaVideoServiceImpl service = (MegaVideoServiceImpl) getPluginService();
            PremiumAccount pa = service.getConfig();
            if (!pa.isSet() || badConfig) {
                pa = service.showConfigDialog();
                if (pa == null || !pa.isSet()) {
                    throw new NotRecoverableDownloadException("No MegaVideo Premium account login information!");
                }
                badConfig = false;
            }

            final HttpMethod httpMethod = getMethodBuilder()
                    .setAction(HTTP_SITE + "/?s=signup")
                    .setParameter("action", "login")
                    .setParameter("nickname", pa.getUsername())
                    .setParameter("password", pa.getPassword())
                    .toPostMethod();
            if (!makeRedirectedRequest(httpMethod))
                throw new ServiceConnectionProblemException("Error posting login info");

            if (getContentAsString().contains("your login was incorrect")) {
                if (HTTP_SITE.equals("http://www.megaporn.com/video"))
                    throw new NotRecoverableDownloadException("Invalid MegaPorn login info. Please note that MegaPorn uses different accounts than MegaVideo.");

                throw new NotRecoverableDownloadException("Invalid MegaVideo Premium account login information!");
            }
        }
    }

}