package cz.vity.freerapid.plugins.services.ceskatelevize;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.services.rtmp.AbstractRtmpRunner;
import cz.vity.freerapid.plugins.services.rtmp.RtmpSession;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.net.URLDecoder;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class which contains main code
 *
 * @author JPEXS
 * @author tong2shot
 */
class CeskaTelevizeFileRunner extends AbstractRtmpRunner {
    private final static Logger logger = Logger.getLogger(CeskaTelevizeFileRunner.class.getName());
    private CeskaTelevizeSettingsConfig config;

    private void setConfig() throws Exception {
        CeskaTelevizeServiceImpl service = (CeskaTelevizeServiceImpl) getPluginService();
        config = service.getConfig();
    }

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        setPageEncoding("Windows-1250");
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkName();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkName() throws Exception {
        HttpMethod httpMethod;
        if (!getContentAsString().contains("callSOAP(")) {
            httpMethod = getMethodBuilder().setReferer(fileURL).setActionFromIFrameSrcWhereTagContains("iFramePlayer").toGetMethod();
            if (!makeRedirectedRequest(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            checkProblems();
        }
        if (!getContentAsString().contains("callSOAP(")) {
            httpMethod = getMethodBuilder().setReferer(fileURL).setActionFromAHrefWhereATagContains("Přehrát video").toGetMethod();
            if (!makeRedirectedRequest(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
        }

        Matcher matcher;
        String filename;
        String nazev;
        try {
            nazev = PlugUtils.unescapeUnicode(PlugUtils.getStringBetween(getContentAsString(), "\"nazev\":\"", "\"").trim());
        } catch (PluginImplementationException e) {
            throw new PluginImplementationException("Program title not found");
        }
        filename = nazev;

        String nazevCasti = "";
        matcher = getMatcherAgainstContent("\"nazevCasti\":(?:null|\"(.*?)\")");
        if (matcher.find()) {
            try {
                nazevCasti = PlugUtils.unescapeUnicode(matcher.group(1).trim());
            } catch (Exception e) {
                //
            }
        }

        String nazevCastiProgram = "";
        matcher = getMatcherAgainstContent("\"nazevCastiProgram\":(?:null|\"(.*?)\")");
        if (matcher.find()) {
            try {
                nazevCastiProgram = PlugUtils.unescapeUnicode(matcher.group(1).trim());
            } catch (Exception e) {
                //
            }
        }

        //they don't provide a consistent way to get program and episode name, so we have to do this weird thing
        if (!nazevCastiProgram.isEmpty() && !nazevCasti.isEmpty() && nazevCastiProgram.contains(nazevCasti)) {
            filename += " - " + nazevCastiProgram;
        } else {
            if (!nazevCasti.isEmpty() && !filename.contains(nazevCasti)) {
                filename += " - " + nazevCasti;
            }
            if (!nazevCastiProgram.isEmpty() && !filename.contains(nazevCastiProgram)) {
                filename += " - " + nazevCastiProgram;
            }
        }

        String title;
        matcher = getMatcherAgainstContent("\"Type\":\"Archive\".+?\"Title\":\"(.*?)\"");
        if (matcher.find()) {
            title = PlugUtils.unescapeUnicode(matcher.group(1).trim());
            if (!title.isEmpty() && !filename.contains(title)) {
                filename += " - " + title;
            }
        }

        //the only way to be sure.. for now...
        if (filename.equals(nazev)) {
            String fDodatek;
            matcher = getMatcherAgainstContent("\"fDodatek\":(?:null|\"(.*?)\")");
            if (matcher.find()) {
                try {
                    fDodatek = PlugUtils.unescapeHtml(PlugUtils.unescapeUnicode(matcher.group(1).trim()));
                    if (!fDodatek.isEmpty() && !filename.contains(fDodatek)) {
                        filename += " - " + fDodatek;
                    }
                } catch (Exception e) {
                    //
                }
            }
        }

        filename += ".flv";
        httpFile.setFileName(filename);
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        setPageEncoding("Windows-1250");
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkName();

            final String callSoapParams = PlugUtils.getStringBetween(getContentAsString(), "callSOAP(", ");");
            final ScriptEngineManager factory = new ScriptEngineManager();
            final ScriptEngine engine = factory.getEngineByName("JavaScript");
            final Map<String, String> params = new LinkedHashMap<String, String>(); //preserve ordering
            engine.put("params", params);
            try {
                engine.eval("function isArray(a){return Object.prototype.toString.apply(a) === '[object Array]';}; "
                        + "function walkObject(a,path){"
                        + "if(a==null) {"
                        + "walk('null',path);"
                        + "}"
                        + "for(var key in a){"
                        + "if(path==''){walk(a[key],key);}"
                        + " else {walk(a[key],path+'['+key+']');};"
                        + "}"
                        + "};"
                        + "function walk(a,path){"
                        + " if(isArray(a)) {walkArray(a,path);}"
                        + " else if(typeof a=='object'){ walkObject(a,path);}"
                        + " else params.put(path,''+a);"
                        + "}"
                        + "function walkArray(a,path){"
                        + "for(var i=0;i<a.length;i++){"
                        + " walk(a[i],path+'['+i+']');"
                        + "}"
                        + "}"
                        + "function callSOAP(obj){"
                        + "walkObject(obj,'');"
                        + "};"
                        + "callSOAP(" + callSoapParams + ");");
            } catch (Exception ex) {
                throw new PluginImplementationException("Cannot get Playlist");
            }
            final MethodBuilder mb = getMethodBuilder().setReferer(fileURL).setAction(getPlaylistUrl());
            for (final String key : params.keySet()) {
                mb.setParameter(key, params.get(key));
            }
            final HttpMethod getPlayListMethod = mb.toPostMethod();
            getPlayListMethod.setRequestHeader("X-Requested-With", "XMLHttpRequest");
            getPlayListMethod.setRequestHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            getPlayListMethod.setRequestHeader("x-addr", "127.0.0.1");
            SwitchItem selectedSwitchItem;
            Video selectedVideo;
            setConfig();
            if (makeRequest(getPlayListMethod)) {
                final Matcher matcher = getMatcherAgainstContent("(http://[^<>]+)");
                if (!matcher.find()) {
                    throw new PluginImplementationException("Playlist URL not found");
                }
                final String playlistUrl = URLDecoder.decode(matcher.group(1), "UTF-8").replace("hashedId", "id");
                final HttpMethod playlistMethod = new GetMethod(playlistUrl);
                if (!makeRedirectedRequest(playlistMethod)) {
                    throw new PluginImplementationException("Cannot connect to playlist");
                }
                selectedSwitchItem = getSelectedSwitchItem(getContentAsString());
                selectedVideo = getSelectedVideo(selectedSwitchItem);
            } else {
                checkProblems();
                throw new PluginImplementationException("Cannot load playlist URL");
            }
            RtmpSession rtmpSession = new RtmpSession(selectedSwitchItem.getBase(), selectedVideo.getSrc());
            rtmpSession.disablePauseWorkaround();
            tryDownloadAndSaveFile(rtmpSession);
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("Neexistuj") || contentAsString.contains("Stránka nenalezena")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
        if (contentAsString.contains("content is not available at")) {
            throw new PluginImplementationException("This content is not available at your territory due to limited copyright");
        }
    }

    private String getPlaylistUrl() throws Exception {
        final HttpMethod method = getGetMethod("http://img.ceskatelevize.cz/libraries/player/ajaxPlaylist.js?ver=1.1");
        if (!makeRedirectedRequest(method)) {
            throw new ServiceConnectionProblemException();
        }
        return PlugUtils.getStringBetween(getContentAsString(), "url: \"", "\"");
    }

    private SwitchItem getSelectedSwitchItem(String playlistContent) throws PluginImplementationException {
        final Matcher switchMatcher = Pattern.compile("<switchItem id=\"([^\"]+)\" base=\"([^\"]+)\" begin=\"([^\"]+)\" duration=\"([^\"]+)\" clipBegin=\"([^\"]+)\".*?>\\s*(<video[^>]*>\\s*)*</switchItem>", Pattern.MULTILINE + Pattern.DOTALL).matcher(playlistContent);
        logger.info("Available switch items : ");
        List<SwitchItem> switchItems = new ArrayList<SwitchItem>();
        while (switchMatcher.find()) {
            String swItemText = switchMatcher.group(0);
            String base = PlugUtils.replaceEntities(switchMatcher.group(2));
            double duration = Double.parseDouble(switchMatcher.group(4));
            SwitchItem newItem = new SwitchItem(base, duration);
            Matcher videoMatcher = Pattern.compile("<video src=\"([^\"]+)\" system-bitrate=\"([0-9]+)\" label=\"([0-9]+)p\" enabled=\"true\" */>").matcher(swItemText);
            while (videoMatcher.find()) {
                newItem.addVideo(new Video(videoMatcher.group(1), Integer.parseInt(videoMatcher.group(3))));
            }
            switchItems.add(newItem);
            logger.info(newItem.toString());
        }
        if (switchItems.isEmpty()) {
            throw new PluginImplementationException("No stream found.");
        }
        SwitchItem selectedSwitchItem = Collections.max(switchItems); //switch item with the longest duration
        logger.info("Selected switch item : " + selectedSwitchItem);
        return selectedSwitchItem;
    }

    private Video getSelectedVideo(SwitchItem switchItem) throws PluginImplementationException {
        Video selectedVideo = null;
        logger.info("Config settings : " + config);
        if (config.getVideoQuality() == VideoQuality.Highest) {
            selectedVideo = Collections.max(switchItem.getVideos());
        } else if (config.getVideoQuality() == VideoQuality.Lowest) {
            selectedVideo = Collections.min(switchItem.getVideos());
        } else {
            final int LOWER_QUALITY_PENALTY = 10;
            int weight = Integer.MAX_VALUE;
            for (Video video : switchItem.getVideos()) {
                int deltaQ = video.getVideoQuality() - config.getVideoQuality().getQuality();
                int tempWeight = (deltaQ < 0 ? Math.abs(deltaQ) + LOWER_QUALITY_PENALTY : deltaQ);
                if (tempWeight < weight) {
                    weight = tempWeight;
                    selectedVideo = video;
                }
            }
        }
        if (selectedVideo == null) {
            throw new PluginImplementationException("Cannot select video");
        }
        logger.info("Video to be downloaded : " + selectedVideo);
        return selectedVideo;
    }
}