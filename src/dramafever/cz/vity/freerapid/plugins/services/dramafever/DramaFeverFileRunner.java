package cz.vity.freerapid.plugins.services.dramafever;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.services.rtmp.AbstractRtmpRunner;
import cz.vity.freerapid.plugins.services.rtmp.RtmpSession;
import cz.vity.freerapid.plugins.services.tunlr.Tunlr;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import cz.vity.freerapid.utilities.LogUtils;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author tong2shot
 */
class DramaFeverFileRunner extends AbstractRtmpRunner {
    private final static Logger logger = Logger.getLogger(DramaFeverFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        if (!isSubtitle()) {
            final GetMethod getMethod = getGetMethod(fileURL);
            if (!client.getSettings().isProxySet()) {
                Tunlr.setupMethod(getMethod);
            }
            if (makeRedirectedRequest(getMethod)) {
                checkProblems();
                checkNameAndSize(getContentAsString());
            } else {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
        }
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        final String fileName = PlugUtils.getStringBetween(content, "<title>", "</title>").replace("- Watch Full Episodes Free on DramaFever", "").trim() + ".flv";
        httpFile.setFileName(fileName);
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        if (isSubtitle()) { //download subtitle
            httpFile.setFileName(fileURL.substring(fileURL.lastIndexOf("/") + 1));
            final GetMethod method = getGetMethod(fileURL);
            if (!tryDownloadAndSaveFile(method)) {
                throw new ServiceConnectionProblemException("Error downloading subtitle");
            }
            return;
        }
        final GetMethod method = getGetMethod(fileURL);
        if (!client.getSettings().isProxySet()) {
            Tunlr.setupMethod(method);
        }
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize(getContentAsString());
            if (isHulu()) {
                processHulu();
                return;
            }
            if (getContentAsString().contains("captionUrl:")) { //queue subtitle if exist
                final String captionUrl = PlugUtils.getStringBetween(getContentAsString(), "captionUrl: '", "'");
                final List<URI> list = new LinkedList<URI>();
                try {
                    list.add(new URI(captionUrl));
                } catch (final URISyntaxException e) {
                    LogUtils.processException(logger, e);
                }
                getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, list);
            }
            String videoUrl = URLDecoder.decode("rtmp" + PlugUtils.getStringBetween(getContentAsString(), "url:\"rtmp", "\""), "UTF-8");
            final Matcher matcher = PlugUtils.matcher("(?:rtmpe?://)(.+?)/ondemand/(.+?)\\?(.+)", videoUrl);
            if (!matcher.find()) {
                throw new PluginImplementationException("Error parsing stream URL");
            }
            final String host = matcher.group(1);
            final String play = matcher.group(2);
            final String app = "ondemand?_fcs_vhost=" + host + "&" + matcher.group(3);
            final RtmpSession rtmpSession = new RtmpSession(host, 1935, app, play);
            tryDownloadAndSaveFile(rtmpSession);
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("page you requested can't be found")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        if (contentAsString.contains("our videos are only available")) {
            throw new PluginImplementationException("Sorry, our videos are only available in North and South America");
        }
        if (contentAsString.contains("This title is not yet available")) {
            throw new PluginImplementationException("This title is not yet available on DramaFever");
        }
    }

    private boolean isSubtitle() {
        return fileURL.matches("http://imgdf.*?\\.akamaihd\\.net.*?/sub/.+?");
    }

    private boolean isHulu() {
        return (getContentAsString().contains("var hulu = true;"));
    }

    private void processHulu() throws Exception {
        final String secretKey = "MAZxpK3WwazfARjIpSXKQ9cmg9nPe5wIOOfKuBIfz7bNdat6gQKHj69ZWNWNVB1";
        final String cid;
        if (getContentAsString().contains("$.dfp.cid")) {
            cid = PlugUtils.getStringBetween(getContentAsString(), "$.dfp.cid=\"", "\"");
        } else {
            cid = PlugUtils.getStringBetween(getContentAsString(), "playVideo(\"", "\"");
        }
        final String eid = Base64.encodeBase64URLSafeString(DigestUtils.md5(cid + secretKey)).replace("+", "-").replace("/", "_");
        final HttpMethod method = getMethodBuilder()
                .setReferer(fileURL)
                .setAction("http://r.hulu.com/videos")
                .setParameter("eid", eid)
                .setParameter("include", "video_assets")
                .setParameter("include_eos", "1")
                .setParameter("_language", "en")
                .setParameter("_package_group_id", "1")
                .setParameter("_region", "US")
                .toGetMethod();
        if (!makeRedirectedRequest(method)) {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
        checkProblems();
        final String videoId = PlugUtils.getStringBetween(getContentAsString(), "<id type=\"integer\">", "</id>");
        final List<URI> list = new LinkedList<URI>();
        try {
            list.add(new URI("http://www.hulu.com/watch/" + videoId));
        } catch (final URISyntaxException e) {
            LogUtils.processException(logger, e);
        }
        getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, list);
        httpFile.setState(DownloadState.COMPLETED);
        httpFile.getProperties().put("removeCompleted", true);
    }

}