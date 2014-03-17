package cz.vity.freerapid.plugins.services.hulu;

/**
 * @author tong2shot
 */
public class HuluSettingsConfig {

    private String username = null;
    private String password = null;
    private VideoQuality videoQuality = VideoQuality._480;
    private boolean downloadSubtitles = false;
    private boolean tunlrEnabled = true;

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

    public boolean isDownloadSubtitles() {
        return downloadSubtitles;
    }

    public void setDownloadSubtitles(boolean downloadSubtitles) {
        this.downloadSubtitles = downloadSubtitles;
    }

    public boolean isTunlrEnabled() {
        return tunlrEnabled;
    }

    public void setTunlrEnabled(boolean tunlrEnabled) {
        this.tunlrEnabled = tunlrEnabled;
    }

    @Override
    public String toString() {
        return "HuluSettingsConfig{" +
                "videoQuality=" + videoQuality +
                '}';
    }
}
