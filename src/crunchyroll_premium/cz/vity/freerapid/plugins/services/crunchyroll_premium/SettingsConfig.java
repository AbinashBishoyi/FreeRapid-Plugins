package cz.vity.freerapid.plugins.services.crunchyroll_premium;

/**
 * @author tong2shot
 */
public class SettingsConfig {

    private String username = null;
    private String password = null;
    private VideoQuality videoQuality = VideoQuality._480;
    private boolean downloadSubtitle = false;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

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
