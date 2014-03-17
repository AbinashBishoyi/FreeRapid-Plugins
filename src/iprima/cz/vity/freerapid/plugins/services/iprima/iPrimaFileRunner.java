package cz.vity.freerapid.plugins.services.iprima;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.services.rtmp.AbstractRtmpRunner;
import cz.vity.freerapid.plugins.services.rtmp.RtmpSession;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;

import java.util.Random;
import java.util.logging.Logger;

/**
 * Class which contains main code
 *
 * @author JPEXS
 * @author ntoskrnl
 */
class iPrimaFileRunner extends AbstractRtmpRunner {
    private final static Logger logger = Logger.getLogger(iPrimaFileRunner.class.getName());
    private iPrimaSettingsConfig config;

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void setConfig() throws Exception {
        final iPrimaServiceImpl service = (iPrimaServiceImpl) getPluginService();
        config = service.getConfig();
    }

    private void checkNameAndSize() throws Exception {
        final String name = PlugUtils.getStringBetween(getContentAsString(), "<meta property=\"og:title\" content=\"", "\"");
        httpFile.setFileName(name + ".flv");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        setConfig();
        HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize();
            if (getContentAsString().contains("http://flash.stream.cz/")) {
                final String id = PlugUtils.getStringBetween(getContentAsString(), "&cdnID=", "&");
                method = getGetMethod("http://cdn-dispatcher.stream.cz/?id=" + id);
                if (!tryDownloadAndSaveFile(method)) {
                    checkProblems();
                    throw new ServiceConnectionProblemException("Error starting download");
                }
            } else {
                final String playName = getPlayName();
                final String geoZone = PlugUtils.getStringBetween(getContentAsString(), "\"zoneGEO\":", ",");
                final Random rnd = new Random();
                method = getMethodBuilder()
                        .setReferer(fileURL)
                        .setAction("http://embed.livebox.cz/iprimaplay/player-embed-v2.js")
                        .setParameter("__tok" + rnd.nextInt(0x40000000) + "__", String.valueOf(rnd.nextInt(0x40000000)))
                        .toGetMethod();
                if (!makeRedirectedRequest(method)) {
                    checkProblems();
                    throw new ServiceConnectionProblemException();
                }
                String app = "iprima_token";
                if (!"0".equals(geoZone)) {
                    app += "_" + geoZone;
                }
                final String auth = PlugUtils.getStringBetween(getContentAsString(), "'?auth='+\"\"+'", "';", 2);
                app += "?auth=" + auth;
                final RtmpSession rtmpSession = new RtmpSession("bcastmw.livebox.cz", 80, app, playName);
                rtmpSession.getConnectParams().put("pageUrl", fileURL);
                rtmpSession.getConnectParams().put("swfUrl", "http://embed.livebox.cz/iprimaplay/flash/LiveboxPlayer.swf?nocache=" + System.currentTimeMillis());
                rtmpSession.disablePauseWorkaround();
                tryDownloadAndSaveFile(rtmpSession);
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private String getPlayName() throws ErrorDuringDownloadingException {
        String playName;
        switch (config.getVideoQuality()) {
            case HD:
                try {
                    playName = PlugUtils.getStringBetween(getContentAsString(), "\"hd_id\":\"", "\"");
                    break;
                } catch (final PluginImplementationException e) {
                    logger.info("HD quality not found, using High");
                }
                //fallthrough
            case High:
                playName = PlugUtils.getStringBetween(getContentAsString(), "\"hq_id\":\"", "\"");
                break;
            case Low:
                playName = PlugUtils.getStringBetween(getContentAsString(), "\"lq_id\":\"", "\"");
                break;
            default:
                throw new PluginImplementationException("Unknown video quality: " + config.getVideoQuality());
        }
        if (playName.contains(".mp4") && !playName.startsWith("mp4:")) {
            playName = "mp4:" + playName;
        }
        return playName;
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        if (getContentAsString().contains("Video bylo odstraněno")
                || getContentAsString().contains("Požadovaná stránka nebyla nalezena")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

}