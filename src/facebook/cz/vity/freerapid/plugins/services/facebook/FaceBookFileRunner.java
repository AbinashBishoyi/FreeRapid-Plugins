package cz.vity.freerapid.plugins.services.facebook;

import cz.vity.freerapid.plugins.exceptions.BadLoginException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethod;

import java.io.InputStream;
import java.net.URLDecoder;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.zip.GZIPInputStream;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 */
class FaceBookFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(FaceBookFileRunner.class.getName());
    private String url;

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final Matcher matcher = PlugUtils.matcher("[\\?&]v=(\\d+)", fileURL);
        if (!matcher.find()) {
            throw new PluginImplementationException("Error parsing file URL");
        }
        final String dataURL = "http://www.facebook.com/video/external_video.php?v=" + matcher.group(1);
        addCookie(new Cookie(".facebook.com", "locale", "en_US", "/", 86400, false));
        HttpMethod method = getGetMethod(dataURL);
        if (client.getHTTPClient().executeMethod(method) == 200) {
            String content = URLDecoder.decode(forcedGetContentAsString(method), "UTF-8");
            logger.info(content);
            if (content.contains("\"status\":\"login\"")) {
                login();
                method = getGetMethod(dataURL);
                if (client.getHTTPClient().executeMethod(method) != 200) {
                    throw new ServiceConnectionProblemException();
                }
                content = URLDecoder.decode(forcedGetContentAsString(method), "UTF-8");
                logger.info(content);
            }
            if (content.contains("\"status\":\"invalid\"")) {
                throw new URLNotAvailableAnymoreException("This video either has been removed or is not visible due to privacy settings");
            }
            checkNameAndSize(content);
        } else {
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize(final String content) throws Exception {
        final String name = PlugUtils.getStringBetween(content, "\"video_title\":\"", "\"");
        httpFile.setFileName(URLDecoder.decode(name, "UTF-8").replace(" [HQ]", "") + ".mp4");
        url = PlugUtils.getStringBetween(content, "\"video_src\":\"", "\"").replace("\\/", "/");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        runCheck();
        final HttpMethod method = getGetMethod(url);
        if (!tryDownloadAndSaveFile(method)) {
            throw new ServiceConnectionProblemException("Error starting download");
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

            if (getContentAsString().contains("Incorrect username") || getContentAsString().contains("The password you entered is incorrect"))
                throw new BadLoginException("Invalid FaceBook account login information!");
        }
    }

    private static String forcedGetContentAsString(final HttpMethod method) {
        //TODO remove when 0.85 is released
        final StringBuilder sb = new StringBuilder();
        InputStream is = null;
        try {
            is = method.getResponseBodyAsStream();
            if (is != null) {
                if (isGzip(method)) {
                    is = new GZIPInputStream(is, 1024);
                }
                final byte[] b = new byte[1024];
                int i;
                while ((i = is.read(b)) > -1) {
                    sb.append(new String(b, 0, i, "UTF-8"));
                }
            }
        } catch (Exception e) {
            //ignore
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (Exception e) {
                    //ignore
                }
            }
        }
        return sb.toString();
    }

    private static boolean isGzip(final HttpMethod method) {
        final Header h = method.getResponseHeader("Content-Encoding");
        return h != null && "gzip".equalsIgnoreCase(h.getValue());
    }

}