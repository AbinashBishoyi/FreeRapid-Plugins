package cz.vity.freerapid.plugins.services.canalplus;

import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.services.adobehds.HdsDownloader;
import cz.vity.freerapid.plugins.services.adobehds.HdsMedia;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpDownloadClient;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFileDownloadTask;

import java.util.List;

/**
 * @author tong2shot
 */
public class CanalPlusHdsDownloader extends HdsDownloader {
    private final int configBitrate;

    public CanalPlusHdsDownloader(HttpDownloadClient client, HttpFile httpFile, HttpFileDownloadTask downloadTask, int configBitrate) {
        super(client, httpFile, downloadTask);
        this.configBitrate = configBitrate;
    }

    @Override
    protected HdsMedia getSelectedMedia(List<HdsMedia> mediaList) throws Exception {
        HdsMedia selectedMedia = null;
        int weight = Integer.MAX_VALUE;
        for (HdsMedia media : mediaList) {
            int deltaBitrate = media.getBitrate() - configBitrate;
            int tempWeight = (deltaBitrate < 0 ? Math.abs(deltaBitrate) + 1 : deltaBitrate);
            if (tempWeight < weight) {
                weight = tempWeight;
                selectedMedia = media;
            }
        }
        if (selectedMedia == null) {
            throw new PluginImplementationException("Unable to select media");
        }
        return selectedMedia;
    }
}
