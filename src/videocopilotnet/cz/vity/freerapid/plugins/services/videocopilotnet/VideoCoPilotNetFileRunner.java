package cz.vity.freerapid.plugins.services.videocopilotnet;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadClientConsts;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class which contains main code
 *
 * @author tong2shot
 */
class VideoCoPilotNetFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(VideoCoPilotNetFileRunner.class.getName());
    private VideoCoPilotNetSettingsConfig config;

    private void setConfig() throws Exception {
        VideoCoPilotNetServiceImpl service = (VideoCoPilotNetServiceImpl) getPluginService();
        config = service.getConfig();
    }


    //http://www.videocopilot.net/tutorials/explosive_training/
    //http://www.videocopilot.net/tutorial/explosive_training/p1/
    //http://www.videocopilot.net/tutorial/explosive_training/p1/sd/
    //http://video9.videocopilot.net/efc5e19e65fe9ca2cbd9818d45ac92a1/videotutorials/projects/01.zip
    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        if (isProject()) return;
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize(getContentAsString());
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException, UnsupportedEncodingException {
        if (isMainTutorialPage()) {
            if (content.contains("font-size:22px;color:#fff;font-weight:bold")) {
                PlugUtils.checkName(httpFile, content, "<div style=\"font-size:22px;color:#fff;font-weight:bold\">", "</");
            } else {
                PlugUtils.checkName(httpFile, content, "<div class=\"tutorials-landing-details-title\">", "</");
            }
        } else if (isTutorialVideoPage()) {
            httpFile.setFileName(PlugUtils.getStringBetween(content, "<div class=\"tutorial_title\">", "</b>").replace("<b style='color:#f1f1f1'>", "") + ".mp4");
        } else {
            throw new PluginImplementationException("Unknown URL pattern");
        }
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        if (isProject()) {
            downloadProject();
            return;
        }
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            final String contentAsString = getContentAsString();
            checkProblems();
            checkNameAndSize(contentAsString);
            setConfig();
            if (isMainTutorialPage()) {
                parseMainTutorialPage();
            } else if (isTutorialVideoPage()) {
                downloadTutorialVideo();
            } else {
                throw new PluginImplementationException("Unknown URL pattern");
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void downloadProject() throws Exception {
        final String fname;
        if (fileURL.contains("fname=")) { //contains fname param
            fname = URLDecoder.decode(fileURL.substring(fileURL.indexOf("fname=") + "fname=".length()), "UTF-8");
        } else {
            fname = fileURL.substring(fileURL.lastIndexOf("/") + 1);
        }
        httpFile.setFileName(fname);
        fileURL = fileURL.replaceFirst("\\?fname=.+", "");
        setClientParameter(DownloadClientConsts.DONT_USE_HEADER_FILENAME, true);
        final HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL).setAction(fileURL).toHttpMethod();
        if (!tryDownloadAndSaveFile(httpMethod)) {
            checkProblems();
            throw new ServiceConnectionProblemException("Error starting download");
        }
    }

    private void downloadTutorialVideo() throws Exception {
        final String videoUrl = PlugUtils.getStringBetween(getContentAsString(), "'file','", "'");
        final HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL).setAction(videoUrl).toHttpMethod();
        if (!tryDownloadAndSaveFile(httpMethod)) {
            checkProblems();
            throw new ServiceConnectionProblemException("Error starting download");
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("File Not Found")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    //
    private boolean isMainTutorialPage() {
        return fileURL.contains("/tutorials/");
    }

    private boolean isTutorialVideoPage() {
        return fileURL.contains("/tutorial/");
    }

    private boolean isProject() {
        return fileURL.contains("/projects/");
    }

    private void parseMainTutorialPage() throws Exception {
        final List<URI> uriList = new LinkedList<URI>();
        //video
        Matcher matcher = Pattern.compile("<div style=\"float:(?:right|left);margin-right:15px;?(?:padding-top:5px)?\">(.+?)</div>", Pattern.DOTALL).matcher(getContentAsString());
        while (matcher.find()) {
            final Matcher tutorialVideoMatcher = PlugUtils.matcher("href=[\"'](.+?)[\"']", matcher.group(1));
            final List<VideoCoPilotNetVideo> vvList = new LinkedList<VideoCoPilotNetVideo>();
            while (tutorialVideoMatcher.find()) {
                vvList.add(new VideoCoPilotNetVideo(tutorialVideoMatcher.group(1)));
            }
            uriList.add(new URI(Collections.max(vvList).tutorialVideoUrl));
        }

        if (config.isDownloadProject()) {
            //project
            //Ex: http://video9.videocopilot.net/efc5e19e65fe9ca2cbd9818d45ac92a1/videotutorials/projects/01.zip
            String projectUrl = null;
            if (getContentAsString().contains("PROJECT</a>")) {
                try {
                    matcher = PlugUtils.matcher("<a href=[\"']([^<>]+?)[\"'][^<>]+?>PROJECT</a>", getContentAsString());
                    if (matcher.find()) projectUrl = matcher.group(1);
                } catch (Exception ex) {
                    // do nothing
                }
            } else if (getContentAsString().contains("Download Project File")) {
                try {
                    projectUrl = getMethodBuilder().setActionFromAHrefWhereATagContains("Download Project File").getEscapedURI();
                } catch (Exception ex) {
                    // do nothing
                }
            }
            if (projectUrl == null) {
                matcher = PlugUtils.matcher("^(\\d+)\\.", httpFile.getFileName());
                if (matcher.find()) {
                    final String tutorialId = Integer.parseInt(matcher.group(1)) < 10 ? "0" + matcher.group(1) : matcher.group(1);
                    final String testProjectUrl = "http://video9.videocopilot.net/efc5e19e65fe9ca2cbd9818d45ac92a1/videotutorials/projects/" + tutorialId + ".zip";
                    if (isFileInServerExist(testProjectUrl)) projectUrl = testProjectUrl;
                }
            }
            if (projectUrl != null) {
                String fileExt;
                try {
                    fileExt = projectUrl.substring(projectUrl.lastIndexOf("."));
                } catch (Exception StringIndexOutOfBoundsException) { //doesn't have extension
                    fileExt = ".zip";
                }
                uriList.add(new URI(projectUrl + "?fname=" + URLEncoder.encode(httpFile.getFileName(), "UTF-8") + fileExt)); //add fname param to projectUrl
            }
        }

        if (uriList.isEmpty()) throw new PluginImplementationException("No video or project found");
        getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, uriList);
        httpFile.getProperties().put("removeCompleted", true);
    }

    private boolean isFileInServerExist(final String url) throws Exception {
        final HttpMethod method = getMethodBuilder().setReferer(fileURL).setAction(url).toGetMethod();
        processHttpMethod(method);
        return !((method.getStatusCode() == HttpStatus.SC_NOT_FOUND) || (method.getStatusCode() == HttpStatus.SC_INTERNAL_SERVER_ERROR));
    }

    private void processHttpMethod(HttpMethod method) throws IOException {
        if (client.getHTTPClient().getHostConfiguration().getProtocol() != null) {
            client.getHTTPClient().getHostConfiguration().setHost(method.getURI().getHost(), 80, client.getHTTPClient().getHostConfiguration().getProtocol());
        }
        client.getHTTPClient().executeMethod(method);
    }

    private class VideoCoPilotNetVideo implements Comparable<VideoCoPilotNetVideo> {
        private final String tutorialVideoUrl;
        private int weight;

        public VideoCoPilotNetVideo(final String tutorialVideoUrl) {
            this.tutorialVideoUrl = tutorialVideoUrl;
            calcWeight();
        }

        public void calcWeight() {
            weight = 0;
            final VideoQuality videoQuality = tutorialVideoUrl.contains("/sd/") ? VideoQuality.SD : VideoQuality.HD;
            if (config.getVideoQuality().equals(videoQuality)) {
                weight = 100;
            } else if (videoQuality.equals(VideoQuality.HD)) { //HD > SD
                weight = 50;
            } else if (videoQuality.equals(VideoQuality.SD)) {
                weight = 49;
            }
        }

        @Override
        public int compareTo(final VideoCoPilotNetVideo that) {
            return Integer.valueOf(this.weight).compareTo(that.weight);
        }
    }


}