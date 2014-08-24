package cz.vity.freerapid.plugins.services.canalplus;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.services.adobehds.AdjustableBitrateHdsDownloader;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.JsonMapper;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.methods.GetMethod;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author tong2shot
 * @since 0.9u4
 */
class CanalPlusFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(CanalPlusFileRunner.class.getName());
    private SettingsConfig config;


    private void setConfig() throws Exception {
        CanalPlusServiceImpl service = (CanalPlusServiceImpl) getPluginService();
        config = service.getConfig();
    }

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        String videoId = getVideoId(fileURL);
        final GetMethod getMethod = getGetMethod(getVideoListUrl(videoId));
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            JsonNode videoNode = getVideoNode(getContentAsString(), videoId);
            checkNameAndSize(videoNode);
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize(JsonNode videoNode) throws ErrorDuringDownloadingException {
        String title = videoNode.findPath("TITRE").getTextValue();
        String episode = videoNode.findPath("SOUS_TITRE").getTextValue();
        if (title == null) {
            throw new PluginImplementationException("Error getting video title");
        }
        String fname = ((episode == null || episode.isEmpty()) ? title : title + " - " + episode) + ".flv";
        logger.info("File name: " + fname);
        httpFile.setFileName(fname);
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        String videoId = getVideoId(fileURL);
        final GetMethod getMethod = getGetMethod(getVideoListUrl(videoId));
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            JsonNode videoNode = getVideoNode(getContentAsString(), videoId);
            checkNameAndSize(videoNode);

            String manifestUrl = videoNode.findPath("HDS").getTextValue();
            if (manifestUrl == null) {
                throw new PluginImplementationException("HDS Manifest URL not found");
            }
            manifestUrl += (!manifestUrl.contains("?") ? "?" : "&") + "hdcore=2.11.3&g=SLMDNWLGMBXS";
            setConfig();
            logger.info("Settings config: " + config);
            final AdjustableBitrateHdsDownloader downloader = new AdjustableBitrateHdsDownloader(client, httpFile, downloadTask, config.getVideoQuality().getBitrate());
            downloader.tryDownloadAndSaveFile(manifestUrl);
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("\"ID\":\"-1\"")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    private String getVideoId(String fileUrl) throws PluginImplementationException {
        Matcher matcher = PlugUtils.matcher("vid=(\\d+)", fileUrl);
        if (!matcher.find()) {
            throw new PluginImplementationException("Video ID not found");
        }
        return matcher.group(1);
    }

    private String getVideoListUrl(String videoId) {
        return "http://service.canal-plus.com/video/rest/getVideosLiees/cplus/" + videoId + "?format=json";
    }

    private JsonNode getVideoNode(String content, String videoId) throws PluginImplementationException {
        ObjectMapper om = new JsonMapper().getObjectMapper();
        JsonNode selectedNode = null;
        try {
            JsonNode rootNode = om.readTree(content);
            if (rootNode.isArray()) { //contains more than 1 items
                for (JsonNode videoNode : rootNode) {
                    if (videoNode.get("ID").getTextValue().equals(videoId)) {
                        selectedNode = videoNode;
                        break;
                    }
                }
            } else if (rootNode.get("ID").getTextValue().equals(videoId)) { //only 1
                selectedNode = rootNode;
            }
        } catch (Exception e) {
            throw new PluginImplementationException("Error getting video node");
        }
        if (selectedNode == null) {
            throw new PluginImplementationException("Unable to select video node");
        }
        return selectedNode;
    }

}
