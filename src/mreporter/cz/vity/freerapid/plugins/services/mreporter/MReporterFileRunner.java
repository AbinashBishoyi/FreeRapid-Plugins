package cz.vity.freerapid.plugins.services.mreporter;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.services.adobehds.HdsDownloader;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.Arrays;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author tong2shot
 * @since 0.9u4
 */
class MReporterFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(MReporterFileRunner.class.getName());
    private SettingsConfig config;

    private void setConfig() throws Exception {
        MReporterServiceImpl service = (MReporterServiceImpl) getPluginService();
        config = service.getConfig();
    }

    private void checkUrl() {
        if (fileURL.contains("/styles/video/ext/")) {
            fileURL = fileURL.replaceFirst("/styles/video/ext/(\\d+)/.+", "/reports/$1");
        }
    }

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        checkUrl();
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
        PlugUtils.checkName(httpFile, content, "\"og:title\" content=\"", "\"");
        httpFile.setFileName(httpFile.getFileName() + ".flv");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        checkUrl();
        logger.info("Starting download in TASK " + fileURL);
        GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize(getContentAsString());

            Matcher matcher = PlugUtils.matcher("uppod\\.swf\\?st=(http://[^\"]+?)\"", getContentAsString());
            if (!matcher.find()) {
                throw new PluginImplementationException("Player URL not found");
            }
            String playerUrl = String.format("%s?rand=%f", matcher.group(1), Math.random()); //http://www.mreporter.ru/styles/video/int/40284/141277?rand=0.04038242530077696
            method = getGetMethod(playerUrl);
            if (!makeRedirectedRequest(method)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            checkProblems();

            setConfig();
            String manifestUrl = getManifestUrl(new Decrypter().decrypt(getContentAsString()));
            HdsDownloader downloader = new HdsDownloader(client, httpFile, downloadTask);
            downloader.tryDownloadAndSaveFile(manifestUrl);
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("Страница не найдена")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    private String getManifestUrl(String content) throws PluginImplementationException {
        Matcher matcher = PlugUtils.matcher("\"file\":\"(http://[^\"]+?\\.\\[(.+?)\\]\\.f4m)\"", content);
        if (!matcher.find()) {
            throw new PluginImplementationException("Cannot find manifest URL");
        }
        String manifestUrl = matcher.group(1); //http://b1.mreporter.ru/c/139866.[360p,144p,240p].f4m
        String[] strQualities = matcher.group(2).replace("p", "").split(","); //360p,144p,240p to [360,144,240]
        logger.info("Available qualities: " + Arrays.toString(strQualities));

        VideoQuality configQuality = config.getVideoQuality();
        final int LOWER_QUALITY_PENALTY = 10;
        int weight = Integer.MAX_VALUE;
        int selectedQuality = -1;
        for (String strQuality : strQualities) {
            int videoQuality = Integer.parseInt(strQuality);
            int deltaQ = videoQuality - configQuality.getQuality();
            int tempWeight = (deltaQ < 0 ? Math.abs(deltaQ) + LOWER_QUALITY_PENALTY : deltaQ);
            if (tempWeight < weight) {
                weight = tempWeight;
                selectedQuality = videoQuality;
            }
        }
        if (selectedQuality == -1) {
            throw new PluginImplementationException("Unable to select quality");
        }

        manifestUrl = manifestUrl.replaceFirst("\\.\\[.+?\\]\\.f4m", "." + selectedQuality + "p.f4m"); //http://b1.mreporter.ru/c/139866.360p.f4m
        logger.info("Config settings: " + config);
        logger.info("Selected quality: " + selectedQuality + "p");
        logger.info("Manifest URL: " + manifestUrl);
        return manifestUrl;
    }

}
