package cz.vity.freerapid.plugins.services.vk;

/**
 * @author tong2shot
 */

public class VkSettingsConfig {
    private VideoQuality videoQuality = VideoQuality._480;

    public VideoQuality getVideoQuality() {
        return videoQuality;
    }

    public void setVideoQuality(final VideoQuality videoQuality) {
        this.videoQuality = videoQuality;
    }
}
