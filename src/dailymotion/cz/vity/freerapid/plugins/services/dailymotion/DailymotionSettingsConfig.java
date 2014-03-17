package cz.vity.freerapid.plugins.services.dailymotion;

/**
 * @author tong2shot
 */
public class DailymotionSettingsConfig {
    private VideoQuality videoQuality = VideoQuality._480;
    private boolean subtitleDownload = false;

    public VideoQuality getVideoQuality() {
        return videoQuality;
    }

    public void setVideoQuality(VideoQuality videoQuality) {
        this.videoQuality = videoQuality;
    }

    public boolean isSubtitleDownload() {
        return subtitleDownload;
    }

    public void setSubtitleDownload(boolean subtitleDownload) {
        this.subtitleDownload = subtitleDownload;
    }
}
