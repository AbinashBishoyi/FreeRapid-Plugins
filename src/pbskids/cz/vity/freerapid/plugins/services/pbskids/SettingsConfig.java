package cz.vity.freerapid.plugins.services.pbskids;

/**
 * @author tong2shot
 */
public class SettingsConfig {
    private VideoQuality videoQuality = VideoQuality._1200;
    private boolean tunlrEnabled = true;

    public VideoQuality getVideoQuality() {
        return videoQuality;
    }

    public void setVideoQuality(VideoQuality videoQuality) {
        this.videoQuality = videoQuality;
    }

    public boolean isTunlrEnabled() {
        return tunlrEnabled;
    }

    public void setTunlrEnabled(boolean tunlrEnabled) {
        this.tunlrEnabled = tunlrEnabled;
    }

    @Override
    public String toString() {
        return "SettingsConfig{" +
                "videoQuality=" + videoQuality +
                '}';
    }
}
