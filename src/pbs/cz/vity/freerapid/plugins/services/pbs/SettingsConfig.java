package cz.vity.freerapid.plugins.services.pbs;

/**
 * @author tong2shot
 */
public class SettingsConfig {

    private boolean downloadSubtitles = false;

    public boolean isDownloadSubtitles() {
        return downloadSubtitles;
    }

    public void setDownloadSubtitles(boolean downloadSubtitles) {
        this.downloadSubtitles = downloadSubtitles;
    }

    @Override
    public String toString() {
        return "SettingsConfig{" +
                "downloadSubtitles=" + downloadSubtitles +
                '}';
    }
}
