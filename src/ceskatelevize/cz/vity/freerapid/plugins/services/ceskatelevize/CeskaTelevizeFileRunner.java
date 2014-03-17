package cz.vity.freerapid.plugins.services.ceskatelevize;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.services.rtmp.AbstractRtmpRunner;
import cz.vity.freerapid.plugins.services.rtmp.RtmpSession;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.net.URLDecoder;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeSet;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class which contains main code
 *
 * @author JPEXS
 */
class CeskaTelevizeFileRunner extends AbstractRtmpRunner {
    private final static Logger logger = Logger.getLogger(CeskaTelevizeFileRunner.class.getName());
    private CeskaTelevizeSettingsConfig config;

    private void setConfig() throws Exception {
        CeskaTelevizeServiceImpl service = (CeskaTelevizeServiceImpl) getPluginService();
        config = service.getConfig();
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        setConfig();
        final GetMethod method = getGetMethod(fileURL);
        String videoSrc;
        String base;
        if (makeRedirectedRequest(method)) {
            if (!getContentAsString().contains("callSOAP(")) {
                HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL).setActionFromIFrameSrcWhereTagContains("iFramePlayer").toGetMethod();
                if (!makeRedirectedRequest(httpMethod)) {
                    checkProblems();
                    throw new ServiceConnectionProblemException();
                }
                checkProblems();
            }
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
            final MethodBuilder mb = getMethodBuilder().setReferer(fileURL).setAction("http://www.ceskatelevize.cz/ajax/playlistURL.php");
            for (final String key : params.keySet()) {
                mb.setParameter(key, params.get(key));
            }
            final HttpMethod getPlayListMethod = mb.toPostMethod();
            getPlayListMethod.setRequestHeader("X-Requested-With", "X-Requested-With");
            getPlayListMethod.setRequestHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            getPlayListMethod.setRequestHeader("x-addr", "127.0.0.1");
            if (makeRequest(getPlayListMethod)) {
                if (!getContentAsString().startsWith("http")) {
                    throw new PluginImplementationException("Server returned invalid playlist URL");
                }
                final String playlistURL = getContentAsString();
                final HttpMethod playlistMethod = new GetMethod(URLDecoder.decode(playlistURL, "UTF-8"));
                if (!makeRedirectedRequest(playlistMethod)) {
                    throw new PluginImplementationException("Cannot connect to playlist");
                }
                final Matcher switchMatcher = Pattern.compile("<switchItem id=\"([^\"]+)\" base=\"([^\"]+)\" begin=\"([^\"]+)\" duration=\"([^\"]+)\" clipBegin=\"([^\"]+)\".*?>\\s*(<video[^>]*>\\s*)*</switchItem>", Pattern.MULTILINE + Pattern.DOTALL).matcher(getContentAsString());

                TreeSet<SwitchItem> body = new TreeSet<SwitchItem>();
                while (switchMatcher.find()) {
                    SwitchItem newItem = new SwitchItem();
                    String swItemText = switchMatcher.group(0);
                    newItem.base = PlugUtils.replaceEntities(switchMatcher.group(2));
                    newItem.duration = Double.parseDouble(switchMatcher.group(4));
                    Matcher videoMatcher = Pattern.compile("<video src=\"([^\"]+)\" system-bitrate=\"([0-9]+)\" label=\"([0-9]+)p\" enabled=\"true\" */>").matcher(swItemText);
                    while (videoMatcher.find()) {
                        newItem.videos.add(new Video(videoMatcher.group(1), videoMatcher.group(3)));
                    }
                    body.add(newItem);
                }
                if (body.isEmpty()) {
                    throw new PluginImplementationException("No stream found.");
                }
                SwitchItem selectedSwitch = body.first();
                base = selectedSwitch.base;

                int preferredQualityInt = config.getQualitySetting();

                videoSrc = null;
                String nearestHigherSrc = null;
                String nearestLowerSrc = null;
                int nearestHigher = 0;
                int nearestLower = 0;
                int highestQuality = 0;
                String highestQualitySrc = null;
                int lowestQuality = 0;
                String lowestQualitySrc = null;
                for (Video video : selectedSwitch.videos) {
                    int qualInt = Integer.parseInt(video.label);
                    if (qualInt > highestQuality) {
                        highestQuality = qualInt;
                        highestQualitySrc = video.src;
                    }
                    if ((lowestQuality == 0) || (qualInt < lowestQuality)) {
                        lowestQuality = qualInt;
                        lowestQualitySrc = video.src;
                    }
                    if (preferredQualityInt > 0) {
                        if (qualInt > preferredQualityInt) {
                            if ((nearestHigher == 0) || (nearestHigher > qualInt)) {
                                nearestHigher = qualInt;
                                nearestHigherSrc = video.src;
                            }
                        }
                        if (qualInt < preferredQualityInt) {
                            if ((nearestLower == 0) || (nearestLower < qualInt)) {
                                nearestLower = qualInt;
                                nearestLowerSrc = video.src;
                            }
                        }
                    }
                    if (qualInt == preferredQualityInt) {
                        videoSrc = video.src;
                        break;
                    }
                }
                if (preferredQualityInt == -1) {
                    videoSrc = lowestQualitySrc;
                } else if (preferredQualityInt == -2) {
                    videoSrc = highestQualitySrc;
                } else if (videoSrc == null) {
                    if (nearestLower != 0) {
                        videoSrc = nearestLowerSrc;
                    } else if (nearestHigher != 0) {
                        videoSrc = nearestHigherSrc;
                    }
                }
                if (videoSrc == null) {
                    throw new PluginImplementationException("Cannot select preferred quality");
                }
                Matcher filenameMatcher = Pattern.compile("/([^/]+)\\....$").matcher(videoSrc);
                if (filenameMatcher.find()) {
                    httpFile.setFileName(filenameMatcher.group(1) + ".flv");
                }
            } else {
                throw new PluginImplementationException("Cannot load playlist URL");
            }
        } else {
            throw new ServiceConnectionProblemException();
        }

        RtmpSession rtmpSession = new RtmpSession(base, videoSrc);
        rtmpSession.disablePauseWorkaround();
        tryDownloadAndSaveFile(rtmpSession);
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("Neexistuj")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
        if (contentAsString.contains("content is not available at")) {
            throw new PluginImplementationException("This content is not available at your territory due to limited copyright");
        }
    }

}