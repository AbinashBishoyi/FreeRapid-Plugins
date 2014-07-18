package cz.vity.freerapid.plugins.services.youtube;

import com.googlecode.mp4parser.DataSource;
import com.googlecode.mp4parser.FileDataSourceImpl;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.builder.ItunesBuilder;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;
import com.googlecode.mp4parser.authoring.tracks.FlvAacTrackImpl;
import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.services.youtube.srt.Transcription2SrtUtil;
import cz.vity.freerapid.plugins.video2audio.AbstractVideo2AudioRunner;
import cz.vity.freerapid.plugins.webclient.DownloadClientConsts;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import cz.vity.freerapid.plugins.webclient.utils.HttpUtils;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import cz.vity.freerapid.utilities.LogUtils;
import jlibs.core.net.URLUtil;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Kajda
 * @author JPEXS
 * @author ntoskrnl
 * @author tong2shot
 * @since 0.82
 */
class YouTubeRunner extends AbstractVideo2AudioRunner {
    private static final Logger logger = Logger.getLogger(YouTubeRunner.class.getName());
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 6.2; WOW64; rv:30.0) Gecko/20100101 Firefox/30.0";
    private static final String DEFAULT_FILE_EXT = ".flv";
    private static final String DASH_AUDIO_ITAG = "dashaudioitag";
    private static final String SECONDARY_DASH_AUDIO_ITAG = "secondarydashaudioitag"; //as backup, in case the primary fails

    private YouTubeSettingsConfig config;
    private int dashAudioItagValue = -1;
    private int secondaryDashAudioItagValue = -1;

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        setClientParameter(DownloadClientConsts.USER_AGENT, USER_AGENT);
        addCookie(new Cookie(".youtube.com", "PREF", "hl=en", "/", 86400, false));
        if (isAttributionLink()) {
            processAttributionLink();
        }
        if (isVideo()) {
            checkFileProblems();
        }
        if (isDashAudio()) {
            normalizeDashAudioUrl();
        }
        final HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            bypassAgeVerification(method);
            checkProblems();
            checkName();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        setClientParameter(DownloadClientConsts.USER_AGENT, USER_AGENT);
        addCookie(new Cookie(".youtube.com", "PREF", "hl=en", "/", 86400, false));
        setConfig();
        if (isAttributionLink()) {
            processAttributionLink();
        }
        if (checkSubtitles()) {
            return;
        }
        if (isVideo()) {
            checkFileProblems();
        }
        if (isDashAudio()) {
            normalizeDashAudioUrl();
        }
        HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            bypassAgeVerification(method);
            checkProblems();
            fileURL = method.getURI().toString();
            checkName();

            if (isUserPage()) {
                parseUserPage(getUserFromUrl());
                return;
            }
            if (isPlaylist()) {
                parsePlaylist();
                return;
            }
            if (isCourseList()) {
                parseCourseList();
                return;
            }

            String swfUrl = PlugUtils.getStringBetween(getContentAsString(), "\"url\": \"", "\"").replace("\\/", "/");
            String fmtStreamMapContent = PlugUtils.getStringBetween(getContentAsString(), "\"url_encoded_fmt_stream_map\": \"", "\"");
            YouTubeSigDecipher ytSigDecipher = null;
            Map<Integer, YouTubeMedia> afDashStreamMap = new LinkedHashMap<Integer, YouTubeMedia>(); //union between afStreamMap and dashStreamMap
            logger.info("Swf URL : " + swfUrl);
            if (config.isEnableDash()
                    || (config.getDownloadMode() == DownloadMode.convertToAudio)
                    || (config.getDownloadMode() == DownloadMode.extractAudio)) {
                Map<Integer, YouTubeMedia> afStreamMap = null; //streams from 'adaptive_fmts', not to be confused with afDashStreamMap
                Map<Integer, YouTubeMedia> dashStreamMap = null; //streams from 'dashmpd'
                if (getContentAsString().contains("\"adaptive_fmts\": \"")) {
                    String afContent = PlugUtils.getStringBetween(getContentAsString(), "\"adaptive_fmts\": \"", "\"");
                    logger.info("Parsing adaptive_fmts");
                    //'adaptive_fmts' parser is similar with 'url_encoded_fmt_stream_map' parser
                    afStreamMap = getFmtStreamMap(afContent);
                }
                if (getContentAsString().contains("\"dashmpd\": \"")) {
                    String dashUrl = PlugUtils.getStringBetween(getContentAsString(), "\"dashmpd\": \"", "\"").replace("\\/", "/");
                    logger.info("DASH URL : " + dashUrl);
                    if (!(dashUrl.contains("/sig/") || dashUrl.contains("/signature/"))) {  //cipher signature
                        Matcher matcher = PlugUtils.matcher("/s/([^/]+)", dashUrl);
                        if (!matcher.find()) {
                            throw new PluginImplementationException("Cipher signature not found");
                        }
                        String signature = matcher.group(1);
                        ytSigDecipher = getYouTubeSigDecipher(swfUrl);
                        signature = ytSigDecipher.decipher(signature); //deciphered signature
                        dashUrl = dashUrl.replaceFirst("/s/[^/]+", "/signature/" + signature);
                        logger.info("DASH URL (deciphered) : " + dashUrl);
                    }
                    method = getMethodBuilder().setReferer(fileURL).setAction(dashUrl).toGetMethod();
                    setTextContentTypes("video/vnd.mpeg.dash.mpd");
                    if (!makeRedirectedRequest(method)) {
                        checkProblems();
                        throw new ServiceConnectionProblemException();
                    }
                    checkProblems();
                    logger.info("Parsing dashmpd");
                    dashStreamMap = getDashStreamMap(getContentAsString());
                }
                if (afStreamMap != null) {
                    afDashStreamMap.putAll(afStreamMap);
                }
                if (dashStreamMap != null) {
                    afDashStreamMap.putAll(dashStreamMap);
                }
            }

            YouTubeMedia youTubeMedia;
            if (dashAudioItagValue == -1) { //not dash audio
                logger.info("Parsing url_encoded_fmt_stream_map");
                Map<Integer, YouTubeMedia> fmtStreamMap = getFmtStreamMap(fmtStreamMapContent); //streams from 'url_encoded_fmt_stream_map'
                Map<Integer, YouTubeMedia> ytStreamMap = new LinkedHashMap<Integer, YouTubeMedia>(); //union between fmtStreamMap and afDashStreamMap
                ytStreamMap.putAll(fmtStreamMap); //put fmtStreamMap on top of the map
                ytStreamMap.putAll(afDashStreamMap);
                youTubeMedia = getSelectedYouTubeMedia(ytStreamMap);
                if ((youTubeMedia.isDashVideo())) {
                    //fmtStreamMap doesn't contain dash, use afDashStreamMap instead of ytStreamMap
                    queueDashAudio(afDashStreamMap, youTubeMedia);
                }
            } else { //dash audio
                //at this moment dash audio set from afStreamMap is subset of dash audio set from dashStreamMap,
                //so it is ok to get youTubeMedia from dashStreamMap, but it's safer to get it from afDashStream.
                youTubeMedia = afDashStreamMap.get(dashAudioItagValue);
                if (youTubeMedia == null) {
                    throw new PluginImplementationException("DASH audio stream with itag='" + dashAudioItagValue + "' not found");
                }
            }

            Container container = youTubeMedia.getContainer();
            httpFile.setFileName(httpFile.getFileName().replaceFirst(Pattern.quote(DEFAULT_FILE_EXT) + "$", container.getFileExt()));
            logger.info("Config setting : " + config);
            logger.info("Downloading media : " + youTubeMedia);
            setClientParameter(DownloadClientConsts.DONT_USE_HEADER_FILENAME, true);
            if (!tryDownloadAndSaveFile(getGetMethod(getMediaUrl(swfUrl, ytSigDecipher, youTubeMedia)))) {
                if (secondaryDashAudioItagValue != -1) { //try secondary dash audio
                    youTubeMedia = afDashStreamMap.get(secondaryDashAudioItagValue);
                    if (youTubeMedia == null) {
                        throw new PluginImplementationException("DASH audio stream with itag='" + secondaryDashAudioItagValue + "' not found");
                    }
                    logger.info("Primary DASH audio failed, trying to download secondary DASH audio");
                    logger.info("Downloading media : " + youTubeMedia);
                    if (!tryDownloadAndSaveFile(getGetMethod(getMediaUrl(swfUrl, ytSigDecipher, youTubeMedia)))) {
                        checkProblems();
                        throw new ServiceConnectionProblemException("Error downloading secondary DASH audio");
                    }
                } else {
                    checkProblems();
                    throw new ServiceConnectionProblemException("Error starting download");
                }

            } else if (config.getDownloadMode() == DownloadMode.convertToAudio) {
                convertToAudio(youTubeMedia.getAudioBitrate(), (container == Container.mp4) || (container == Container.dash_a));

            } else if (config.getDownloadMode() == DownloadMode.extractAudio) {
                if ((container == Container.flv) && (youTubeMedia.getAudioEncoding().equalsIgnoreCase("MP3"))) { //to mp3
                    //for MP3 track inside FLV container, convertToAudio is extraction, not conversion
                    convertToAudio(youTubeMedia.getAudioBitrate(), false);
                } else if ((container == Container.flv) || (container == Container.mp4) || (container == Container.dash_a)) { //to m4a
                    extractAudio(container);
                } else {
                    throw new PluginImplementationException("Unsupported container : " + container);
                }

            } else if ((config.getDownloadMode() == DownloadMode.downloadVideo)
                    && config.isEnableInternalMultiplexer()
                    && youTubeMedia.isDash()) {
                multiplexDash(container == Container.dash_a);
            }

        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private String getMediaUrl(String swfUrl, YouTubeSigDecipher ytSigDecipher, YouTubeMedia youTubeMedia) throws Exception {
        String videoURL = youTubeMedia.getUrl();
        String signatureInUrl = null;
        try {
            signatureInUrl = URLUtil.getQueryParams(videoURL, "UTF-8").get("signature");
        } catch (Exception e) {
            //
        }
        if (signatureInUrl == null) { //no "signature" param in url
            String signature;
            if (youTubeMedia.isCipherSignature()) { //signature is encrypted
                logger.info("Cipher signature : " + youTubeMedia.getSignature());
                if (ytSigDecipher == null) {
                    ytSigDecipher = getYouTubeSigDecipher(swfUrl);
                }
                signature = ytSigDecipher.decipher(youTubeMedia.getSignature());
                logger.info("Deciphered signature : " + signature);
            } else {
                signature = youTubeMedia.getSignature();
            }
            videoURL += "&signature=" + signature;
        }
        return videoURL;
    }

    private YouTubeSigDecipher getYouTubeSigDecipher(String swfUrl) throws IOException, ServiceConnectionProblemException {
        InputStream is = client.makeRequestForFile(getGetMethod(swfUrl));
        if (is == null) {
            throw new ServiceConnectionProblemException("Error downloading SWF");
        }
        return new YouTubeSigDecipher(is);
    }

    private void checkFileProblems() throws Exception {
        logger.info("Checking file problems");
        HttpMethod method = getGetMethod(String.format("https://gdata.youtube.com/feeds/api/videos/%s?v=2", getIdFromUrl()));
        int httpCode = client.makeRequest(method, true);
        if ((httpCode == HttpStatus.SC_NOT_FOUND)
                || (httpCode == HttpStatus.SC_FORBIDDEN)
                || getContentAsString().contains("ResourceNotFoundException")
                || getContentAsString().contains("ServiceForbiddenException")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    private void checkProblems() {
        //
    }

    private void checkName() throws ErrorDuringDownloadingException {
        try {
            PlugUtils.checkName(httpFile, getContentAsString(), "<meta name=\"title\" content=\"", "\"");
        } catch (final PluginImplementationException e) {
            PlugUtils.checkName(httpFile, getContentAsString(), "<title>", "- YouTube\n</title>");
        }
        String fileName = PlugUtils.unescapeHtml(PlugUtils.unescapeHtml(httpFile.getFileName()));
        if (dashAudioItagValue != -1) {
            fileName += Container.dash_a.getFileExt();
        } else if (isVideo()) {
            fileName += DEFAULT_FILE_EXT;
        }
        httpFile.setFileName(fileName);
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private void setConfig() throws Exception {
        final YouTubeServiceImpl service = (YouTubeServiceImpl) getPluginService();
        config = service.getConfig();
    }

    private Map<Integer, YouTubeMedia> getFmtStreamMap(String content) {
        Map<Integer, YouTubeMedia> fmtStreamMap = new LinkedHashMap<Integer, YouTubeMedia>();
        String fmtStreams[] = content.split(",");
        for (String fmtStream : fmtStreams) {
            try {
                String fmtStreamComponents[] = PlugUtils.unescapeUnicode(fmtStream).split("&"); // \u0026 as separator
                int itag = -1;
                String url = null;
                String signature = null;
                boolean cipherSig = false;
                for (String fmtStreamComponent : fmtStreamComponents) {
                    String fmtStreamComponentParts[] = fmtStreamComponent.split("=");
                    String key = fmtStreamComponentParts[0];
                    String value = fmtStreamComponentParts[1];
                    if (key.equals("itag")) {
                        itag = Integer.parseInt(value);
                    } else if (key.equals("url")) {
                        url = URLDecoder.decode(value, "UTF-8");
                        String sigParam = null;
                        try {
                            sigParam = URLUtil.getQueryParams(url, "UTF-8").get("signature");
                        } catch (Exception e) {
                            //
                        }
                        if (sigParam != null) { //contains "signature" param
                            signature = sigParam;
                        }
                    } else if (key.equals("signature") || key.equals("sig") || key.equals("s")) {
                        signature = value;
                        cipherSig = key.equals("s");
                    }
                }
                if (itag == -1 || url == null || signature == null) {
                    throw new PluginImplementationException("Invalid YouTube media : " + fmtStream);
                }
                YouTubeMedia youTubeMedia = new YouTubeMedia(itag, url, signature, cipherSig);
                logger.info("Found video : " + youTubeMedia);
                fmtStreamMap.put(itag, youTubeMedia);
            } catch (Exception e) {
                LogUtils.processException(logger, e);
            }
        }
        return fmtStreamMap;
    }

    private Map<Integer, YouTubeMedia> getDashStreamMap(String dashContent) throws Exception {
        Map<Integer, YouTubeMedia> dashStreamMap = new LinkedHashMap<Integer, YouTubeMedia>();
        if (dashContent != null) {
            try {
                final NodeList representationElements = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(dashContent
                        .getBytes("UTF-8"))).getElementsByTagName("Representation");
                for (int i = 0, n = representationElements.getLength(); i < n; i++) {
                    try {
                        final Element representationElement = (Element) representationElements.item(i);
                        int itag;
                        String url;
                        String signature = null;
                        boolean cipherSig = false; //assume there is cipher sig in the future, couldn't find sample at the moment.
                        String sigParam = null;

                        itag = Integer.parseInt(representationElement.getAttribute("id"));
                        url = representationElement.getElementsByTagName("BaseURL").item(0).getTextContent();
                        try {
                            sigParam = URLUtil.getQueryParams(url, "UTF-8").get("signature");
                        } catch (Exception e) {
                            //
                        }
                        if (sigParam != null) { //contains "signature" param
                            signature = sigParam;
                        }
                        if (signature == null) {
                            throw new PluginImplementationException("Invalid YouTube DASH media : " + representationElement.getTextContent());
                        }
                        YouTubeMedia youTubeMedia = new YouTubeMedia(itag, url, signature, cipherSig);
                        logger.info("Found DASH media : " + youTubeMedia);
                        dashStreamMap.put(itag, youTubeMedia);
                    } catch (Exception e) {
                        LogUtils.processException(logger, e);
                    }
                }
            } catch (Exception e) {
                throw new PluginImplementationException("Error parsing DASH descriptor", e);
            }
        }
        return dashStreamMap;
    }

    private YouTubeMedia getSelectedYouTubeMedia(Map<Integer, YouTubeMedia> ytMediaMap) throws ErrorDuringDownloadingException {
        if (ytMediaMap.isEmpty()) {
            throw new PluginImplementationException("No available YouTube media");
        }
        int selectedItag = -1;

        if ((config.getDownloadMode() == DownloadMode.convertToAudio) || (config.getDownloadMode() == DownloadMode.extractAudio)) {
            final int LOWER_QUALITY_PENALTY = 5;
            final boolean isConvertToAudio = (config.getDownloadMode() == DownloadMode.convertToAudio);
            int configAudioBitrate = (isConvertToAudio ? config.getConvertAudioQuality().getBitrate() : config.getExtractAudioQuality().getBitrate());

            //select audio bitrate
            int selectedAudioBitrate = -1;
            int weight = Integer.MAX_VALUE;
            for (YouTubeMedia ytMedia : ytMediaMap.values()) {
                if (ytMedia.isDashVideo()
                        || (isConvertToAudio && !ytMedia.isVid2AudSupported())
                        || (!isConvertToAudio && !ytMedia.isAudioExtractSupported())) {
                    continue;
                }
                int audioBitrate = ytMedia.getAudioBitrate();
                int deltaAudioBitrate = audioBitrate - configAudioBitrate;
                int tempWeight = (deltaAudioBitrate < 0 ? Math.abs(deltaAudioBitrate) + LOWER_QUALITY_PENALTY : deltaAudioBitrate);
                if (tempWeight < weight) {
                    weight = tempWeight;
                    selectedAudioBitrate = audioBitrate;
                }
            }
            if (selectedAudioBitrate == -1) {
                throw new PluginImplementationException("Unable to select audio bitrate");
            }

            //calc (the lowest) video quality to get the fittest itag  -> select video quality
            //prefer DASH audio (videoQuality==-1)
            weight = Integer.MAX_VALUE;
            for (YouTubeMedia ytMedia : ytMediaMap.values()) {
                if (ytMedia.isDashVideo()
                        || (isConvertToAudio && !ytMedia.isVid2AudSupported())
                        || (!isConvertToAudio && !ytMedia.isAudioExtractSupported())) {
                    continue;
                }
                if (ytMedia.getAudioBitrate() == selectedAudioBitrate) {
                    int tempWeight = ytMedia.getVideoQuality();
                    if (tempWeight < weight) {
                        weight = tempWeight;
                        selectedItag = ytMedia.getItag();
                    }
                }
            }
            if (selectedItag == -1) {
                throw new PluginImplementationException("Unable to select YouTube media");
            }

        } else { //download video
            //select video quality
            VideoQuality configVideoQuality = config.getVideoQuality();
            if (configVideoQuality == VideoQuality.Highest) {
                selectedItag = Collections.max(ytMediaMap.values(), new Comparator<YouTubeMedia>() {
                    @Override
                    public int compare(YouTubeMedia o1, YouTubeMedia o2) {
                        return Integer.valueOf(o1.getVideoQuality()).compareTo(o2.getVideoQuality());
                    }
                }).getItag();
            } else if (configVideoQuality == VideoQuality.Lowest) {
                for (YouTubeMedia ytMedia : ytMediaMap.values()) {
                    if (ytMedia.isDash()) { //skip DASH
                        continue;
                    }
                    selectedItag = ytMedia.getItag(); //last key of fmtStreamMap
                }
            } else {
                final int LOWER_QUALITY_PENALTY = 10;
                int weight = Integer.MAX_VALUE;
                for (YouTubeMedia ytMedia : ytMediaMap.values()) {
                    int deltaQ = ytMedia.getVideoQuality() - configVideoQuality.getQuality();
                    int tempWeight = (deltaQ < 0 ? Math.abs(deltaQ) + LOWER_QUALITY_PENALTY : deltaQ);
                    if (tempWeight < weight) {
                        weight = tempWeight;
                        selectedItag = ytMedia.getItag();
                    }
                }
            }

            //select container
            final Container configContainer = config.getContainer();
            if (configContainer != Container.Any) {
                final int selectedVideoQuality = ytMediaMap.get(selectedItag).getVideoQuality();
                int weight = Integer.MIN_VALUE;
                for (YouTubeMedia ytMedia : ytMediaMap.values()) {
                    if (ytMedia.getVideoQuality() == selectedVideoQuality) {
                        int tempWeight = 0;
                        Container container = ytMedia.getContainer();
                        if (configContainer == container) {
                            tempWeight = 100;
                        } else if (container == Container.mp4) { //mp4 > flv > webm > 3gp > DASH (H264) > DASH (VP9)
                            tempWeight = 50;
                        } else if (container == Container.flv) {
                            tempWeight = 49;
                        } else if (container == Container.webm) {
                            tempWeight = 48;
                        } else if (container == Container._3gp) {
                            tempWeight = 47;
                        } else if (container == Container.dash_v) {
                            tempWeight = 30;
                        } else if (container == Container.dash_v_vpx) {
                            tempWeight = 10;
                        }
                        if (tempWeight > weight) {
                            weight = tempWeight;
                            selectedItag = ytMedia.getItag();
                        }
                    }
                }
            }
        }

        return ytMediaMap.get(selectedItag);
    }

    private void queueDashAudio(Map<Integer, YouTubeMedia> afDashStreamMap, YouTubeMedia selectedDashVideoStream) throws Exception {
        int selectedItag = -1;
        int secondaryItag = -1; //as backup, in case the primary fails
        int weight = Integer.MIN_VALUE;
        int secondaryWeight = Integer.MIN_VALUE;
        for (YouTubeMedia ytMedia : afDashStreamMap.values()) {
            if ((ytMedia.getContainer() != Container.dash_a) || (ytMedia.getAudioEncoding().equalsIgnoreCase("Vorbis"))) { //skip non DASH audio or Vorbis
                continue;
            }
            int tempWeight = Integer.MIN_VALUE;
            //audio bitrate as criteria, it'd be better if we have videoQ-audioBitRate map for the qualifier
            switch (ytMedia.getAudioBitrate()) {
                case 128:
                    tempWeight = 50;
                    break;
                case 256:
                    tempWeight = (selectedDashVideoStream.getVideoQuality() >= VideoQuality._720.getQuality() ? 51 : 47);
                    break;
                case 48:
                    tempWeight = 48;
                    break;
            }
            if (tempWeight > weight) {
                secondaryWeight = weight;
                weight = tempWeight;
                secondaryItag = selectedItag;
                selectedItag = ytMedia.getItag();
            } else if (tempWeight > secondaryWeight) {
                secondaryWeight = tempWeight;
                secondaryItag = ytMedia.getItag();
            }
        }
        if (selectedItag == -1) {
            throw new PluginImplementationException("DASH audio pair not found");
        }
        logger.info("Queueing DASH audio stream : " + afDashStreamMap.get(selectedItag));
        List<URI> uriList = new LinkedList<URI>();
        String url = fileURL + "&" + DASH_AUDIO_ITAG + "=" + selectedItag + (secondaryItag == -1 ? "" : "&" + SECONDARY_DASH_AUDIO_ITAG + "=" + secondaryItag);
        try {
            uriList.add(new URI(url));
        } catch (final URISyntaxException e) {
            LogUtils.processException(logger, e);
        }
        getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, uriList);
    }

    private boolean isDashAudio() {
        return fileURL.contains("&" + DASH_AUDIO_ITAG + "=");
    }

    private void normalizeDashAudioUrl() throws PluginImplementationException {
        Matcher matcher = PlugUtils.matcher("&" + DASH_AUDIO_ITAG + "=(\\d+)", fileURL);
        if (!matcher.find()) {
            throw new PluginImplementationException("DASH audio itag param not found");
        }
        dashAudioItagValue = Integer.parseInt(matcher.group(1));

        matcher = PlugUtils.matcher("&" + SECONDARY_DASH_AUDIO_ITAG + "=(\\d+)", fileURL);
        if (matcher.find()) {
            secondaryDashAudioItagValue = Integer.parseInt(matcher.group(1));
        }
        fileURL = fileURL.replaceFirst("&" + DASH_AUDIO_ITAG + "=.+", ""); //remove dash audio itag param
    }

    private boolean isVideo() {
        return !isUserPage() && !isPlaylist() && !isCourseList() && !isSubtitles();
    }

    private boolean isAttributionLink() {
        return fileURL.contains("/attribution_link?");
    }

    private void processAttributionLink() throws Exception {
        HttpMethod method = getGetMethod(fileURL);
        if (!makeRedirectedRequest(method)) {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
        checkProblems();
        fileURL = method.getURI().toString();
    }

    private String getIdFromUrl() throws ErrorDuringDownloadingException {
        final Matcher matcher = PlugUtils.matcher("(?:[\\?&]v=|\\.be/)([^\\?&#]+)", fileURL);
        if (!matcher.find()) {
            throw new PluginImplementationException("Error getting video id");
        }
        return matcher.group(1);
    }

    private LinkedList<URI> getURIList(String action, final String URIRegex) throws Exception {
        final LinkedList<URI> uriList = new LinkedList<URI>();
        setFileStreamContentTypes(new String[0], new String[]{"application/atom+xml"});
        do {
            final HttpMethod method = getMethodBuilder()
                    .setReferer(null)
                    .setAction(action)
                    .toGetMethod();
            if (!makeRedirectedRequest(method)) {
                throw new ServiceConnectionProblemException();
            }
            Matcher matcher = getMatcherAgainstContent(URIRegex);
            while (matcher.find()) {
                try {
                    final String link = PlugUtils.replaceEntities(matcher.group(1));
                    final URI uri = new URI(link);
                    if (!uriList.contains(uri)) {
                        uriList.add(uri);
                    }
                } catch (final URISyntaxException e) {
                    LogUtils.processException(logger, e);
                }
            }
            matcher = getMatcherAgainstContent("<link rel='next'.*? href='(.+?)'");
            if (!matcher.find()) {
                break;
            }
            action = PlugUtils.replaceEntities(matcher.group(1));
        } while (getContentAsString().contains("<link rel='next'"));
        return uriList;
    }

    private LinkedList<URI> getVideoURIList(final String action) throws Exception {
        return getURIList(action, "<media:player url='(.+?)(?:&.+?)?'");
    }

    private LinkedList<URI> getLectureCourseMaterialURIList(final String action) throws Exception {
        return getURIList(action, "<yt:material.*? url='(.+?)'");
    }

    private void queueLinks(final List<URI> uriList) throws PluginImplementationException {
        if (uriList.isEmpty()) {
            throw new PluginImplementationException("No video links found");
        }
        getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, uriList);
        httpFile.getProperties().put("removeCompleted", true);
        logger.info(uriList.size() + " videos added");
    }

    private boolean isUserPage() {
        return fileURL.contains("/user/");
    }

    private String getUserFromUrl() throws ErrorDuringDownloadingException {
        final Matcher matcher = PlugUtils.matcher("/user/([^\\?&#/]+)", fileURL);
        if (!matcher.find()) {
            throw new PluginImplementationException("Error getting user id");
        }
        return matcher.group(1);
    }

    //user uploaded video
    //reference : https://developers.google.com/youtube/2.0/developers_guide_protocol#User_Uploaded_Videos
    private void parseUserPage(final String user) throws Exception {
        final String action = "http://gdata.youtube.com/feeds/api/users/" + user + "/uploads";
        final List<URI> uriList = getVideoURIList(action);
        // YouTube returns the videos in descending date order, which is a bit illogical.
        // If the user wants them that way, don't reverse.
        if (!config.isReversePlaylistOrder()) {
            Collections.reverse(uriList);
        }
        queueLinks(uriList);
    }

    private boolean isPlaylist() {
        return fileURL.contains("/playlist?");
    }

    private String getPlaylistIdFromUrl() throws PluginImplementationException {
        final Matcher matcher = PlugUtils.matcher("list=(?:PL|UU|FL)?([^\\?&#/]+)", fileURL);
        if (!matcher.find()) {
            throw new PluginImplementationException("Error getting playlist id");
        }
        return matcher.group(1);
    }

    private String getUserFromContent() throws PluginImplementationException {
        return PlugUtils.getStringBetween(getContentAsString(), "<a class=\"profile-thumb\" href=\"/user/", "\"");
    }

    //Favorite List and Playlist
    //reference : https://developers.google.com/youtube/2.0/developers_guide_protocol#Favorite_Videos
    //reference : https://developers.google.com/youtube/2.0/developers_guide_protocol#Retrieving_a_playlist
    private void parsePlaylist() throws Exception {
        if (fileURL.contains("list=UU")) { //user uploaded video
            final String user = getUserFromContent();
            parseUserPage(user);
        } else if (fileURL.contains("list=FL")) { //favorite list
            final String user = getUserFromContent();
            final String action = String.format("http://gdata.youtube.com/feeds/api/users/%s/favorites", user);
            final List<URI> uriList = getVideoURIList(action);
            queueLinks(uriList);
        } else { //playlist
            final String playlistId = getPlaylistIdFromUrl();
            final String action = String.format("http://gdata.youtube.com/feeds/api/playlists/%s?v=2", playlistId);
            final List<URI> uriList = getVideoURIList(action);
            queueLinks(uriList);
        }
    }

    private boolean isCourseList() {
        return fileURL.contains("/course?list=");
    }

    private String getCourseIdFromUrl() throws PluginImplementationException {
        final Matcher matcher = PlugUtils.matcher("list=(?:EC)?([^\\?&#/]+)", fileURL);
        if (!matcher.find()) {
            throw new PluginImplementationException("Error getting course id");
        }
        return matcher.group(1);
    }

    //Course list contains video playlist, lecture materials, and course materials
    //reference : https://developers.google.com/youtube/2.0/developers_guide_protocol#Courses
    //reference : https://developers.google.com/youtube/2.0/developers_guide_protocol#Lectures
    private void parseCourseList() throws Exception {
        //Step #1 queue video playlist related to the course
        final String courseId = getCourseIdFromUrl();
        String action = String.format("http://gdata.youtube.com/feeds/api/playlists/%s?v=2", courseId);
        List<URI> uriList = getVideoURIList(action);
        if (uriList.isEmpty()) {
            logger.info("No video links found");
        } else {
            getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, uriList);
            logger.info(uriList.size() + " videos added");
        }

        //Step #2 queue lecture materials
        action = String.format("https://stage.gdata.youtube.com/feeds/api/edu/lectures?course=%s", courseId);
        uriList = getLectureCourseMaterialURIList(action);
        if (uriList.isEmpty()) {
            logger.info("No lecture material links found");
        } else {
            getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, uriList);
            logger.info(uriList.size() + " lecture materials added");
        }

        //Step #3 queue course materials
        action = String.format("http://gdata.youtube.com/feeds/api/edu/courses/%s?v=2", courseId);
        uriList = getLectureCourseMaterialURIList(action);
        if (uriList.isEmpty()) {
            logger.info("No course material links found");
        } else {
            getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, uriList);
            logger.info(uriList.size() + " course materials added");
        }
        httpFile.getProperties().put("removeCompleted", true);
    }

    private boolean isSubtitles() {
        return fileURL.contains("#subtitles:");
    }

    private boolean checkSubtitles() throws Exception {
        Matcher matcher = PlugUtils.matcher("#subtitles:(.*?):(.+)", fileURL);
        if (matcher.find()) {
            runCheck();
            final String lang = matcher.group(1);
            String fileExtension;
            if (!lang.isEmpty()) {
                fileExtension = "." + lang + ".srt";
            } else {
                fileExtension = ".srt";
            }
            httpFile.setFileName(httpFile.getFileName() + fileExtension);
            httpFile.setFileName(HttpUtils.replaceInvalidCharsForFileSystem(PlugUtils.unescapeHtml(httpFile.getFileName()), "_"));

            final String url = "http://www.youtube.com/api/timedtext?type=track" + matcher.group(2);
            final HttpMethod method = getGetMethod(url);
            if (!makeRedirectedRequest(method)) {
                throw new ServiceConnectionProblemException();
            }
            try {
                final byte[] converted = Transcription2SrtUtil.convert(getContentAsString()).getBytes("UTF-8");
                httpFile.setFileSize(converted.length);
                downloadTask.saveToFile(new ByteArrayInputStream(converted));
            } catch (final Exception e) {
                LogUtils.processException(logger, e);
                throw new PluginImplementationException("Error converting and saving subtitles", e);
            }
            return true;
        } else if (config.isDownloadSubtitles() && isVideo()) {
            final String id = getIdFromUrl();
            final HttpMethod method = getGetMethod("http://www.youtube.com/api/timedtext?type=list&v=" + id);
            if (makeRedirectedRequest(method)) {
                final List<URI> list = new LinkedList<URI>();
                matcher = getMatcherAgainstContent("<track id=\"\\d*\" name=\"(.*?)\" lang_code=\"(.*?)\"");
                while (matcher.find()) {
                    final String name = matcher.group(1);
                    final String lang = matcher.group(2);
                    final String url = fileURL + "#subtitles:" + lang + ":&v=" + id + "&name=" + name + "&lang=" + lang;
                    try {
                        list.add(new URI(url));
                    } catch (final URISyntaxException e) {
                        LogUtils.processException(logger, e);
                    }
                }
                getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, list);
            }
        }
        return false;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void multiplexDash(final boolean isAudio) throws IOException {
        if (downloadTask.isTerminated()) {
            logger.info("Download task was terminated");
            return;
        }
        logger.info("Multiplexing DASH streams");
        final HttpFile downloadFile = downloadTask.getDownloadFile();
        final File inputFile = downloadFile.getStoreFile();
        if (!inputFile.exists()) {
            logger.warning("Input file not found, multiplexing aborted");
            return;
        }

        File videoFile;
        File audioFile;
        String fnameNoExt = downloadFile.getFileName().replaceFirst("\\.[^\\.]{3,4}$", "");
        String fnameOutput = fnameNoExt + Container.mp4.getFileExt();
        if (isAudio) {
            videoFile = new File(downloadFile.getSaveToDirectory(), fnameNoExt + Container.dash_v.getFileExt());
            audioFile = inputFile;
        } else {
            videoFile = inputFile;
            audioFile = new File(downloadFile.getSaveToDirectory(), fnameNoExt + Container.dash_a.getFileExt());
        }
        if (!videoFile.exists()) {
            logger.info("DASH video file not found");
            return;
        }
        if (!audioFile.exists()) {
            logger.info("DASH audio file not found");
            return;
        }

        logger.info("DASH video file size: " + videoFile.length());
        logger.info("DASH audio file size: " + audioFile.length());

        FileOutputStream fos = null;
        FileDataSourceImpl videoFds = null;
        FileDataSourceImpl audioFds = null;
        File outputFile = new File(downloadFile.getSaveToDirectory(), fnameOutput);
        int outputFileCounter = 2;
        boolean finished = false;
        try {
            while (outputFile.exists()) {
                fnameOutput = fnameNoExt + "-" + outputFileCounter++ + Container.mp4.getFileExt();
                outputFile = new File(downloadFile.getSaveToDirectory(), fnameOutput);
            }
            fos = new FileOutputStream(outputFile);
            logger.info("Output file name: " + fnameOutput);
            downloadFile.setState(DownloadState.COMPLETED);
            videoFds = new FileDataSourceImpl(videoFile);
            audioFds = new FileDataSourceImpl(audioFile);
            Movie videoMovie = MovieCreator.build(videoFds);
            Movie audioMovie = MovieCreator.build(audioFds);
            Track audioTrack = audioMovie.getTracks().get(0);
            audioTrack.getTrackMetaData().setLanguage("eng");
            videoMovie.addTrack(audioTrack);
            com.coremedia.iso.boxes.Container out = new DefaultMp4Builder().build(videoMovie);
            out.writeContainer(fos.getChannel());
            logger.info("Output file size: " + fos.getChannel().position());
            finished = true;
        } finally {
            closeSilently(videoFds);
            closeSilently(audioFds);
            closeSilently(fos);
        }
        if (finished) {
            logger.info("Deleting DASH files");
            audioFile.delete();
            videoFile.delete();
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void extractAudio(final Container container) throws IOException {
        if (downloadTask.isTerminated()) {
            logger.info("Download task was terminated");
            return;
        }
        logger.info("Extracting audio");
        final HttpFile downloadFile = downloadTask.getDownloadFile();
        final File inputFile = downloadFile.getStoreFile();
        if (!inputFile.exists()) {
            logger.warning("Input file not found, extraction aborted");
            return;
        }

        String tempSuffix = ".extract.temp";
        String fnameNoExt = downloadFile.getFileName().replaceFirst("\\.[^\\.]{3,4}$", "");
        String fnameOutput = fnameNoExt + Container.dash_a.getFileExt(); //.m4a
        String fnameTempOutput = fnameOutput + tempSuffix;
        FileOutputStream fos = null;
        File outputFile = new File(downloadFile.getSaveToDirectory(), fnameOutput);
        File tempOutputFile = new File(downloadFile.getSaveToDirectory(), fnameTempOutput);
        FileDataSourceImpl inputFds = null;
        int outputFileCounter = 2;
        boolean finished = false;
        try {
            while (outputFile.exists() || tempOutputFile.exists()) {
                fnameOutput = fnameNoExt + "-" + outputFileCounter++ + Container.dash_a.getFileExt();
                fnameTempOutput = fnameOutput + tempSuffix;
                outputFile = new File(downloadFile.getSaveToDirectory(), fnameOutput);
                tempOutputFile = new File(downloadFile.getSaveToDirectory(), fnameTempOutput);
            }
            fos = new FileOutputStream(tempOutputFile);
            logger.info("Temp output file name: " + fnameTempOutput);
            logger.info("Output file name: " + fnameOutput);
            downloadFile.setState(DownloadState.COMPLETED);

            Movie movie;
            inputFds = new FileDataSourceImpl(inputFile);
            if (container == Container.flv) {
                FlvAacTrackImpl flvAACTrack = new FlvAacTrackImpl(inputFds);
                movie = new Movie();
                movie.addTrack(flvAACTrack);
            } else { //mp4 or m4a
                movie = MovieCreator.build(inputFds);
            }
            com.coremedia.iso.boxes.Container out = new ItunesBuilder().build(movie);
            out.writeContainer(fos.getChannel());
            logger.info("Output file size: " + fos.getChannel().position());
            finished = true;
        } finally {
            closeSilently(inputFds);
            closeSilently(fos);
        }
        if (finished) {
            logger.info("Renaming temp file to output file");
            tempOutputFile.renameTo(outputFile);
            logger.info("Deleting input file");
            inputFile.delete();
        }
    }

    private void closeSilently(OutputStream os) {
        if (os != null) {
            try {
                os.close();
            } catch (IOException e) {
                LogUtils.processException(logger, e);
            }
        }
    }

    private void closeSilently(DataSource ds) {
        if (ds != null) {
            try {
                ds.close();
            } catch (IOException e) {
                LogUtils.processException(logger, e);
            }
        }
    }


    private void bypassAgeVerification(HttpMethod method) throws Exception {
        if (method.getURI().toString().matches("https?://(www\\.)?youtube\\.com/verify_age.*")
                || getContentAsString().contains("watch7-player-age-gate-content")
                || getContentAsString().contains("Sign in to confirm your age")
                || getContentAsString().contains("<script>window.location = \"https:\\/\\/www.youtube.com\\/verify_age")
                || getContentAsString().contains("<script>window.location = \"http:\\/\\/www.youtube.com\\/verify_age")) {
            setClientParameter(DownloadClientConsts.USER_AGENT, "Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)");
            method = getGetMethod(fileURL);
            if (!makeRedirectedRequest(method)) {
                throw new ServiceConnectionProblemException();
            }

            //controversy
            if (getContentAsString().contains("<script>window.location = \"http:\\/\\/www.youtube.com\\/verify_controversy")
                    || getContentAsString().contains("<script>window.location = \"https:\\/\\/www.youtube.com\\/verify_controversy")) {
                method = getMethodBuilder()
                        .setAction(PlugUtils.getStringBetween(getContentAsString(), "window.location = \"", "\"").replace("\\/", "/"))
                        .toGetMethod();
                if (!makeRedirectedRequest(method)) {
                    throw new ServiceConnectionProblemException();
                }
            }
            if (method.getURI().toString().matches("https?://(www\\.)?youtube\\.com/verify_controversy.*")
                    || getContentAsString().contains("verify_controversy?action_confirm=1")) {
                method = getMethodBuilder()
                        .setBaseURL("https://www.youtube.com")
                        .setActionFromFormWhereActionContains("verify_controversy", true)
                        .toPostMethod();
                if (!makeRedirectedRequest(method)) {
                    throw new ServiceConnectionProblemException();
                }
            }
            setClientParameter(DownloadClientConsts.USER_AGENT, USER_AGENT);
        } else if (getContentAsString().contains("I confirm that I am 18 years of age or older")) {
            if (!makeRedirectedRequest(getGetMethod(fileURL + "&has_verified=1"))) {
                throw new ServiceConnectionProblemException();
            }
        }
        if (getContentAsString().contains("Sign in to view this video")
                || getContentAsString().contains("Sign in to confirm your age")) {  //just in case they change age verification mechanism
            throw new PluginImplementationException("Age verification is broken");
        }
    }
}
