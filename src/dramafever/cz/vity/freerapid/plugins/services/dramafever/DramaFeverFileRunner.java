package cz.vity.freerapid.plugins.services.dramafever;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.tor.TorProxyClient;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.JsonMapper;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.net.URLDecoder;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author tong2shot
 */
class DramaFeverFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(DramaFeverFileRunner.class.getName());
    private final static String DEFAULT_EXT = ".flv";
    private SettingsConfig config;


    private void setConfig() throws Exception {
        DramaFeverServiceImpl service = (DramaFeverServiceImpl) getPluginService();
        config = service.getConfig();
    }

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize(getContentAsString());
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        final String fileName;
        try {
            fileName = PlugUtils.getStringBetween(content, "\"og:title\" content=\"", "\"").trim() + DEFAULT_EXT;
        } catch (PluginImplementationException e) {
            throw new PluginImplementationException("File name not found");
        }
        logger.info("File name: " + fileName);
        httpFile.setFileName(fileName);
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize(getContentAsString());
            String seriesId = getSeriesId(fileURL);
            String episodeNumber = getEpisodeNumber(fileURL);

            method = getMethodBuilder()
                    .setReferer(fileURL)
                    .setAction("http://www.dramafever.com/amp/episode/feed.json")
                    .setParameter("guid", seriesId + "." + episodeNumber)
                    .toGetMethod();
            TorProxyClient torClient = TorProxyClient.forCountry("us", client, getPluginService().getPluginContext().getConfigurationStorageSupport());
            if (!torClient.makeRequest(method)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            checkProblems();

            setConfig();
            JsonMapper mapper = new JsonMapper();
            Map deserialized;
            Map mediaGroup;
            try {
                deserialized = mapper.deserialize(getContentAsString(), Map.class);
                mediaGroup = (Map) ((Map) ((Map) deserialized.get("channel")).get("item")).get("media-group");
            } catch (Exception e) {
                throw new PluginImplementationException("Error getting media group");
            }

            String manifestUrl;
            try {
                manifestUrl = (String) ((Map) ((Map) ((List) mediaGroup.get("media-content")).get(0)).get("@attributes")).get("url");
            } catch (Exception e) {
                throw new PluginImplementationException("Manifest URL not found");
            }
            manifestUrl = URLDecoder.decode(manifestUrl, "UTF-8");

            logger.info("Settings config: " + config);
            if (config.isDownloadSubtitle()) {
                String subtitleUrl = null;
                try {
                    subtitleUrl = (String) ((Map) ((Map) ((List) mediaGroup.get("media-subTitle")).get(0)).get("@attributes")).get("href");
                } catch (Exception e) {
                    //
                }
                if ((subtitleUrl != null) && !subtitleUrl.isEmpty()) {
                    SubtitleDownloader subtitleDownloader = new SubtitleDownloader();
                    subtitleDownloader.downloadSubtitle(client, httpFile, subtitleUrl);
                }
            }

            manifestUrl += (!manifestUrl.contains("?") ? "?" : "&") + "hdcore=3.1.0&plugin=aasp-3.1.0.43.124";
            DramaFeverHdsDownloader downloader = new DramaFeverHdsDownloader(client, httpFile, downloadTask, config.getVideoQuality().getBitrate());
            downloader.tryDownloadAndSaveFile(manifestUrl);
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("page you requested can't be found")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        if (contentAsString.contains("our videos are only available")) {
            throw new NotRecoverableDownloadException("Sorry, our videos are only available in North and South America");
        }
        if (contentAsString.contains("This title is not yet available")) {
            throw new NotRecoverableDownloadException("This title is not yet available on DramaFever");
        }
        if (contentAsString.contains("<title>DramaFever - Not Allowed</title>")) {
            throw new NotRecoverableDownloadException("This content is not available in your location");
        }
    }

    private String getSeriesId(String fileUrl) throws ErrorDuringDownloadingException {
        Matcher matcher = PlugUtils.matcher("dramafever\\.com/[^/]+?/(\\d+)/", fileUrl);
        if (!matcher.find()) {
            throw new PluginImplementationException("Series ID not found");
        }
        return matcher.group(1);
    }

    private String getEpisodeNumber(String fileUrl) throws ErrorDuringDownloadingException {
        Matcher matcher = PlugUtils.matcher("dramafever\\.com/[^/]+?/\\d+/(\\d+)/", fileUrl);
        if (!matcher.find()) {
            throw new PluginImplementationException("Episode number not found");
        }
        return matcher.group(1);
    }

}