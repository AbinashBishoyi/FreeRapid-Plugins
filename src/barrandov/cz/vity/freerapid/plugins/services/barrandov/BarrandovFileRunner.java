package cz.vity.freerapid.plugins.services.barrandov;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.services.rtmp.AbstractRtmpRunner;
import cz.vity.freerapid.plugins.services.rtmp.RtmpSession;
import cz.vity.freerapid.plugins.services.rtmp.SwfVerificationHelper;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.Random;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class which contains main code
 *
 * @author JPEXS
 */
class BarrandovFileRunner extends AbstractRtmpRunner {

    private final static Logger logger = Logger.getLogger(BarrandovFileRunner.class.getName());
    private BarrandovSettingsConfig config;
    private final static String SWF_URL = "http://www.barrandov.tv/flash/unigramPlayer_v1.swf";
    private final static SwfVerificationHelper helper = new SwfVerificationHelper(SWF_URL);

    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        checkNameAndSize();
    }
    private String streamName;
    private String server;
    private int videoId;
    private String appName;

    private void setConfig() throws Exception {
        BarrandovServiceImpl service = (BarrandovServiceImpl) getPluginService();
        config = service.getConfig();
    }

    private void checkNameAndSize() throws Exception {
        setConfig();
        Matcher m = Pattern.compile(".*barrandov\\.tv/([0-9]+)\\-.*").matcher(fileURL);
        if (!m.matches()) {
            throw new PluginImplementationException("Bad link format");
        }

        videoId = Integer.parseInt(m.group(1));
        Random r = new Random();
        final GetMethod playerDataMethod = getGetMethod("http://www.barrandov.tv/special/videoplayerdata/" + videoId + "?r=" + r.nextInt(65536));
        if (!makeRedirectedRequest(playerDataMethod)) {
            throw new PluginImplementationException("Cannot load video data");
        }

        String hostName = PlugUtils.getStringBetween(getContentAsString(), "<hostname>", "</hostname>");
        streamName = PlugUtils.getStringBetween(getContentAsString(), "<streamname>", "</streamname>");
        if (streamName.contains("qual")) {
            streamName = streamName.replace("qual", "500");
        }
        String hasquality = PlugUtils.getStringBetween(getContentAsString(), "<hasquality>", "</hasquality>");
        String hashdquality = PlugUtils.getStringBetween(getContentAsString(), "<hashdquality>", "</hashdquality>");

        int quality = config.getQualitySetting();
        if (hashdquality.equals("true") && (quality == 2)) {
            streamName = streamName.replace("500", "HD");
            logger.info("Selected HD quality");
        } else if (hasquality.equals("true") && (quality >= 1)) {
            streamName = streamName.replace("500", "1000");
            logger.info("Selected medium quality");
        } else {
            logger.info("Selected low quality");
        }

        server = hostName.substring(0, hostName.indexOf("/"));
        appName = hostName.substring(hostName.indexOf("/") + 1);
        String parts[] = streamName.split(":");
        String name = "";
        if (parts.length > 1) {
            name = parts[1];
        } else {
            name = parts[0];
        }
        String ext = name.substring(name.lastIndexOf(".") + 1);
        if (!ext.equals("flv")) {
            name = name.substring(0, name.length() - ext.length()) + "flv";
        }
        httpFile.setFileName(name);
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        checkNameAndSize();
        logger.info("appName:"+appName+" streamName:"+streamName+" server:"+server);

        GetMethod gm=new GetMethod(fileURL);
        if(!makeRedirectedRequest(gm))
        {
            throw new PluginImplementationException("Cannot connect to url");
        }
        RtmpSession rtmpSession = new RtmpSession(server, 1935, appName, streamName,true);
        rtmpSession.getConnectParams().put("pageUrl", fileURL);
        helper.setSwfVerification(rtmpSession, client);
        rtmpSession.setSecureToken("#ed%h0#w@1");
        if (!tryDownloadAndSaveFile(rtmpSession)) {
            checkProblems();//if downloading failed
            logger.warning(getContentAsString());//log the info
            throw new PluginImplementationException();//some unknown problem
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
    }
}
