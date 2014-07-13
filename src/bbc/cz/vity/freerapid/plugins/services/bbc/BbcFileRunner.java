package cz.vity.freerapid.plugins.services.bbc;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.rtmp.AbstractRtmpRunner;
import cz.vity.freerapid.plugins.services.rtmp.RtmpSession;
import cz.vity.freerapid.plugins.services.rtmp.SwfVerificationHelper;
import cz.vity.freerapid.plugins.services.tor.TorProxyClient;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import cz.vity.freerapid.utilities.LogUtils;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 * @author tong2shot
 */
class BbcFileRunner extends AbstractRtmpRunner {
    private final static Logger logger = Logger.getLogger(BbcFileRunner.class.getName());
    private final static String SWF_URL = "http://emp.bbci.co.uk/emp/releases/smp-flash/revisions/1.9.23/1.9.23_smp.swf";
    private final static String LIMELIGHT_SWF_URL = "http://www.bbc.co.uk/emp/releases/iplayer/revisions/617463_618125_4/617463_618125_4_emp.swf";
    private final static SwfVerificationHelper limelightHelper = new SwfVerificationHelper(LIMELIGHT_SWF_URL);
    private final static SwfVerificationHelper helper = new SwfVerificationHelper(SWF_URL);
    private final static String DEFAULT_EXT = ".flv";
    private final static String MEDIA_SELECTOR_HASH = "7dff7671d0c697fedb1d905d9a121719938b92bf";
    private final static String MEDIA_SELECTOR_ASN = "1";

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
        checkUrl();
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            requestPlaylist(getPid(fileURL));
            checkNameAndSize(getContentAsString());
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize(String playlistContent) throws ErrorDuringDownloadingException {
        String name = PlugUtils.getStringBetween(playlistContent, "<title>", "</title>").replace(": ", " - ");
        httpFile.setFileName(name + DEFAULT_EXT);
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        checkUrl();
        logger.info("Starting download in TASK " + fileURL);
        HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            //sometimes they redirect, set fileURL to the new page
            fileURL = method.getURI().toString();
            requestPlaylist(getPid(fileURL));
            checkNameAndSize(getContentAsString());
            setConfig();
            Matcher matcher = getMatcherAgainstContent("<item[^<>]*?identifier=\"([^<>]+?)\"");
            if (!matcher.find()) {
                throw new PluginImplementationException("Identifier not found");
            }
            String vpid = matcher.group(1);
            String atk = Hex.encodeHexString(DigestUtils.sha(MEDIA_SELECTOR_HASH + vpid));
            String mediaSelector = String.format("http://open.live.bbc.co.uk/mediaselector/5/select/version/2.0/mediaset/pc/vpid/%s/atk/%s/asn/%s/", vpid, atk, MEDIA_SELECTOR_ASN);
            method = getGetMethod(mediaSelector);
            if (config.isEnableTor()) {
                //internally, UK & proxy users don't use Tor
                final TorProxyClient torClient = TorProxyClient.forCountry("gb", client, getPluginService().getPluginContext().getConfigurationStorageSupport());
                if (!torClient.makeRequest(method)) {
                    checkMediaSelectorProblems();
                    throw new ServiceConnectionProblemException();
                }
            } else {
                if (!makeRedirectedRequest(method)) {
                    checkMediaSelectorProblems();
                    throw new ServiceConnectionProblemException();
                }
            }
            checkMediaSelectorProblems();
            Stream selectedStream = getStream(getContentAsString());
            final RtmpSession rtmpSession = getRtmpSession(selectedStream);
            boolean isLimelight = selectedStream.supplier.equalsIgnoreCase("limelight");
            rtmpSession.getConnectParams().put("pageUrl", fileURL);
            rtmpSession.getConnectParams().put("swfUrl", isLimelight ? LIMELIGHT_SWF_URL : SWF_URL);
            if (isLimelight) {
                limelightHelper.setSwfVerification(rtmpSession, client);
            } else {
                helper.setSwfVerification(rtmpSession, client);
            }
            tryDownloadAndSaveFile(rtmpSession);
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkMediaSelectorProblems() throws NotRecoverableDownloadException {
        Matcher matcher = getMatcherAgainstContent("<error id=\"(.+?)\"");
        if (matcher.find()) {
            final String id = matcher.group(1);
            if (id.equals("notavailable")) {
                throw new URLNotAvailableAnymoreException("Media not found");
            } else if (id.equals("notukerror") || id.equals("geolocation")) {
                throw new NotRecoverableDownloadException("This video is not available in your area");
            } else {
                throw new NotRecoverableDownloadException("Error fetching media selector: '" + id + "'");
            }
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        if (getContentAsString().contains("this programme is not available")
                || getContentAsString().contains("Not currently available on BBC iPlayer")) {
            throw new URLNotAvailableAnymoreException("This programme is not available anymore");
        }
        if (getContentAsString().contains("Page not found") || getContentAsString().contains("page was not found")) {
            throw new URLNotAvailableAnymoreException("Page not found");
        }
    }

    private String getPid(String fileUrl) throws PluginImplementationException {
        Matcher matcher = PlugUtils.matcher("/(?:programmes|iplayer(?:/[^/]+)?|i(?:/[^/]+)?)/([a-z\\d]{8})", fileUrl);
        if (!matcher.find()) {
            throw new PluginImplementationException("PID not found");
        }
        return matcher.group(1);
    }

    private void requestPlaylist(String pid) throws Exception {
        GetMethod method = getGetMethod("http://www.bbc.co.uk/iplayer/playlist/" + pid);
        if (!makeRedirectedRequest(method)) {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
        checkProblems();
    }

    private RtmpSession getRtmpSession(Stream stream) {
        return new RtmpSession(stream.server, config.getRtmpPort().getPort(), stream.app, stream.play, stream.encrypted);
    }

    private Stream getStream(String content) throws Exception {
        boolean video = false;
        final List<Stream> streamList = new ArrayList<Stream>();
        try {
            final NodeList mediaElements = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(content.getBytes("UTF-8"))
            ).getElementsByTagName("media");
            for (int i = 0, n = mediaElements.getLength(); i < n; i++) {
                try {
                    final Element mediaElement = (Element) mediaElements.item(i);
                    if (config.isDownloadSubtitles() && mediaElement.getAttribute("kind").equals("captions")) {
                        downloadSubtitle(mediaElement);
                    } else {
                        NodeList connectionElements = mediaElement.getElementsByTagName("connection");
                        for (int j = 0, connectionElementsLength = connectionElements.getLength(); j < connectionElementsLength; j++) {
                            Element connectionElement = (Element) connectionElements.item(j);
                            final Stream stream = Stream.build(mediaElement, connectionElement);
                            if (stream != null) {
                                streamList.add(stream);
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
        if (streamList.isEmpty()) {
            throw new PluginImplementationException("No suitable streams found");
        }

        Stream selectedStream = null;
        if (video) { //video/tv
            final int LOWER_QUALITY_PENALTY = 10;
            int weight = Integer.MAX_VALUE;
            for (Stream stream : streamList) {
                int deltaQ = stream.quality - config.getVideoQuality().getQuality();
                int tempWeight = (deltaQ < 0 ? Math.abs(deltaQ) + LOWER_QUALITY_PENALTY : deltaQ);
                if (tempWeight < weight) {
                    weight = tempWeight;
                    selectedStream = stream;
                }
            }
            if (selectedStream == null) {
                throw new PluginImplementationException("Unable to select stream");
            }
            int selectedQuality = selectedStream.quality;

            //select the highest bitrate for the selected quality            
            int selectedBitrate = Integer.MIN_VALUE;
            for (Stream stream : streamList) {
                if ((stream.quality == selectedQuality) && (stream.bitrate > selectedBitrate)) {
                    selectedBitrate = stream.bitrate;
                    selectedStream = stream;
                }
            }

            //select CDN
            weight = Integer.MIN_VALUE;
            for (Stream stream : streamList) {
                if ((stream.quality == selectedQuality) && (stream.bitrate == selectedBitrate)) {
                    int tempWeight = 0;
                    if (stream.supplier.equalsIgnoreCase(config.getCdn().toString())) {
                        tempWeight = 100;
                    } else if (stream.supplier.equalsIgnoreCase(Cdn.Level3.toString())) { //level3>limelight>akamai
                        tempWeight = 50;
                    } else if (stream.supplier.equalsIgnoreCase(Cdn.Limelight.toString())) {
                        tempWeight = 49;
                    } else if (stream.supplier.equalsIgnoreCase(Cdn.Akamai.toString())) {
                        tempWeight = 48;
                    }
                    if (tempWeight > weight) {
                        weight = tempWeight;
                        selectedStream = stream;
                    }
                }
            }

        } else { //audio/radio, ignore config, always pick the highest bitrate
            selectedStream = Collections.max(streamList, new Comparator<Stream>() {
                @Override
                public int compare(Stream o1, Stream o2) {
                    return Integer.valueOf(o1.bitrate).compareTo(o2.bitrate);
                }
            });
        }

        logger.info("Stream kind : " + (video ? "TV" : "Radio"));
        logger.info("Config settings : " + config);
        logger.info("Selected stream : " + selectedStream);
        return selectedStream;
    }

    private void downloadSubtitle(Element media) throws Exception {
        String subtitleUrl = null;
        try {
            Element connection = (Element) media.getElementsByTagName("connection").item(0);
            subtitleUrl = connection.getAttribute("href");
        } catch (Exception e) {
            LogUtils.processException(logger, e);
        }
        if ((subtitleUrl != null) && !subtitleUrl.isEmpty()) {
            SubtitleDownloader subtitleDownloader = new SubtitleDownloader();
            subtitleDownloader.downloadSubtitle(client, httpFile, subtitleUrl);
        }
    }

    private static class Stream implements Comparable<Stream> {
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

        @Override
        public int compareTo(Stream that) {
            return Integer.valueOf(this.quality).compareTo(that.quality);
        }

    }
}
