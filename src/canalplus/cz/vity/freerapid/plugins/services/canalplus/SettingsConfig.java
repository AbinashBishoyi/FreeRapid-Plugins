package cz.vity.freerapid.plugins.services.canalplus;

/**
 * @author tong2shot
 */
public class SettingsConfig {
    private VideoQuality videoQuality = VideoQuality._1500;

    public VideoQuality getVideoQuality() {
        return videoQuality;
    }

    public void setVideoQuality(VideoQuality videoQuality) {
        this.videoQuality = videoQuality;
    }

    @Override
    public String toString() {
        return "SettingsConfig{" +
                "videoQuality=" + videoQuality +
                '}';
    }
}
