package cz.vity.freerapid.plugins.services.dramafever;

/**
 * @author tong2shot
 */
public class SettingsConfig {
    private VideoQuality videoQuality = VideoQuality.Highest;
    private boolean downloadSubtitle = true;

    public VideoQuality getVideoQuality() {
        return videoQuality;
    }

    public void setVideoQuality(VideoQuality videoQuality) {
        this.videoQuality = videoQuality;
    }

    public boolean isDownloadSubtitle() {
        return downloadSubtitle;
    }

    public void setDownloadSubtitle(boolean downloadSubtitle) {
        this.downloadSubtitle = downloadSubtitle;
    }

    @Override
    public String toString() {
        return "SettingsConfig{" +
                "videoQuality=" + videoQuality +
                ", downloadSubtitle=" + downloadSubtitle +
                '}';
    }
}
