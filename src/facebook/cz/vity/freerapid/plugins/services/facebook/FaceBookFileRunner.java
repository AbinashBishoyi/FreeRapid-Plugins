package cz.vity.freerapid.plugins.services.facebook;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethod;

import java.net.URI;
import java.net.URLDecoder;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 * @author tong2shot
 */
class FaceBookFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(FaceBookFileRunner.class.getName());
    private static boolean isLoggedIn = false;
    private static Cookie[] cookies;

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        addCookie(new Cookie(".facebook.com", "locale", "en_US", "/", 86400, false));
        addCookie(new Cookie(".facebook.com", "datr", "ABCDEFG", "/", 86400, false)); //so we can get full items in album, instead of 28 items
        eatCookies();
        client.setReferer(fileURL);
        HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            if (getContentAsString().contains("content is currently unavailable")) {
                login();
                eatCookies(); // to make sure other threads eat cookies
                method = getGetMethod(fileURL);
                if (!makeRedirectedRequest(method)) {
                    checkProblems();
                    throw new ServiceConnectionProblemException();
                }
                if (getContentAsString().contains("content is currently unavailable")) {
                    throw new URLNotAvailableAnymoreException("The link may have expired, or you may not have permission");
                }
                checkProblems();
            }
            if (isAlbumUrl()) {
                processAlbum();
                return;
            }
            if (getContentAsString().contains("new SWFObject")) { //video
                if (getContentAsString().contains("\"status\":\"invalid\"")) {
                    throw new URLNotAvailableAnymoreException("This video either has been removed or is not visible due to privacy settings");
                }
                //the unicode directional formatting codes confuse Matcher, so remove them
                final String content = getContentAsString().replaceAll("[\\u202A\\u202B\\u202C]", "");
                final String name = PlugUtils.getStringBetween(content, "<title id=\"pageTitle\">", "| Facebook</title>");
                httpFile.setFileName(name + ".mp4");
                final String videoData = URLDecoder.decode(PlugUtils.unescapeUnicode(PlugUtils.getStringBetween(getContentAsString(), "\"params\",\"", "\"")), "UTF-8");
                final String videoUrl;
                if (!videoData.contains("\"hd_src\":null")) {  //high quality as default
                    videoUrl = PlugUtils.getStringBetween(videoData, "\"hd_src\":\"", "\"");
                } else {
                    videoUrl = PlugUtils.getStringBetween(videoData, "\"sd_src\":\"", "\"");
                }
                method = getGetMethod(videoUrl.replace("\\/", "/"));
            } else { //pic
                final MethodBuilder methodBuilder;
                //language cookie doesn't seem to work, search link from regex, instead of grabbing link that contains "Download" token.
                Matcher matcher = getMatcherAgainstContent("<a class=\"fbPhotosPhotoActionsItem\" href=\"(https?://[^>]+?(?:akamaihd\\.net|fbcdn\\.net)/.+?)\"");
                if (matcher.find()) {
                    methodBuilder = getMethodBuilder()
                            .setReferer(fileURL)
                            .setAction(matcher.group(1));
                } else {
                    methodBuilder = getMethodBuilder()
                            .setReferer(fileURL)
                            .setActionFromImgSrcWhereTagContains("fbPhotoImage");
                }
                matcher = PlugUtils.matcher("https?://.+?/([^/]+?)(?:\\?.+?)?$", methodBuilder.getAction());
                if (!matcher.find()) {
                    throw new PluginImplementationException("Error parsing picture url");
                }
                httpFile.setFileName(matcher.group(1));
                method = methodBuilder.toGetMethod();
            }
            if (!tryDownloadAndSaveFile(method)) {
                checkProblems();
                throw new ServiceConnectionProblemException("Error starting download");
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    //If already logged-in, add the cookies. This way we only have to login once for entire FRD session (until we close FRD)
    private void eatCookies() {
        synchronized (FaceBookFileRunner.class) {
            if (isLoggedIn && (cookies.length > 0)) {
                for (Cookie cookie : cookies) {
                    addCookie(cookie);
                }
            }
        }
    }

    //Login is optional, if the content is public then we don't have to login. If content is detected as private then we have to login.
    //Once login, will stay logged-in for entire FRD session, until FRD is closed that is.
    private void login() throws Exception {
        synchronized (FaceBookFileRunner.class) {
            if (isLoggedIn) return; //receiving isLoggedIn signal from other thread
            logger.info("Entering login subroutine...");
            FaceBookServiceImpl service = (FaceBookServiceImpl) getPluginService();
            PremiumAccount pa = service.getConfig();
            if (!pa.isSet()) {
                pa = service.showConfigDialog();
                if (pa == null || !pa.isSet()) {
                    throw new BadLoginException("No FaceBook account login information!");
                }
            }
            HttpMethod method = getGetMethod("https://www.facebook.com/login.php");
            if (!makeRedirectedRequest(method)) {
                throw new ServiceConnectionProblemException();
            }
            method = getMethodBuilder()
                    .setActionFromFormByIndex(1, true)
                    .setReferer(method.getURI().toString())
                    .setParameter("email", pa.getUsername())
                    .setParameter("pass", pa.getPassword())
                    .toPostMethod();
            if (!makeRedirectedRequest(method))
                throw new ServiceConnectionProblemException("Error posting login info");

            if (getContentAsString().contains("Incorrect username") || getContentAsString().contains("The password you entered is incorrect") || getContentAsString().contains("Incorrect Email"))
                throw new BadLoginException("Invalid FaceBook account login information!");
            isLoggedIn = true;
            cookies = getCookies();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        //
    }

    private boolean isAlbumUrl() {
        return fileURL.matches("https?://(?:www\\.)?facebook\\.com/(media/set/.+|.+?/videos|video/\\?id=.+)");
    }

    private void processAlbum() throws Exception {
        final Matcher matcher = getMatcherAgainstContent("href=\"(https?://(?:www\\.)?facebook\\.com/(?:photo\\.php\\?[^#]+?|video/video\\.php\\?[^#]+?))\"");
        final List<URI> uriList = new LinkedList<URI>();
        while (matcher.find()) {
            URI uri = new URI(PlugUtils.unescapeHtml(matcher.group(1)));
            if (!uriList.contains(uri)) {
                uriList.add(uri);
            }
        }
        if (uriList.isEmpty()) {
            throw new PluginImplementationException("No picture/video links found");
        }
        getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, uriList);
        httpFile.getProperties().put("removeCompleted", true);
        logger.info(String.valueOf(uriList.size()));
    }

}