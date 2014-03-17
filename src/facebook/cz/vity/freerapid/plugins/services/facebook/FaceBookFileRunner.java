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

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        addCookie(new Cookie(".facebook.com", "locale", "en_US", "/", 86400, false));
        client.setReferer(fileURL);
        HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            if (getContentAsString().contains("content is currently unavailable")) {
                login();
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
                final String fileName = URLDecoder.decode(PlugUtils.unescapeUnicode(PlugUtils.getStringBetween(getContentAsString(), "\"video_title\",\"", "\"")), "UTF-8");
                httpFile.setFileName(fileName.replace(" [HQ]", "") + ".mp4");
                String videoUrl;
                if (getContentAsString().contains("\"highqual_src\"")) {  //high quality as default
                    videoUrl = PlugUtils.getStringBetween(getContentAsString(), "\"highqual_src\",\"", "\"");
                } else {
                    videoUrl = PlugUtils.getStringBetween(getContentAsString(), "\"video_src\",\"", "\"");
                }
                videoUrl = URLDecoder.decode(PlugUtils.unescapeUnicode(videoUrl), "UTF-8");
                method = getGetMethod(videoUrl);
            } else { //pic
                final MethodBuilder methodBuilder = getMethodBuilder()
                        .setReferer(fileURL)
                        .setActionFromAHrefWhereATagContains("Download");
                final Matcher matcher = PlugUtils.matcher("http://.+?/([^/]+)(?:\\?.+?)$", methodBuilder.getAction());
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

    private void login() throws Exception {
        synchronized (FaceBookFileRunner.class) {
            FaceBookServiceImpl service = (FaceBookServiceImpl) getPluginService();
            PremiumAccount pa = service.getConfig();
            if (!pa.isSet()) {
                pa = service.showConfigDialog();
                if (pa == null || !pa.isSet()) {
                    throw new BadLoginException("No FaceBook account login information!");
                }
            }
            HttpMethod method = getGetMethod("http://www.facebook.com/login.php");
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
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        //
    }

    private boolean isAlbumUrl() {
        return fileURL.matches("http://(?:www\\.)facebook\\.com/(media/set/.+|.+?/videos|video/\\?id=.+)");
    }

    private void processAlbum() throws Exception {
        final Matcher matcher = getMatcherAgainstContent("href=\"(http://(?:www\\.)facebook\\.com/(?:photo\\.php\\?[^#]+?|video/video\\.php?[^#]+?))\"");
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
    }

}