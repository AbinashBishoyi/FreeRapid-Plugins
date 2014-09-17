package cz.vity.freerapid.plugins.services.nova_novaplus;

/**
 * @author tong2shot
 */

class SettingsConfig {
    private VideoQuality videoQuality = VideoQuality.HQ;

    public VideoQuality getVideoQuality() {
        return videoQuality;
    }

    public void setVideoQuality(final VideoQuality videoQuality) {
        this.videoQuality = videoQuality;
    }

    @Override
    public String toString() {
        return "SettingsConfig{" +
                "videoQuality=" + videoQuality +
                '}';
    }
}
