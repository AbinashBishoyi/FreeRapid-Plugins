package cz.vity.freerapid.plugins.services.crunchyroll;

/**
 * @author tong2shot
 */
public class SettingsConfig {

    private boolean downloadSubtitle = false;

    public boolean isDownloadSubtitle() {
        return downloadSubtitle;
    }

    public void setDownloadSubtitle(boolean downloadSubtitle) {
        this.downloadSubtitle = downloadSubtitle;
    }

    @Override
    public String toString() {
        return "SettingsConfig{" +
                "downloadSubtitle=" + downloadSubtitle +
                '}';
    }
}
