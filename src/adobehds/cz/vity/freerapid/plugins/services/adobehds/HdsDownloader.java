package cz.vity.freerapid.plugins.services.adobehds;

import cz.vity.freerapid.plugins.webclient.DefaultFileStreamRecognizer;
import cz.vity.freerapid.plugins.webclient.DownloadClient;
import cz.vity.freerapid.plugins.webclient.DownloadClientConsts;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpDownloadClient;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFileDownloadTask;
import cz.vity.freerapid.plugins.webclient.utils.HttpUtils;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;

import java.io.IOException;
import java.util.Collections;
import java.util.logging.Logger;

/**
 * @author ntoskrnl
 */
public class HdsDownloader {
    /*
     * Useful resources:
     *
     * https://github.com/K-S-V/Scripts/blob/master/AdobeHDS.php
     * http://download.macromedia.com/f4v/video_file_format_spec_v10_1.pdf
     * http://sourceforge.net/apps/mediawiki/osmf.adobe/index.php?title=Flash_Media_Manifest_(F4M)_File_Format_obsolete
     */

    private static final Logger logger = Logger.getLogger(HdsDownloader.class.getName());

    private final HttpDownloadClient client;
    private final HttpFile httpFile;
    private final HttpFileDownloadTask downloadTask;

    public HdsDownloader(final HttpDownloadClient client, final HttpFile httpFile, final HttpFileDownloadTask downloadTask) {
        this.client = client;
        this.httpFile = httpFile;
        this.downloadTask = downloadTask;
    }

    public void tryDownloadAndSaveFile(final String manifestUrl) throws Exception {
        client.getHTTPClient().getParams().setParameter(DownloadClientConsts.FILE_STREAM_RECOGNIZER, new DefaultFileStreamRecognizer(new String[0], new String[]{"video/f4m"}, false));
        final HdsManifest manifest = new HdsManifest(client, manifestUrl);
        final HdsMedia media = Collections.max(manifest.getMedias());
        logger.info("Downloading media: " + media);

        httpFile.setState(DownloadState.GETTING);
        logger.info("Starting HDS download");

        httpFile.getProperties().remove(DownloadClient.START_POSITION);
        httpFile.getProperties().remove(DownloadClient.SUPPOSE_TO_DOWNLOAD);
        httpFile.setResumeSupported(false);

        final String fn = httpFile.getFileName();
        if (fn == null || fn.isEmpty())
            throw new IOException("No defined file name");
        httpFile.setFileName(HttpUtils.replaceInvalidCharsForFileSystem(PlugUtils.unescapeHtml(fn), "_"));

        client.getHTTPClient().getParams().setBooleanParameter(DownloadClientConsts.NO_CONTENT_LENGTH_AVAILABLE, true);

        final FragmentRequester requester = new FragmentRequester(httpFile, client, media);
        final HdsInputStream in = new HdsInputStream(requester);

        try {
            downloadTask.saveToFile(in);
        } finally {
            in.close();
        }
    }

}