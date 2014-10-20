package cz.vity.freerapid.plugins.services.itv;

import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpDownloadClient;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import cz.vity.freerapid.plugins.webclient.utils.HttpUtils;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import cz.vity.freerapid.utilities.LogUtils;

import java.io.*;
import java.net.URLDecoder;
import java.util.logging.Logger;

/**
 * @author tong2shot
 */
class SubtitleDownloader {
    private final static Logger logger = Logger.getLogger(SubtitleDownloader.class.getName());

    public void downloadSubtitle(HttpDownloadClient client, HttpFile httpFile, String subtitleUrl) throws Exception {
        if ((subtitleUrl == null) || subtitleUrl.isEmpty()) {
            return;
        }
        logger.info("Downloading subtitle");
        logger.info("Subtitle URL: " + subtitleUrl);
        InputStream is = client.makeRequestForFile(client.getGetMethod(subtitleUrl));
        if (is == null) {
            throw new ServiceConnectionProblemException("Error downloading subtitle");
        }
        BufferedInputStream timedTextIs = new BufferedInputStream(is);

        String fnameNoExt = PlugUtils.unescapeHtml(URLDecoder.decode(HttpUtils.replaceInvalidCharsForFileSystem(
                httpFile.getFileName().replaceFirst("\\.[^\\.]{3,4}$", ""), "_"), "UTF-8"));
        String fnameOutput = fnameNoExt + ".srt";
        File outputFile = new File(httpFile.getSaveToDirectory(), fnameOutput);
        BufferedWriter bw = null;
        int outputFileCounter = 2;
        try {
            while (outputFile.exists()) {
                fnameOutput = fnameNoExt + "-" + outputFileCounter++ + ".srt";
                outputFile = new File(httpFile.getSaveToDirectory(), fnameOutput);
            }
            bw = new BufferedWriter(new FileWriter((outputFile)));
            bw.write(TimedText2Srt.convert(timedTextIs));
        } finally {
            try {
                timedTextIs.close();
            } catch (IOException e) {
                LogUtils.processException(logger, e);
            }
            if (bw != null) {
                try {
                    bw.close();
                } catch (IOException e) {
                    LogUtils.processException(logger, e);
                }
            }
        }
    }

}
