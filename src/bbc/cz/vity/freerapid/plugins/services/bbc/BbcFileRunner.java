package cz.vity.freerapid.plugins.services.bbc;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.rtmp.AbstractRtmpRunner;
import cz.vity.freerapid.plugins.services.rtmp.RtmpSession;
import cz.vity.freerapid.plugins.services.rtmp.SwfVerificationHelper;
import cz.vity.freerapid.plugins.services.tunlr.Tunlr;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import cz.vity.freerapid.utilities.LogUtils;
import jlibs.core.net.URLUtil;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 * @author tong2shot
 */
class BbcFileRunner extends AbstractRtmpRunner {
    private final static Logger logger = Logger.getLogger(BbcFileRunner.class.getName());
    private final static String SWF_URL = "http://www.bbc.co.uk/emp/releases/iplayer/revisions/617463_618125_4/617463_618125_4_emp.swf";
    private final static SwfVerificationHelper helper = new SwfVerificationHelper(SWF_URL);
    private final static String DEFAULT_EXT = ".flv";
    private final static String SUBTITLE_FILENAME_PARAM = "fname";

    private SettingsConfig config;

    private void setConfig() throws Exception {
        final BbcServiceImpl service = (BbcServiceImpl) getPluginService();
        config = service.getConfig();
    }

    private void checkUrl() {
        fileURL = fileURL.replace("/programmes/", "/iplayer/episode/");
    }

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        if (isSubtitle(fileURL)) {
            return;
        }
        checkUrl();
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        String name;
        final Matcher matcher = getMatcherAgainstContent("<div class=\"module\" id=\"programme-info\">\\s*?<h2>(.+?)<span class=\"blq-hide\"> - </span><span>(.*?)</span></h2>");
        if (matcher.find()) {
            final String series = matcher.group(1).replace(": ", " - ");
            final String episode = matcher.group(2).replace(": ", " - ");
            name = series + (episode.isEmpty() ? "" : " - " + episode);
        } else {
            try {
                name = PlugUtils.getStringBetween(getContentAsString(), "emp.setEpisodeTitle(\"", "\"").replace("\\/", ".").replace(": ", " - ");
            } catch (PluginImplementationException e1) {
                try {
                    name = PlugUtils.getStringBetween(getContentAsString(), "<meta name=\"title\" content=\"", "\" />");
                } catch (PluginImplementationException e2) {
                    throw new PluginImplementationException("File name not found");
                }
            }
        }
        httpFile.setFileName(name + DEFAULT_EXT);
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        checkUrl();
        logger.info("Starting download in TASK " + fileURL);

        if (isSubtitle(fileURL)) {
            downloadSubtitle();
            return;
        }

        HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize();
            //sometimes they redirect, set fileURL to the new page
            fileURL = method.getURI().toString();
            setConfig();
            final String pid;
            Matcher matcher = PlugUtils.matcher("/programmes/([a-z\\d]+)", fileURL);
            if (matcher.find()) {
                method = getGetMethod("http://www.bbc.co.uk/iplayer/playlist/" + matcher.group(1));
                if (!makeRedirectedRequest(method)) {
                    throw new ServiceConnectionProblemException();
                }
                matcher = getMatcherAgainstContent("<item[^<>]*?identifier=\"([^<>]+?)\"");
                if (!matcher.find()) {
                    throw new PluginImplementationException("Identifier not found");
                }
                pid = matcher.group(1);
            } else {
                matcher = getMatcherAgainstContent("emp\\.setPid\\(\".+?\", \"(.+?)\"\\);");
                if (!matcher.find()) {
                    throw new PluginImplementationException("PID not found");
                }
                pid = matcher.group(1);
            }
            String mediaSelector;
            try {
                mediaSelector = PlugUtils.getStringBetween(getContentAsString(), "\"my_mediaselector_json_url\":\"", "\"").replace("\\/", "/").replace("/all/", "/stream/");
            } catch (PluginImplementationException e) {
                mediaSelector = "http://www.bbc.co.uk/mediaselector/4/mtis/stream/" + pid + "?cb=" + new Random().nextInt(100000);
            }
            method = getGetMethod(mediaSelector);
            if (!client.getSettings().isProxySet()) {
                Tunlr.setupMethod(method);
            }
            if (!makeRedirectedRequest(method)) {
                throw new ServiceConnectionProblemException();
            }
            matcher = getMatcherAgainstContent("<error id=\"(.+?)\"");
            if (matcher.find()) {
                final String id = matcher.group(1);
                if (id.equals("notavailable")) {
                    throw new URLNotAvailableAnymoreException("Playlist not found");
                } else if (id.equals("notukerror")) {
                    throw new NotRecoverableDownloadException("This video is not available in your area");
                } else {
                    throw new NotRecoverableDownloadException("Error fetching playlist: '" + id + "'");
                }
            }

            final RtmpSession rtmpSession = getRtmpSession(getStream(getContentAsString()));
            rtmpSession.getConnectParams().put("pageUrl", fileURL);
            rtmpSession.getConnectParams().put("swfUrl", SWF_URL);
            helper.setSwfVerification(rtmpSession, client);
            tryDownloadAndSaveFile(rtmpSession);
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        if (getContentAsString().contains("this programme is not available")) {
            throw new URLNotAvailableAnymoreException("This programme is not available anymore");
        }
        if (getContentAsString().contains("Page not found") || getContentAsString().contains("page was not found")) {
            throw new URLNotAvailableAnymoreException("Page not found");
        }
    }

    private RtmpSession getRtmpSession(Stream stream) {
        return new RtmpSession(stream.server, 1935, stream.app, stream.play, stream.encrypted);
    }

    private Stream getStream(String content) throws Exception {
        boolean video = false;
        final List<Stream> list = new ArrayList<Stream>();
        //streamMap
        //For radio : key=bitrate, value=stream
        //For TV    : key=quality, value=stream
        //sorted by key, ascending
        final TreeMap<Integer, Stream> streamMap = new TreeMap<Integer, Stream>();
        try {
            final NodeList mediaElements = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(content.getBytes("UTF-8"))
            ).getElementsByTagName("media");
            for (int i = 0, n = mediaElements.getLength(); i < n; i++) {
                try {
                    final Element mediaElement = (Element) mediaElements.item(i);
                    if (config.isDownloadSubtitles() && mediaElement.getAttribute("kind").equals("captions")) {
                        queueSubtitle(mediaElement);
                    } else {
                        NodeList connectionElements = mediaElement.getElementsByTagName("connection");
                        for (int j = 0, connectionElementsLength = connectionElements.getLength(); j < connectionElementsLength; j++) {
                            Element connectionElement = (Element) connectionElements.item(j);
                            final Stream stream = Stream.build(mediaElement, connectionElement);
                            if (stream != null) {
                                list.add(stream);
                                if (!video && mediaElement.getAttribute("kind").equals("video")) {
                                    video = true;
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    LogUtils.processException(logger, e);
                }
            }
        } catch (Exception e) {
            throw new PluginImplementationException("Error parsing playlist XML", e);
        }
        if (list.isEmpty()) {
            throw new PluginImplementationException("No suitable streams found");
        }

        for (Stream stream : list) {
            if (video) {
                int quality = stream.quality;
                if (streamMap.containsKey(quality) && (streamMap.get(quality).bitrate > stream.bitrate)) { //put the highest bitrate on same quality
                    continue;
                }
                if ((streamMap.containsKey(quality) && (streamMap.get(quality).supplier.equals("limelight"))) || stream.supplier.equals("akamai")) { //akamai is not downloadable, prefer limelight
                    continue;
                }
                streamMap.put(quality, stream); //For TV : key=quality, value=stream
            } else {
                streamMap.put(stream.bitrate, stream); //For radio : key=bitrate, value=stream
            }
        }

        Stream selectedStream = null;
        if (video) { //video/tv
            if (config.getVideoQuality() == VideoQuality.Highest) {
                selectedStream = streamMap.get(streamMap.lastKey());
            } else if (config.getVideoQuality() == VideoQuality.Lowest) {
                selectedStream = streamMap.get(streamMap.firstKey());
            } else {
                final int LOWER_QUALITY_PENALTY = 10;
                int weight = Integer.MAX_VALUE;
                for (Map.Entry<Integer, Stream> streamEntry : streamMap.entrySet()) {
                    Stream stream = streamEntry.getValue();
                    int deltaQ = stream.quality - config.getVideoQuality().getQuality();
                    int tempWeight = (deltaQ < 0 ? Math.abs(deltaQ) + LOWER_QUALITY_PENALTY : deltaQ);
                    if (tempWeight < weight) {
                        weight = tempWeight;
                        selectedStream = stream;
                    }
                }
            }
        } else { //audio/radio, ignore config, always pick the highest bitrate
            selectedStream = streamMap.get(streamMap.lastKey());
        }

        logger.info("Stream kind : " + (video ? "TV" : "Radio"));
        if (video) {
            logger.info("Config settings : " + config);
        }
        logger.info("Selected stream : " + selectedStream);
        return selectedStream;
    }

    private boolean isSubtitle(String fileUrl) {
        return fileUrl.contains("/subtitles/");
    }

    private void queueSubtitle(Element media) throws Exception {
        Element connection = (Element) media.getElementsByTagName("connection").item(0);
        String subtitleUrl = connection.getAttribute("href") + "?" + SUBTITLE_FILENAME_PARAM + "="
                + URLEncoder.encode(httpFile.getFileName().replaceFirst(Pattern.quote(DEFAULT_EXT) + "$", ""), "UTF-8"); //add fname param
        List<URI> uriList = new LinkedList<URI>();
        uriList.add(new URI(new org.apache.commons.httpclient.URI(subtitleUrl, false, "UTF-8").toString()));
        if (uriList.isEmpty()) {
            logger.warning("No subtitles found");
        }
        getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, uriList);
    }

    private void downloadSubtitle() throws Exception {
        URL url = new URL(fileURL);
        String filename = URLUtil.getQueryParams(url.toString(), "UTF-8").get(SUBTITLE_FILENAME_PARAM);
        if (filename == null) {
            throw new PluginImplementationException("File name not found");
        }
        httpFile.setFileName(URLDecoder.decode(filename, "UTF-8") + ".srt");
        fileURL = url.getProtocol() + "://" + url.getAuthority() + url.getPath();

        HttpMethod method = getGetMethod(fileURL);
        if (!makeRedirectedRequest(method)) {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
        checkProblems();

        //Timed Text to SubRip
        StringBuilder subtitleSb = new StringBuilder();
        try {
            Element body = (Element) DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(getContentAsString().getBytes("UTF-8"))).getElementsByTagName("body").item(0);
            NodeList pElements = body.getElementsByTagName("p");
            for (int i = 0, pElementsLength = pElements.getLength(); i < pElementsLength; i++) {
                Element pElement = (Element) pElements.item(i);
                subtitleSb.append(i + 1).append("\n");
                subtitleSb.append(pElement.getAttribute("begin").replace(".", ",").replaceFirst(",(\\d{2})$", ",0$1").replaceFirst(",(\\d)$", ",00$1")); //pad out, 3 digits
                subtitleSb.append(" --> ");
                subtitleSb.append(pElement.getAttribute("end").replace(".", ",").replaceFirst(",(\\d{2})$", ",0$1").replaceFirst(",(\\d)$", ",00$1"));
                subtitleSb.append("\n");
                addSubtitleElement(subtitleSb, pElement.getChildNodes(), pElement.getChildNodes().getLength(), 0);
                subtitleSb.append("\n\n");
            }
        } catch (Exception e) {
            LogUtils.processException(logger, e);
        }

        final byte[] subtitle = subtitleSb.toString().getBytes("UTF-8");
        httpFile.setFileSize(subtitle.length);
        try {
            downloadTask.saveToFile(new ByteArrayInputStream(subtitle));
        } catch (Exception e) {
            LogUtils.processException(logger, e);
            throw new PluginImplementationException("Error saving subtitle", e);
        }
    }

    private void addSubtitleElement(StringBuilder sb, NodeList childNodes, int childNodeLength, int childNodeCounter) throws PluginImplementationException {
        if (childNodeCounter < childNodeLength) {
            Node childNode = childNodes.item(childNodeCounter);
            if (childNode.getNodeName().equals("br")) {
                sb.append("\n");
            } else if (childNode.getNodeName().equals("#text")) {
                sb.append(PlugUtils.unescapeUnicode(childNode.getNodeValue().trim()));
            } else if (childNode.getNodeName().equals("span")) {
                addSubtitleElement(sb, childNode.getChildNodes(), childNode.getChildNodes().getLength(), 0);
            }
            addSubtitleElement(sb, childNodes, childNodeLength, childNodeCounter + 1);
        }
    }

    private static class Stream {
        private final String server;
        private final String app;
        private final String play;
        private final boolean encrypted;
        private final int bitrate;
        private final int quality;
        private final String supplier;

        public static Stream build(final Element media, final Element connection) {
            String protocol = connection.getAttribute("protocol");
            if (protocol == null || protocol.isEmpty()) {
                protocol = connection.getAttribute("href");
            }
            if (protocol == null || protocol.isEmpty() || !protocol.startsWith("rtmp")) {
                logger.info("Not supported: " + media.getAttribute("service"));
                return null;//of what they serve, only RTMP streams are supported at the moment
            }
            final String server = connection.getAttribute("server");
            String app = connection.getAttribute("application");
            app = (app == null || app.isEmpty() ? "ondemand" : app) + "?" + PlugUtils.replaceEntities(connection.getAttribute("authString"));
            final String play = connection.getAttribute("identifier");
            final boolean encrypted = protocol.startsWith("rtmpe") || protocol.startsWith("rtmpte");
            final int bitrate = Integer.parseInt(media.getAttribute("bitrate"));
            final boolean video = media.getAttribute("kind").equals("video");
            final int quality = video ? Integer.parseInt(media.getAttribute("height")) : -1; //height as quality;
            final String supplier = connection.getAttribute("supplier");
            return new Stream(server, app, play, encrypted, bitrate, quality, supplier);
        }

        private Stream(String server, String app, String play, boolean encrypted, int bitrate, int quality, String supplier) {
            this.server = server;
            this.app = app;
            this.play = play;
            this.encrypted = encrypted;
            this.bitrate = bitrate;
            this.quality = quality;
            this.supplier = supplier;
            logger.info("Found stream : " + this);
        }

        @Override
        public String toString() {
            return "Stream{" +
                    "server='" + server + '\'' +
                    ", app='" + app + '\'' +
                    ", play='" + play + '\'' +
                    ", encrypted=" + encrypted +
                    ", bitrate=" + bitrate +
                    ", quality=" + quality +
                    ", supplier='" + supplier + '\'' +
                    '}';
        }
    }
}