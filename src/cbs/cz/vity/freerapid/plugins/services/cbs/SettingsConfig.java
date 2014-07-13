package cz.vity.freerapid.plugins.services.cbs;

/**
 * @author tong2shot
 */
public class SettingsConfig {
    private VideoQuality videoQuality = VideoQuality.Highest;
    private boolean downloadSubtitles = false;

    public VideoQuality getVideoQuality() {
        return videoQuality;
    }

    public void setVideoQuality(VideoQuality videoQuality) {
        this.videoQuality = videoQuality;
    }

    public boolean isDownloadSubtitles() {
        return downloadSubtitles;
    }

    public void setDownloadSubtitles(boolean downloadSubtitles) {
        this.downloadSubtitles = downloadSubtitles;
    }

    @Override
    public String toString() {
        return "SettingsConfig{" +
                "videoQuality=" + videoQuality +
                ", downloadSubtitles=" + downloadSubtitles +
                '}';
    }
}
