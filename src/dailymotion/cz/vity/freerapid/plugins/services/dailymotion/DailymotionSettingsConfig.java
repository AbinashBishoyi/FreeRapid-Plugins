package cz.vity.freerapid.plugins.services.dailymotion;

/**
 * @author tong2shot
 */
public class DailymotionSettingsConfig {
    private int qualitySetting;
    private boolean subtitleDownload;

    public int getQualitySetting() {
        return qualitySetting;
    }

    public void setQualitySetting(int qualitySetting) {
        this.qualitySetting = qualitySetting;
    }

    public boolean isSubtitleDownload() {
        return subtitleDownload;
    }

    public void setSubtitleDownload(boolean subtitleDownload) {
        this.subtitleDownload = subtitleDownload;
    }
}
