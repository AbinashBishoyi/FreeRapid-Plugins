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

import java.util.ArrayList;
import java.util.HashMap;
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
        final GetMethod getMethod = getGetMethod(getVideoInfoUrl(videoId));
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            HashMap videoEntry = getVideoEntry(getContentAsString(), videoId);
            checkNameAndSize(videoEntry);
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize(HashMap videoEntry) throws ErrorDuringDownloadingException {
        String title;
        String episode;
        try {
            HashMap titrage = (HashMap) ((HashMap) videoEntry.get("INFOS")).get("TITRAGE");
            title = (String) titrage.get("TITRE");
            episode = (String) titrage.get("SOUS_TITRE");
        } catch (Exception e) {
            throw new PluginImplementationException("Error getting file name");
        }
        String fname = (episode.isEmpty() ? title : title + " - " + episode) + ".flv";
        logger.info("File name: " + fname);
        httpFile.setFileName(fname);
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        String videoId = getVideoId(fileURL);
        final GetMethod getMethod = getGetMethod(getVideoInfoUrl(videoId));
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            HashMap videoEntry = getVideoEntry(getContentAsString(), videoId);
            checkNameAndSize(videoEntry);

            String manifestUrl;
            try {
                manifestUrl = (String) ((HashMap) ((HashMap) videoEntry.get("MEDIA")).get("VIDEOS")).get("HDS");
            } catch (Exception e) {
                throw new PluginImplementationException("HDS URL not found");
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

    private String getVideoInfoUrl(String videoId) throws PluginImplementationException {
        return "http://service.canal-plus.com/video/rest/getVideosLiees/cplus/" + videoId + "?format=json";
    }

    private HashMap getVideoEntry(String content, String videoId) throws PluginImplementationException {
        JsonMapper jsonMapper = new JsonMapper();
        ArrayList<HashMap> deserialized;
        HashMap selectedEntry = null;
        try {
            deserialized = jsonMapper.deserialize(content, ArrayList.class);
            for (HashMap entry : deserialized) {
                if (entry.get("ID").equals(videoId)) {
                    selectedEntry = entry;
                    break;
                }
            }
        } catch (Exception e) {
            throw new PluginImplementationException("Error getting video entry");
        }
        if (selectedEntry == null) {
            throw new PluginImplementationException("Unable to select video entry");
        }
        return selectedEntry;
    }

}
