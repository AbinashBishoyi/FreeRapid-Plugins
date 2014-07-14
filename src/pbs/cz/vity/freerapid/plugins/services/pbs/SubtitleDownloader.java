package cz.vity.freerapid.plugins.services.pbs;

import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpDownloadClient;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import cz.vity.freerapid.plugins.webclient.utils.HttpUtils;
import cz.vity.freerapid.utilities.LogUtils;
import org.apache.commons.httpclient.HttpMethod;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.logging.Logger;

/**
 * @author tong2shot
 */
public class SubtitleDownloader {
    private final static Logger logger = Logger.getLogger(SubtitleDownloader.class.getName());

    public void downloadSubtitle(HttpDownloadClient client, HttpFile httpFile, String subtitleUrl) throws Exception {
        if ((subtitleUrl == null) || subtitleUrl.isEmpty()) {
            return;
        }
        logger.info("Downloading subtitle");
        HttpMethod method = client.getGetMethod(subtitleUrl);
        if (200 != client.makeRequest(method, true)) {
            throw new PluginImplementationException("Failed to request subtitle");
        }
        String subtitleText = client.getContentAsString();

        String path = new URL(subtitleUrl).getPath();
        String fileExt = path.substring(path.lastIndexOf("."));
        String fnameNoExt = HttpUtils.replaceInvalidCharsForFileSystem(httpFile.getFileName().replaceFirst("\\.[^\\.]{3,4}$", ""), "_");
        String fnameOutput = fnameNoExt + fileExt;
        File outputFile = new File(httpFile.getSaveToDirectory(), fnameOutput);
        BufferedWriter bw = null;
        int outputFileCounter = 2;
        try {
            while (outputFile.exists()) {
                fnameOutput = fnameNoExt + "-" + outputFileCounter++ + fileExt;
                outputFile = new File(httpFile.getSaveToDirectory(), fnameOutput);
            }
            bw = new BufferedWriter(new FileWriter((outputFile)));
            if (fileExt.equals(".dfxp")) {
                bw.write(TimedText2Srt.convert(subtitleText));
            } else {
                bw.write(subtitleText);
            }
        } finally {
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
