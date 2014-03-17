package cz.vity.freerapid.plugins.services.ceskatelevize;

/**
 * @author JPEXS
 * @author tong2shot
 */
public class CeskaTelevizeSettingsConfig {
    private VideoQuality videoQuality = VideoQuality.Highest;

    public void setVideoQuality(VideoQuality videoQuality) {
        this.videoQuality = videoQuality;
    }

    public VideoQuality getVideoQuality() {
        return videoQuality;
    }

    @Override
    public String toString() {
        return "CeskaTelevizeSettingsConfig{" +
                "videoQuality=" + videoQuality +
                '}';
    }
}